package com.renzaifei.asp.data;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModZhCnLangProvider extends LanguageProvider {
    public ModZhCnLangProvider(PackOutput output) {
        super(output, AnvilCraftSimpleProcessing.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add(ModBlocks.ADVANCED_STAMPING_PLATFORM.get(), "高级冲压平台");
        add(ModBlocks.ADVANCED_CRUSHING_TABLE.get(), "高级粉碎台");
        add(ModBlocks.ADVANCED_UNPACK_TABLE.get(), "高级解包台");
        add(ModBlocks.ADVANCED_MESH_TABLE.get(), "高级过筛台");

        add("itemGroup.anvilcraft_simple_processing.anvilcraft_simple_processing_items" , "铁砧工艺：简易加工");

    }
}
