package com.renzaifei.asp.event;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.block.ModBlocks;
import com.renzaifei.asp.entity.block.AdvancedStampingPlatformBlockEntity;
import com.renzaifei.asp.util.RecipeUtil;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraftSimpleProcessing.MOD_ID)
public class AnvilEvent {

    @SubscribeEvent
    public static void onLand(dev.dubhe.anvilcraft.api.event.AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (level.getServer() == null) return;
        final BlockPos hitBlockPos = pos.below();
        final BlockState hitBlockState = level.getBlockState(hitBlockPos);
        if (hitBlockState.is(ModBlocks.ADVANCED_STAMPING_PLATFORM.get())) {
            onAdvancedStampingPlatform(level, hitBlockPos, hitBlockState);
        }
    }

    private static void onAdvancedStampingPlatform(Level level, BlockPos hitBlockPos,
                                                    BlockState hitBlockState) {
        var be = (AdvancedStampingPlatformBlockEntity) level.getBlockEntity(hitBlockPos);
        if (be == null) return;
        List<ItemStack> outputs = RecipeUtil.craft(
                ModRecipeTypes.STAMPING_TYPE.get(), be.getInputItemHandler(null));
        if (outputs.isEmpty()) return;
        var facing = hitBlockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos frontPos = hitBlockPos.relative(facing);
        Vec3 spawnPos = new Vec3(
                frontPos.getX() + 0.5 - facing.getStepX() * 0.3,
                frontPos.getY() + 0.625,
                frontPos.getZ() + 0.5 - facing.getStepZ() * 0.3
        );
        AnvilUtil.dropItems(outputs, level, spawnPos);
    }
}