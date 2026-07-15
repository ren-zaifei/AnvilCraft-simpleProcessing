package com.renzaifei.asp.data;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModEnUsLangProvider extends LanguageProvider {
    public ModEnUsLangProvider(PackOutput output) {
        super(output, AnvilCraftSimpleProcessing.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(ModBlocks.ADVANCED_STAMPING_PLATFORM.get(), "Advanced Stamping Platform");
        add(ModBlocks.ADVANCED_CRUSHING_TABLE.get(), "Advanced Crushing Table");
        add(ModBlocks.ADVANCED_UNPACK_TABLE.get(), "Advanced Unpack Table");
        add(ModBlocks.ADVANCED_MESH_TABLE.get(), "Advanced Mesh Table");

        add("itemGroup.anvilcraft_simple_processing.anvilcraft_simple_processing_items" , "AnvilCraft : SimpleProcessing");

    }
}
