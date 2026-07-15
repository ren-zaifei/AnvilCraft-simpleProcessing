package com.renzaifei.asp.client.renderer;

import com.renzaifei.asp.entity.block.AdvancedMeshTableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class AdvancedMeshTableRenderer extends SimpleRender<AdvancedMeshTableBlockEntity>{
    protected AdvancedMeshTableRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx , new Vec3(0.5, 0.95, 0.5));
    }
}
