package com.renzaifei.asp.util;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class RecipeUtil {

    private RecipeUtil() {}
    private static final Map<RecipeType<?>, Set<RecipeHolder<?>>> byType = new HashMap<>();
    private static final Map<Item, Set<RecipeHolder<?>>> byInput = new HashMap<>();
    private static final Map<Item, Set<RecipeHolder<?>>> byOutput = new HashMap<>();
    private static final Set<RecipeHolder<?>> all = new HashSet<>();

    private static volatile boolean cacheValid = false;
    @Nullable
    private static RecipeManager recipeManager;

    public static void rebuild(RecipeManager manager) {
        recipeManager = manager;
        byType.clear();
        byInput.clear();
        byOutput.clear();
        all.clear();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            all.add(holder);
            Recipe<?> recipe = holder.value();
            byType.computeIfAbsent(recipe.getType(), k -> new HashSet<>()).add(holder);
            for (ItemStack stack : extractOutputs(recipe)) {
                if (!stack.isEmpty()) {
                    byOutput.computeIfAbsent(stack.getItem(), k -> new HashSet<>()).add(holder);
                }
            }
            for (Item item : extractInputItems(recipe)) {
                byInput.computeIfAbsent(item, k -> new HashSet<>()).add(holder);
            }
        }
        cacheValid = true;
    }

    public static List<RecipeHolder<?>> find(
            @Nullable RecipeType<?> type,
            @Nullable ItemStack input,
            @Nullable Item output) {

        if (!cacheValid) return Collections.emptyList();
        Set<RecipeHolder<?>> candidates = null;
        if (type != null) {
            candidates = byType.getOrDefault(type, Collections.emptySet());
        }
        if (output != null) {
            Set<RecipeHolder<?>> outputSet = byOutput.getOrDefault(output, Collections.emptySet());
            if (candidates == null) {
                candidates = outputSet;
            } else {
                candidates = intersect(candidates, outputSet);
            }
        }
        if (input != null && !input.isEmpty()) {
            Set<RecipeHolder<?>> inputSet = byInput.getOrDefault(input.getItem(), Collections.emptySet());
            if (candidates == null) {
                candidates = inputSet;
            } else {
                candidates = intersect(candidates, inputSet);
            }
            List<RecipeHolder<?>> result = new ArrayList<>();
            for (RecipeHolder<?> holder : candidates) {
                if (matchesInput(holder.value(), input)) {
                    result.add(holder);
                }
            }
            return Collections.unmodifiableList(result);
        }

        if (candidates == null) {
            candidates = all;
        }

        return Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    public static List<ItemStack> getOutputs(RecipeType<?> type, ItemStack input) {
        List<RecipeHolder<?>> recipes = find(type, input, null);
        List<ItemStack> outputs = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            int required = getRequiredCount(holder.value(), input);
            if (required <= 0) continue;
            int times = input.getCount() / required;
            if (times <= 0) continue;
            for (ItemStack stack : extractOutputs(holder.value())) {
                if (!stack.isEmpty()) {
                    ItemStack scaled = stack.copy();
                    scaled.setCount(stack.getCount() * times);
                    outputs.add(scaled);
                }
            }
        }
        return Collections.unmodifiableList(outputs);
    }

    private static int getRequiredCount(Recipe<?> recipe, ItemStack input) {
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ItemIngredientPredicate ingredient : processRecipe.getInputItems()) {
                if (ingredient.test(input)) {
                    for (ItemStack match : ingredient.getItems()) {
                        if (ItemStack.isSameItemSameComponents(match, input)) {
                            return match.getCount();
                        }
                    }
                    return 1;
                }
            }
        }
        for (net.minecraft.world.item.crafting.Ingredient ing : recipe.getIngredients()) {
            if (ing.test(input)) {
                for (ItemStack match : ing.getItems()) {
                    if (ItemStack.isSameItemSameComponents(match, input)) {
                        return match.getCount();
                    }
                }
                return 1;
            }
        }
        return 0;
    }

    /**
     * 在给定物品处理器中查找匹配的配方并直接消耗输入、返回产物。
     * @param type   配方类型
     * @param inputHandler 输入槽位的物品处理器（会直接修改）
     * @return 产物列表；未匹配到任何配方时返回空列表
     */
    public static List<ItemStack> craft(
            RecipeType<?> type,
            IItemHandler inputHandler) {
        if (!cacheValid) return Collections.emptyList();

        int slots = inputHandler.getSlots();
        List<ItemStack> inputs = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            ItemStack inSlot = inputHandler.getStackInSlot(i);
            if (!inSlot.isEmpty()) inputs.add(inSlot);
        }
        if (inputs.isEmpty()) return Collections.emptyList();
        if (inputs.size() == 1) {
            ItemStack inSlot = inputs.getFirst();
            List<ItemStack> outputs = getOutputs(type, inSlot);
            if (!outputs.isEmpty()) {
                int idx = findSlot(inputHandler, inSlot);
                if (idx >= 0) inputHandler.extractItem(idx, inSlot.getCount(), false);
            }
            return outputs;
        }
        Set<RecipeHolder<?>> candidates = null;
        for (ItemStack input : inputs) {
            Set<RecipeHolder<?>> set = byInput.getOrDefault(input.getItem(), Collections.emptySet());
            if (candidates == null) {
                candidates = new HashSet<>(set);
            } else {
                candidates.retainAll(set);
            }
            if (candidates.isEmpty()) return Collections.emptyList();
        }

        int[] required = new int[inputs.size()];
        for (var holder : candidates) {
            if (type != null && holder.value().getType() != type) continue;
            if (!(holder.value() instanceof AbstractProcessRecipe<?> r)) continue;
            var ingredients = r.getInputItems();
            if (ingredients.isEmpty() || ingredients.size() > inputs.size()) continue;
            Arrays.fill(required, 0);

            boolean allMatched = true;
            for (var ingredient : ingredients) {
                boolean found = false;
                for (int j = 0; j < inputs.size(); j++) {
                    if (required[j] > 0) continue;
                    if (ingredient.test(inputs.get(j))) {
                        required[j] = getRequiredCountFor(ingredient, inputs.get(j));
                        found = true;
                        break;
                    }
                }
                if (!found) { allMatched = false; break; }
            }
            if (!allMatched) continue;

            int times = Integer.MAX_VALUE;
            for (int j = 0; j < inputs.size(); j++) {
                if (required[j] > 0) {
                    times = Math.min(times, inputs.get(j).getCount() / required[j]);
                }
            }
            if (times <= 0) continue;

            for (int j = 0; j < inputs.size(); j++) {
                if (required[j] > 0) {
                    int idx = findSlot(inputHandler, inputs.get(j));
                    if (idx >= 0) inputHandler.extractItem(idx, required[j] * times, false);
                }
            }

            List<ItemStack> outputs = new ArrayList<>();
            for (var chance : r.getResultItems()) {
                ItemStack out = chance.stack().copy();
                out.setCount(out.getCount() * times);
                outputs.add(out);
            }
            return outputs;
        }

        return Collections.emptyList();
    }

    /** 在 handler 中找到与 target 同物品的槽位索引，未找到返回 -1。 */
    private static int findSlot(IItemHandler handler, ItemStack target) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, target)) return i;
        }
        return -1;
    }

    /** 获取 ItemIngredientPredicate 对指定物品堆的单次消耗量。 */
    private static int getRequiredCountFor(
            dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate ingredient,
            ItemStack input) {
        for (ItemStack match : ingredient.getItems()) {
            if (ItemStack.isSameItemSameComponents(match, input)) {
                return match.getCount();
            }
        }
        return 1;
    }

    @Nullable
    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }

    private static Set<Item> extractInputItems(Recipe<?> recipe) {
        Set<Item> items = new HashSet<>();
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ItemIngredientPredicate ingredient : processRecipe.getInputItems()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (!stack.isEmpty()) {
                        items.add(stack.getItem());
                    }
                }
            }
        }
        for (net.minecraft.world.item.crafting.Ingredient ing : recipe.getIngredients()) {
            for (ItemStack stack : ing.getItems()) {
                if (!stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            }
        }

        return items;
    }

    private static List<ItemStack> extractOutputs(Recipe<?> recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ChanceItemStack chance : processRecipe.getResultItems()) {
                ItemStack stack = chance.stack();
                if (!stack.isEmpty()) {
                    outputs.add(stack);
                }
            }
            return outputs;
        }
        try {
            ItemStack resultItem = recipe.getResultItem(RegistryAccess.EMPTY);
            if (!resultItem.isEmpty()) {
                outputs.add(resultItem);
            }
        } catch (Exception e) {
            AnvilCraftSimpleProcessing.LOGGER.warn("Get Recipe Error : {}" , e.getMessage());
        }

        return outputs;
    }

    private static boolean matchesInput(Recipe<?> recipe, ItemStack input) {
        if (input.isEmpty()) return false;
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ItemIngredientPredicate ingredient : processRecipe.getInputItems()) {
                if (ingredient.test(input)) {
                    return true;
                }
            }
            return false;
        }
        for (net.minecraft.world.item.crafting.Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.test(input)) {
                return true;
            }
        }

        return false;
    }

    private static Set<RecipeHolder<?>> intersect(Set<RecipeHolder<?>> a, Set<RecipeHolder<?>> b) {
        if (a.size() > b.size()) {
            Set<RecipeHolder<?>> tmp = a;
            a = b;
            b = tmp;
        }
        Set<RecipeHolder<?>> result = new HashSet<>();
        for (RecipeHolder<?> holder : a) {
            if (b.contains(holder)) {
                result.add(holder);
            }
        }
        return result;
    }
}