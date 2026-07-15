package com.renzaifei.asp.util;

import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.*;

/**
 * 此类大部分由ai生成
 *
 */
public final class RecipeUtil {

    private RecipeUtil() {}

    // ==================== 合成入口 ====================

    /**
     * 在给定物品处理器中查找匹配的 {@link AbstractProcessRecipe}，
     * 消耗输入原料，返回概率求值后的产物列表。
     *
     * @param type         配方类型（如 {@code ITEM_CRUSH_TYPE}）
     * @param inputHandler 输入槽位（会直接修改）
     * @param serverLevel  服务端世界，用于概率判定的随机源
     * @return 产物列表；未匹配到配方时返回空列表
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<ItemStack> craft(
            RecipeType<?> type,
            IItemHandler inputHandler,
            ServerLevel serverLevel) {

        RecipeManager manager = serverLevel.getServer().getRecipeManager();
        List<ItemStack> inputs = collectInputs(inputHandler);
        if (inputs.isEmpty()) return List.of();

        // 通过 NeoForge 公开的 getAllRecipesFor 按类型获取配方
        List<RecipeHolder<?>> recipes = (List) manager.getAllRecipesFor((RecipeType) type);

        List<ItemStack> outputs = inputs.size() == 1
                ? tryCraftSingle(inputHandler, inputs.getFirst(), recipes, serverLevel)
                : tryCraftMulti(inputHandler, inputs, recipes, serverLevel);

        return Collections.unmodifiableList(outputs);
    }

    // ==================== 单输入合成 ====================

    private static List<ItemStack> tryCraftSingle(
            IItemHandler handler,
            ItemStack input,
            List<RecipeHolder<?>> recipes,
            ServerLevel level) {

        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof AbstractProcessRecipe<?> recipe)) continue;

            // 单输入路径只处理只有一个原料的配方，防止多原料配方被部分匹配
            if (recipe.getInputItems().size() != 1) continue;

            int required = matchInputCount(recipe, input);
            if (required <= 0) continue;

            int times = input.getCount() / required;
            if (times <= 0) continue;

            List<ItemStack> outputs = evaluateResults(recipe, times, level);
            extractSlot(handler, input, required * times);
            return outputs;
        }
        return List.of();
    }

    // ==================== 多输入合成 ====================

    private static List<ItemStack> tryCraftMulti(
            IItemHandler handler,
            List<ItemStack> inputs,
            List<RecipeHolder<?>> recipes,
            ServerLevel level) {

        int[] required = new int[inputs.size()];

        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof AbstractProcessRecipe<?> recipe)) continue;
            if (!matchMultiInput(recipe, inputs, required)) continue;

            int times = calcTimes(inputs, required);
            if (times <= 0) continue;

            List<ItemStack> outputs = evaluateResults(recipe, times, level);
            consumeInputs(handler, inputs, required, times);
            return outputs;
        }
        return List.of();
    }

    // ==================== 原料匹配 ====================

    /** 返回配方中对 {@code input} 的需求数量；不匹配时返回 0。 */
    private static int matchInputCount(AbstractProcessRecipe<?> recipe, ItemStack input) {
        for (ItemIngredientPredicate ingredient : recipe.getInputItems()) {
            if (ingredient.test(input)) {
                return ingredient.count();
            }
        }
        return 0;
    }

    /**
     * 贪心匹配：将配方的每个原料依次分配到第一个未使用且匹配的输入槽位。
     * 结果填入 {@code required}（每槽需要的数量）。
     */
    private static boolean matchMultiInput(
            AbstractProcessRecipe<?> recipe,
            List<ItemStack> inputs,
            int[] required) {

        List<ItemIngredientPredicate> ingredients = recipe.getInputItems();
        if (ingredients.isEmpty() || ingredients.size() > inputs.size()) return false;

        Arrays.fill(required, 0);

        for (ItemIngredientPredicate ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < inputs.size(); i++) {
                if (required[i] > 0) continue; // 已被前面的原料占用
                if (ingredient.test(inputs.get(i))) {
                    required[i] = ingredient.count();
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /** 计算所有输入能被整除的最大合成次数。 */
    private static int calcTimes(List<ItemStack> inputs, int[] required) {
        int times = Integer.MAX_VALUE;
        for (int i = 0; i < inputs.size(); i++) {
            if (required[i] > 0) {
                times = Math.min(times, inputs.get(i).getCount() / required[i]);
            }
        }
        return times == Integer.MAX_VALUE ? 0 : times;
    }

    // ==================== 产物求值 ====================

    /**
     * 直接评估 {@link ChanceItemStack#count()} 的 {@code NumberProvider}，
     * 仅在 count > 0 时产出。不使用 {@code getResult()} 是因为它内部做了
     * {@code Math.clamp(count, 1, 99)}，会导致 0 被强制变成 1。
     */
    private static List<ItemStack> evaluateResults(
            AbstractProcessRecipe<?> recipe, int times, ServerLevel serverLevel) {

        LootContext lootContext = new LootContext.Builder(
                new LootParams(serverLevel, Map.of(), Map.of(), 0))
                .create(java.util.Optional.empty());

        List<ItemStack> outputs = new ArrayList<>();
        for (ChanceItemStack chance : recipe.getResultItems()) {
            for (int i = 0; i < times; i++) {
                int count = chance.count().getInt(lootContext);
                if (count > 0) {
                    ItemStack result = chance.stack().copyWithCount(count);
                    mergeIntoOutputs(outputs, result);
                }
            }
        }
        return outputs;
    }

    /** 合并同种产物堆，避免返回大量零散堆。 */
    private static void mergeIntoOutputs(List<ItemStack> outputs, ItemStack toAdd) {
        for (ItemStack existing : outputs) {
            if (ItemStack.isSameItemSameComponents(existing, toAdd)) {
                int total = existing.getCount() + toAdd.getCount();
                int max = existing.getMaxStackSize();
                if (total <= max) {
                    existing.setCount(total);
                } else {
                    existing.setCount(max);
                    outputs.add(toAdd.copyWithCount(total - max));
                }
                return;
            }
        }
        outputs.add(toAdd);
    }

    // ==================== 输入输出操作 ====================

    /** 从 handler 中收集所有非空物品堆。 */
    private static List<ItemStack> collectInputs(IItemHandler handler) {
        int slots = handler.getSlots();
        List<ItemStack> inputs = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) inputs.add(stack);
        }
        return inputs;
    }

    /** 消耗 handler 中对应输入槽位的物品。 */
    private static void consumeInputs(IItemHandler handler, List<ItemStack> inputs,
                                      int[] required, int times) {
        for (int i = 0; i < inputs.size(); i++) {
            if (required[i] > 0) {
                int idx = findSlot(handler, inputs.get(i));
                if (idx >= 0) handler.extractItem(idx, required[i] * times, false);
            }
        }
    }

    /** 消耗单个物品堆。 */
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
}