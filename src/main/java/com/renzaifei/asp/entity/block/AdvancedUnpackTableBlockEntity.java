package com.renzaifei.asp.entity.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedUnpackTableBlockEntity extends SimpleBlockEntity{
    public AdvancedUnpackTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState , 4 , 0);
    }

    public static AdvancedUnpackTableBlockEntity createBlockEntity(
            BlockEntityType<AdvancedUnpackTableBlockEntity> type, BlockPos pos, BlockState state) {
        return new AdvancedUnpackTableBlockEntity(type, pos, state);
    }
}
