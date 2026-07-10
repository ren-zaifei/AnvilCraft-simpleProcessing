package com.renzaifei.asp.util;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.anvilcraft.lib.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 此类大部分由ai生成
 *
 */
//我真的很不擅长数据这类东西
public final class RecipeUtil {

    private RecipeUtil() {}

    // ==================== 缓存 ====================

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

    // ==================== 配方查询 ====================

    public static List<RecipeHolder<?>> find(
            @Nullable RecipeType<?> type,
            @Nullable ItemStack input,
            @Nullable Item output) {

        if (!cacheValid) return List.of();
        Set<RecipeHolder<?>> candidates = null;

        if (type != null) {
            candidates = byType.getOrDefault(type, Set.of());
        }
        if (output != null) {
            Set<RecipeHolder<?>> set = byOutput.getOrDefault(output, Set.of());
            candidates = candidates == null ? set : intersect(candidates, set);
        }
        if (input != null && !input.isEmpty()) {
            Set<RecipeHolder<?>> set = byInput.getOrDefault(input.getItem(), Set.of());
            candidates = candidates == null ? set : intersect(candidates, set);
            List<RecipeHolder<?>> result = new ArrayList<>();
            for (var holder : candidates) {
                if (matchesInput(holder.value(), input)) result.add(holder);
            }
            return Collections.unmodifiableList(result);
        }
        if (candidates == null) {
            candidates = all;
        }
        return Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    // ==================== 合成入口 ====================

    /**
     * 在给定物品处理器中查找匹配的配方并消耗输入、返回产物。
     *
     * @param type         配方类型
     * @param inputHandler 输入槽位（会直接修改）
     * @param serverLevel  服务端世界，用于概率判定的随机源
     * @return 产物列表；未匹配到配方时返回空列表
     */
    public static List<ItemStack> craft(
            RecipeType<?> type,
            IItemHandler inputHandler,
            ServerLevel serverLevel) {
        if (!cacheValid) return List.of();

        List<ItemStack> inputs = collectInputs(inputHandler);
        if (inputs.isEmpty()) return List.of();

        return inputs.size() == 1
                ? tryCraftSingle(type, inputHandler, inputs.getFirst(), serverLevel)
                : tryCraftMulti(type, inputHandler, inputs, serverLevel);
    }

    // ==================== 单输入合成 ====================

    private static List<ItemStack> tryCraftSingle(
            RecipeType<?> type,
            IItemHandler handler,
            ItemStack input,
            ServerLevel level) {
        for (var holder : find(type, input, null)) {
            int required = getRequiredCount(holder.value(), input);
            if (required <= 0) continue;

            int times = input.getCount() / required;
            if (times <= 0) continue;

            List<ItemStack> outputs = holder.value() instanceof AbstractProcessRecipe<?> processRecipe
                    ? evaluateResults(processRecipe, times, level)
                    : scaleVanillaOutputs(holder.value(), times);
            if (!outputs.isEmpty()) {
                extractSlot(handler, input, input.getCount());
            }
            return outputs;
        }
        return List.of();
    }

    // ==================== 多输入合成 ====================

    private static List<ItemStack> tryCraftMulti(
            RecipeType<?> type,
            IItemHandler handler,
            List<ItemStack> inputs,
            ServerLevel level) {
        Set<RecipeHolder<?>> candidates = buildMultiCandidateSet(inputs);
        if (candidates.isEmpty()) return List.of();

        int[] required = new int[inputs.size()];
        for (var holder : candidates) {
            if (!(holder.value() instanceof AbstractProcessRecipe<?> r)) continue;
            if (type != null && holder.value().getType() != type) continue;
            if (!matchMultiInput(r, inputs, required)) continue;

            int times = minDivisible(inputs, required);
            if (times <= 0) continue;

            List<ItemStack> outputs = evaluateResults(r, times, level);
            if (outputs.isEmpty()) continue;

            consumeInputs(handler, inputs, required, times);
            return outputs;
        }
        return List.of();
    }

    /** 构建多输入配方的候选集：取所有输入物品的配方交集 */
    private static Set<RecipeHolder<?>> buildMultiCandidateSet(List<ItemStack> inputs) {
        Set<RecipeHolder<?>> candidates = null;
        for (ItemStack input : inputs) {
            Set<RecipeHolder<?>> set = byInput.getOrDefault(input.getItem(), Set.of());
            if (candidates == null) {
                candidates = new HashSet<>(set);
            } else {
                candidates.retainAll(set);
            }
            if (candidates.isEmpty()) return Set.of();
        }
        return candidates;
    }

    /** 将配方原料逐一匹配到输入物品，结果填入 required 数组 */
    private static boolean matchMultiInput(AbstractProcessRecipe<?> recipe,
                                           List<ItemStack> inputs, int[] required) {
        var ingredients = recipe.getInputItems();
        if (ingredients.isEmpty() || ingredients.size() > inputs.size()) return false;
        Arrays.fill(required, 0);
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
            if (!found) return false;
        }
        return true;
    }

    /** 计算所有输入能被最大整除次数 */
    private static int minDivisible(List<ItemStack> inputs, int[] required) {
        int times = Integer.MAX_VALUE;
        for (int j = 0; j < inputs.size(); j++) {
            if (required[j] > 0) {
                times = Math.min(times, inputs.get(j).getCount() / required[j]);
            }
        }
        return times == Integer.MAX_VALUE ? 0 : times;
    }

    // ==================== 输入输出操作 ====================

    /** 从 handler 中收集所有非空物品堆 */
    private static List<ItemStack> collectInputs(IItemHandler handler) {
        int slots = handler.getSlots();
        List<ItemStack> inputs = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) inputs.add(stack);
        }
        return inputs;
    }

    /** 消耗 handler 中对应输入槽位的物品 */
    private static void consumeInputs(IItemHandler handler, List<ItemStack> inputs,
                                      int[] required, int times) {
        for (int j = 0; j < inputs.size(); j++) {
            if (required[j] > 0) {
                int idx = findSlot(handler, inputs.get(j));
                if (idx >= 0) handler.extractItem(idx, required[j] * times, false);
            }
        }
    }

    /** 消耗单个物品堆（不区分槽位，直接调用 extractItem） */
    private static void extractSlot(IItemHandler handler, ItemStack target, int amount) {
        int idx = findSlot(handler, target);
        if (idx >= 0) handler.extractItem(idx, amount, false);
    }

    private static int findSlot(IItemHandler handler, ItemStack target) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, target)) return i;
        }
        return -1;
    }

    // ==================== 产物求值 ====================

    /**
     * 对配方的概率产物进行随机判定，返回实际产出的物品列表。
     * 直接调用 AnvilCraft 原生的 {@link InWorldRecipeContext#getInt} 评估
     * NumberProvider，每个 craft 批次独立判定。
     */
    private static List<ItemStack> evaluateResults(
            AbstractProcessRecipe<?> recipe, int times, ServerLevel serverLevel) {
        InWorldRecipeContext context = new InWorldRecipeContext(serverLevel, Vec3.ZERO, null);
        List<ItemStack> outputs = new ArrayList<>();
        for (ChanceItemStack chance : recipe.getResultItems()) {
            int total = 0;
            for (int i = 0; i < times; i++) {
                total += context.getInt(chance.count(), 0, 99);
            }
            if (total > 0) {
                ItemStack out = chance.stack().copy();
                out.setCount(total);
                outputs.add(out);
            }
        }
        return outputs;
    }

    /** 原版配方的产物缩放（不做概率判定） */
    private static List<ItemStack> scaleVanillaOutputs(Recipe<?> recipe, int times) {
        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack stack : extractOutputs(recipe)) {
            if (!stack.isEmpty()) {
                ItemStack scaled = stack.copy();
                scaled.setCount(stack.getCount() * times);
                outputs.add(scaled);
            }
        }
        return outputs;
    }

    // ==================== 原料匹配辅助 ====================

    private static int getRequiredCount(Recipe<?> recipe, ItemStack input) {
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ItemIngredientPredicate ingredient : processRecipe.getInputItems()) {
                if (ingredient.test(input)) {
                    return getRequiredCountFor(ingredient, input);
                }
            }
        }
        for (net.minecraft.world.item.crafting.Ingredient ing : recipe.getIngredients()) {
            if (ing.test(input)) {
                for (ItemStack match : ing.getItems()) {
                    if (ItemStack.isSameItemSameComponents(match, input)) return match.getCount();
                }
                return 1;
            }
        }
        return 0;
    }

    private static int getRequiredCountFor(ItemIngredientPredicate ingredient, ItemStack input) {
        for (ItemStack match : ingredient.getItems()) {
            if (ItemStack.isSameItemSameComponents(match, input)) return match.getCount();
        }
        return 1;
    }

    // ==================== 缓存索引提取 ====================

    private static Set<Item> extractInputItems(Recipe<?> recipe) {
        Set<Item> items = new HashSet<>();
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ItemIngredientPredicate ingredient : processRecipe.getInputItems()) {
                for (ItemStack stack : ingredient.getItems()) {
                    if (!stack.isEmpty()) items.add(stack.getItem());
                }
            }
        }
        for (var ing : recipe.getIngredients()) {
            for (ItemStack stack : ing.getItems()) {
                if (!stack.isEmpty()) items.add(stack.getItem());
            }
        }
        return items;
    }

    private static List<ItemStack> extractOutputs(Recipe<?> recipe) {
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            List<ItemStack> outputs = new ArrayList<>();
            for (ChanceItemStack chance : processRecipe.getResultItems()) {
                ItemStack stack = chance.stack();
                if (!stack.isEmpty()) outputs.add(stack);
            }
            return outputs;
        }
        try {
            ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
            return result.isEmpty() ? List.of() : List.of(result);
        } catch (Exception e) {
            AnvilCraftSimpleProcessing.LOGGER.warn("Get Recipe Error : {}", e.getMessage());
            return List.of();
        }
    }

    private static boolean matchesInput(Recipe<?> recipe, ItemStack input) {
        if (input.isEmpty()) return false;
        if (recipe instanceof AbstractProcessRecipe<?> processRecipe) {
            for (ItemIngredientPredicate ingredient : processRecipe.getInputItems()) {
                if (ingredient.test(input)) return true;
            }
            return false;
        }
        for (var ingredient : recipe.getIngredients()) {
            if (ingredient.test(input)) return true;
        }
        return false;
    }

    // ==================== 集合工具 ====================

    /** 取两个集合的交集（不修改参数） */
    private static Set<RecipeHolder<?>> intersect(Set<RecipeHolder<?>> a, Set<RecipeHolder<?>> b) {
        if (a.size() > b.size()) { var tmp = a; a = b; b = tmp; }
        Set<RecipeHolder<?>> result = new HashSet<>();
        for (var holder : a) {
            if (b.contains(holder)) result.add(holder);
        }
        return result;
    }

    // ==================== 其他公开方法 ====================

    @Nullable
    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
