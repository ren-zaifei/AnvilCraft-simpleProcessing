package com.renzaifei.asp.block;

import com.renzaifei.asp.block.interfaces.ISimpleEntityBlock;
import com.renzaifei.asp.entity.block.AdvancedUnpackTableBlockEntity;
import com.renzaifei.asp.entity.block.ModBlockEntities;
import com.renzaifei.asp.entity.block.SimpleBlockEntity;
import dev.dubhe.anvilcraft.block.CrushingTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class AdvancedUnpackTableBlock extends CrushingTableBlock implements ISimpleEntityBlock<AdvancedUnpackTableBlockEntity> {
    public AdvancedUnpackTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<AdvancedUnpackTableBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ADVANCED_UNPACK_TABLE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedUnpackTableBlockEntity(getBlockEntityType(), pos, state);
    }

    @Override
    public IItemHandler getInputItemHandler(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AdvancedUnpackTableBlockEntity be) {
            return be.getInputItemHandler(null);
        }
        return null;
    }

    @Override
    public IItemHandler getOutputItemHandler(Level level, BlockPos pos) {
        return null;
    }

    @Override
    @Nullable
    public BlockEntityTicker<AdvancedUnpackTableBlockEntity> getBlockEntityTicker() {
        return AdvancedUnpackTableBlockEntity::serverTick;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack handStack, BlockState state,
                                           Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        return ISimpleEntityBlock.super.useItemOn(handStack, state, level, pos, player, hand, hit);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        ISimpleEntityBlock.super.onRemove(state, level, pos, newState, movedByPiston);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return SimpleBlockEntity.getComparatorSignal(level, pos);
    }
}
