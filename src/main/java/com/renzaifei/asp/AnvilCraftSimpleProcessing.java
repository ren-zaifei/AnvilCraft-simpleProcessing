package com.renzaifei.asp;

import com.mojang.logging.LogUtils;
import com.renzaifei.asp.block.ModBlocks;
import com.renzaifei.asp.entity.block.ModBlockEntities;
import com.renzaifei.asp.item.ModItemGroups;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AnvilCraftSimpleProcessing.MOD_ID)
public class AnvilCraftSimpleProcessing {
    public static final String MOD_ID = "anvilcraft_simple_processing";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Registrum REGISTRATE = Registrum.create(MOD_ID);

    public AnvilCraftSimpleProcessing(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register();
        ModBlockEntities.register();
        ModItemGroups.register(modEventBus);
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
