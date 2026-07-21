package com.renzaifei.asp.util;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
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

    // ==================== 多合一锻造模板合成 ====================

    /**
     * 尝试将输入槽中不同种类的 {@code #anvilcraft:templates} 标签物品合成为多合一锻造模板。
     * 忽略非模板物品（保留在槽中不动），只消费带有该标签的物品。
     * <ul>
     *   <li>恰好 2 种不同模板物品 → 二合一锻造模板</li>
     *   <li>恰好 4 种不同模板物品 → 四合一锻造模板</li>
     *   <li>恰好 8 种不同模板物品 → 八合一锻造模板</li>
     * </ul>
     *
     * @param inputHandler 输入槽位（会直接修改）
     * @return 产物列表；不满足条件时返回空列表
     */
    public static List<ItemStack> tryCraftSmithingTemplate(
            IItemHandler inputHandler) {

        List<ItemStack> inputs = collectInputs(inputHandler);
        if (inputs.isEmpty()) return List.of();

        // 筛选出带有 #anvilcraft:templates 标签的物品，且种类各不相同
        List<ItemStack> templateStacks = new ArrayList<>();
        Set<Item> seenItems = new HashSet<>();
        for (ItemStack stack : inputs) {
            if (!stack.is(ModItemTags.TEMPLATES)) continue; // 跳过杂物
            if (!seenItems.add(stack.getItem())) return List.of(); // 有重复种类，不合成
            templateStacks.add(stack);
        }

        // 模板物品数量必须恰好为 2、4 或 8
        if (templateStacks.isEmpty()) return List.of();
        Item resultItem;
        switch (templateStacks.size()) {
            case 2 -> resultItem = ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get();
            case 4 -> resultItem = ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE.get();
            case 8 -> resultItem = ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE.get();
            default -> { return List.of(); }
        }

        // 只消耗模板物品（每种 1 个），杂物保留不动
        for (ItemStack stack : templateStacks) {
            extractSlot(inputHandler, stack, 1);
        }

        return List.of(new ItemStack(resultItem));
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