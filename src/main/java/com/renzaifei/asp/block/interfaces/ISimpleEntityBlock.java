package com.renzaifei.asp.block.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public interface ISimpleEntityBlock<T extends BlockEntity> extends EntityBlock {


    /** 方块对应的 BlockEntityType。通常为 {@code ModBlockEntities.XXX.get()}。 */
    BlockEntityType<T> getBlockEntityType();

    /** 创建 BlockEntity 实例。 */
    @Override
    BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    /** 获取指定位置的输入物品处理器。无有效 BE 时返回 {@code null}。 */
    IItemHandler getInputItemHandler(Level level, BlockPos pos);
    /** 获取指定位置的输出物品处理器。无有效 BE 时返回 {@code null}。 */
    IItemHandler getOutputItemHandler(Level level, BlockPos pos);

    /**
     * 返回此方块的 ticker 回调。
     * 若方块不需要每 tick 逻辑，返回 {@code null} 即可跳过。
     */
    @Nullable
    default BlockEntityTicker<T> getBlockEntityTicker() {
        return null;
    }


    /**
     * 根据 {@link #getBlockEntityType()} 自动匹配 ticker。
     * <p>
     * 一般无需覆写。若 {@link #getBlockEntityTicker()} 返回 {@code null} 则不 tick。
     */
    @SuppressWarnings("unchecked")
    default <E extends BlockEntity> BlockEntityTicker<E> getTicker(
            Level level, BlockState state, BlockEntityType<E> type) {
        if (level.isClientSide) return null;
        if (type == getBlockEntityType()) {
            BlockEntityTicker<T> ticker = getBlockEntityTicker();
            if (ticker != null) {
                return (BlockEntityTicker<E>) ticker;
            }
        }
        return null;
    }


    /**
     * 处理玩家右键交互 —— 空手取出物品，手持物品尝试存入。
     *
     * @param handStack 玩家手持物品堆（可能为空）
     * @param state     方块当前状态
     * @param level     当前世界
     * @param pos       方块坐标
     * @param player    交互的玩家
     * @param hand      交互手
     * @param hit       射线命中结果
     * @return 交互结果，决定后续行为（成功 / 透传原版处理）
     */
    default ItemInteractionResult useItemOn(ItemStack handStack, BlockState state,
                                            Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        IItemHandler inputHandler = getInputItemHandler(level, pos);
        IItemHandler outputHandler = getOutputItemHandler(level, pos);
        if (outputHandler != null && hasAnyItem(outputHandler)) {
            for (int i = 0; i < outputHandler.getSlots(); i++) {
                ItemStack inSlot = outputHandler.getStackInSlot(i);
                if (!inSlot.isEmpty()) {
                    ItemStack taken = outputHandler.extractItem(i, inSlot.getCount(), false);
                    if (!player.getInventory().add(taken)) {
                        player.drop(taken, false);
                    }
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (inputHandler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (handStack.isEmpty()) {
            for (int i = inputHandler.getSlots() - 1; i >= 0; i--) {
                ItemStack taken = inputHandler.extractItem(i,
                        inputHandler.getStackInSlot(i).getCount(), false);
                if (!taken.isEmpty()) {
                    player.setItemInHand(hand, taken);
                    return ItemInteractionResult.SUCCESS;
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack remaining = ItemHandlerHelper.insertItem(inputHandler, handStack.copy(), false);
        if (remaining.getCount() != handStack.getCount()) {
            player.setItemInHand(hand, remaining);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 检查 IItemHandler 中是否有任意非空槽位。 */
    private static boolean hasAnyItem(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    /**
     * 方块被替换或破坏时，将槽位内物品以 <b>单个</b> {@link ItemEntity} 形式掉落在方块中心。
     *
     * @param state         方块旧状态
     * @param level         当前世界
     * @param pos           方块坐标
     * @param newState      方块新状态
     * @param movedByPiston 是否由活塞推动
     */
    default void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) return;
        dropAllItems(getInputItemHandler(level, pos), level, pos);
        dropAllItems(getOutputItemHandler(level, pos), level, pos);
    }

    /** 将 handler 中所有槽位的物品以单堆形式掉落在方块中心。 */
    private static void dropAllItems(IItemHandler handler, Level level, BlockPos pos) {
        if (handler == null) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.extractItem(i, 64, false);
            if (!stack.isEmpty()) {
                ItemEntity entity = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        stack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
            }
        }
    }
}
