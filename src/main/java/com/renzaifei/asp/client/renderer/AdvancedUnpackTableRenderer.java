package com.renzaifei.asp.client.renderer;

import com.renzaifei.asp.entity.block.AdvancedUnpackTableBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class AdvancedUnpackTableRenderer extends SimpleRender<AdvancedUnpackTableBlockEntity>{
    protected AdvancedUnpackTableRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx , new Vec3(0.5, 0.95, 0.5));
    }
}
