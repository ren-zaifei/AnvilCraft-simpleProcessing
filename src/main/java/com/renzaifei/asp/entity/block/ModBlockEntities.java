package com.renzaifei.asp.entity.block;

import com.renzaifei.asp.block.ModBlocks;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static com.renzaifei.asp.AnvilCraftSimpleProcessing.REGISTRATE;

public class ModBlockEntities {


    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ADVANCED_STAMPING_PLATFORM.get(),
                SimpleBlockEntity::getCombinedItemHandler
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ADVANCED_CRUSHING_TABLE.get(),
                SimpleBlockEntity::getCombinedItemHandler
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ADVANCED_UNPACK_TABLE.get(),
                SimpleBlockEntity::getCombinedItemHandler
        );

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ADVANCED_MESH_TABLE.get(),
                SimpleBlockEntity::getCombinedItemHandler
        );
    }

    public static final BlockEntityEntry<AdvancedStampingPlatformBlockEntity> ADVANCED_STAMPING_PLATFORM = REGISTRATE
            .blockEntity("advanced_stamping_platform", AdvancedStampingPlatformBlockEntity::createBlockEntity)
            .validBlock(ModBlocks.ADVANCED_STAMPING_PLATFORM)
            .register();

    public static final BlockEntityEntry<AdvancedCrushingTableBlockEntity> ADVANCED_CRUSHING_TABLE = REGISTRATE
            .blockEntity("advanced_crushing_table", AdvancedCrushingTableBlockEntity::createBlockEntity)
            .validBlock(ModBlocks.ADVANCED_CRUSHING_TABLE)
            .register();

    public static final BlockEntityEntry<AdvancedUnpackTableBlockEntity> ADVANCED_UNPACK_TABLE = REGISTRATE
            .blockEntity("advanced_unpack_table", AdvancedUnpackTableBlockEntity::createBlockEntity)
            .validBlock(ModBlocks.ADVANCED_UNPACK_TABLE)
            .register();

    public static final BlockEntityEntry<AdvancedMeshTableBlockEntity> ADVANCED_MESH_TABLE = REGISTRATE
            .blockEntity("advanced_mesh_table", AdvancedMeshTableBlockEntity::createBlockEntity)
            .validBlock(ModBlocks.ADVANCED_MESH_TABLE)
            .register();

    public static void register() {
    }
}
