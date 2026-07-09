package com.renzaifei.asp.client.renderer;

import com.renzaifei.asp.entity.block.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RenderRegister {

    private RenderRegister() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerBlockEntityRenderer(
                        ModBlockEntities.ADVANCED_STAMPING_PLATFORM.get(),
                        AdvancedStampingPlatformRenderer::new));
    }
}