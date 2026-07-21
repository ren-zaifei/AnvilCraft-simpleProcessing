package com.renzaifei.asp.entity.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


public class AdvancedStampingPlatformBlockEntity extends SimpleBlockEntity {

    public AdvancedStampingPlatformBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState , 8 , 0);
    }

    public static AdvancedStampingPlatformBlockEntity createBlockEntity(
            BlockEntityType<AdvancedStampingPlatformBlockEntity> type, BlockPos pos, BlockState state) {
        return new AdvancedStampingPlatformBlockEntity(type, pos, state);
    }

    @Override
    public boolean canInteractFromSide(@Nullable Direction side) {
        return true;
    }
}
