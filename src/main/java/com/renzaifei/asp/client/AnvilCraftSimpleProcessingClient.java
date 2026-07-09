package com.renzaifei.asp.client;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.client.renderer.RenderRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = AnvilCraftSimpleProcessing.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftSimpleProcessingClient {
    public AnvilCraftSimpleProcessingClient(IEventBus modBus, ModContainer container) {
        RenderRegister.register(modBus);
    }
}