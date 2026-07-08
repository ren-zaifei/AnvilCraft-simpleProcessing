package com.renzaifei.asp.item;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.renzaifei.asp.AnvilCraftSimpleProcessing.REGISTRATE;

public class ModItemGroups {

    private static final DeferredRegister<CreativeModeTab> DEFERRED_REGISTER = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            AnvilCraftSimpleProcessing.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MOD_ITEMS = DEFERRED_REGISTER.register(
            "anvilcraft_simple_processing_items",
            () -> CreativeModeTab.builder()
                    .icon(ModBlocks.BRASS_STAMPING_PLATFORM::asStack)
                    .displayItems((ctx, entries) -> {
                    })
                    .title(
                            REGISTRATE.addLang(
                                    "itemGroup",
                                    AnvilCraftSimpleProcessing.of("anvilcraft_simple_processing_items"),
                                    "AnvilCraft SimpleProcessing"
                            )
                    )
                    .withTabsBefore(dev.dubhe.anvilcraft.init.item.ModItemGroups.ANVILCRAFT_BUILD_BLOCK.getId())
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        DEFERRED_REGISTER.register(modEventBus);
    }
}
