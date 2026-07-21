package com.renzaifei.asp.event;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.block.ModBlocks;
import com.renzaifei.asp.entity.block.AdvancedStampingPlatformBlockEntity;
import com.renzaifei.asp.entity.block.SimpleBlockEntity;
import com.renzaifei.asp.util.RecipeUtil;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
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
        if (hitBlockState.is(ModBlocks.ADVANCED_CRUSHING_TABLE.get())) {
            generateResult(level, hitBlockPos, ModRecipeTypes.ITEM_CRUSH_TYPE.get());
        }
        if (hitBlockState.is(ModBlocks.ADVANCED_UNPACK_TABLE.get())) {
            generateResult(level, hitBlockPos,ModRecipeTypes.UNPACK_TYPE.get());
        }
        if (hitBlockState.is(ModBlocks.ADVANCED_MESH_TABLE.get())) {
            generateResult(level, hitBlockPos,ModRecipeTypes.MESH_TYPE.get());
        }
    }

    private static void generateResult(Level level , BlockPos hitBlockPos , RecipeType<?> RecipeType) {
        var be = (SimpleBlockEntity) level.getBlockEntity(hitBlockPos);
        if (be == null) return;
        List<ItemStack> outputs = RecipeUtil.craft(
                RecipeType, be.getInputItemHandler(null),
                (ServerLevel) level);
        if (outputs.isEmpty()) return;
        AnvilUtil.dropItems(outputs, level, new Vec3(hitBlockPos.getX() + 0.5, hitBlockPos.getY(), hitBlockPos.getZ() + 0.5));
    }

    private static void onAdvancedStampingPlatform(Level level, BlockPos hitBlockPos,
                                                    BlockState hitBlockState) {
        var be = (AdvancedStampingPlatformBlockEntity) level.getBlockEntity(hitBlockPos);
        if (be == null) return;
        var serverLevel = (ServerLevel) level;
        List<ItemStack> outputs = RecipeUtil.craft(
                ModRecipeTypes.STAMPING_TYPE.get(), be.getInputItemHandler(null),
                serverLevel);
        if (outputs.isEmpty()) {
            outputs = RecipeUtil.tryCraftSmithingTemplate(
                    be.getInputItemHandler(null));
        }
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