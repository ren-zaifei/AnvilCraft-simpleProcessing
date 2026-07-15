package com.renzaifei.asp.entity.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedCrushingTableBlockEntity extends SimpleBlockEntity{
    public AdvancedCrushingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static AdvancedCrushingTableBlockEntity createBlockEntity(
            BlockEntityType<AdvancedCrushingTableBlockEntity> type, BlockPos pos, BlockState state) {
        return new AdvancedCrushingTableBlockEntity(type, pos, state);
    }
}
