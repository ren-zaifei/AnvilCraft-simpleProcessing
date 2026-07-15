package com.renzaifei.asp.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.renzaifei.asp.entity.block.AdvancedCrushingTableBlockEntity;
import com.renzaifei.asp.entity.block.AdvancedStampingPlatformBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;


public class AdvancedCrushingTableRenderer extends SimpleRender<AdvancedCrushingTableBlockEntity>{

    protected AdvancedCrushingTableRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx , new Vec3(0.5, 0.95, 0.5));
    }

}