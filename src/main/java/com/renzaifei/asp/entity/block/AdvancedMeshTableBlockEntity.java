package com.renzaifei.asp.entity.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedMeshTableBlockEntity extends SimpleBlockEntity{
    public AdvancedMeshTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static AdvancedMeshTableBlockEntity createBlockEntity(
            BlockEntityType<AdvancedMeshTableBlockEntity> type, BlockPos pos, BlockState state) {
        return new AdvancedMeshTableBlockEntity(type, pos, state);
    }
}
