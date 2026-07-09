package com.renzaifei.asp.entity.block;

import com.renzaifei.asp.block.ModBlocks;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static com.renzaifei.asp.AnvilCraftSimpleProcessing.REGISTRATE;

public class ModBlockEntities {
    public static final BlockEntityEntry<AdvancedStampingPlatformBlockEntity> ADVANCED_STAMPING_PLATFORM = REGISTRATE
            .blockEntity("advanced_stamping_platform", AdvancedStampingPlatformBlockEntity::createBlockEntity)
            .validBlock(ModBlocks.ADVANCED_STAMPING_PLATFORM)
            .register();

    public static void register() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ADVANCED_STAMPING_PLATFORM.get(),
                AdvancedStampingPlatformBlockEntity::getInputItemHandler
        );
    }
}
