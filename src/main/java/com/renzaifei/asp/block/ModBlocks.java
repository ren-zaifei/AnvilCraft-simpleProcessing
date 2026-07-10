package com.renzaifei.asp.block;

import com.renzaifei.asp.entity.block.AdvancedCrushingTableBlockEntity;
import com.renzaifei.asp.entity.block.AdvancedStampingPlatformBlockEntity;
import com.renzaifei.asp.item.ModItemGroups;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CrushingTableBlock;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import static com.renzaifei.asp.AnvilCraftSimpleProcessing.REGISTRATE;

public class ModBlocks {

    static {
        REGISTRATE.defaultCreativeTab(ModItemGroups.MOD_ITEMS.getKey());
    }

    public static void register() {}

    public static final BlockEntry<? extends Block> ADVANCED_STAMPING_PLATFORM = REGISTRATE
            .block("advanced_stamping_platform", AdvancedStampingPlatformBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.isValidSpawn(Blocks::never))
            .blockstate(DataGenUtil::horizontalFacingBlock)
            .simpleItem()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .recipe((ctx, provider) -> {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ctx.get())
                        .pattern("CAC")
                        .pattern("B B")
                        .pattern("B B")
                        .define('A', ModItemTags.MAGNET_INGOTS)
                        .define('B', ModItemTags.BRASS_INGOTS)
                        .define('C', ItemTags.PLANKS)
                        .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.MAGNET_INGOTS), AnvilCraftDatagen.has(ModItemTags.MAGNET_INGOTS))
                        .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.BRASS_INGOTS), AnvilCraftDatagen.has(ModItemTags.BRASS_INGOTS))
                        .save(provider);
            })
            .simpleBlockEntity(AdvancedStampingPlatformBlockEntity::new)
            .register();


    public static final BlockEntry<? extends Block> ADVANCED_CRUSHING_TABLE = REGISTRATE
            .block("advanced_crushing_table", AdvancedCrushingTableBlock::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.isValidSpawn(Blocks::never))
            .blockstate(DataGenUtil::noExtraModelOrState)
            .simpleItem()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .recipe((ctx, provider) -> {
                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ctx.get())
                        .requires(ModBlocks.ADVANCED_STAMPING_PLATFORM)
                        .requires(Items.GRINDSTONE)
                        .unlockedBy("has_" + Items.GRINDSTONE, AnvilCraftDatagen.has(Items.GRINDSTONE))
                        .save(provider);
            })
            .register();
}
