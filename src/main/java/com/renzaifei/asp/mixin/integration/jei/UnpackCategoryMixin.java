package com.renzaifei.asp.mixin.integration.jei;

import com.renzaifei.asp.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.UnpackCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UnpackCategory.class)
public class UnpackCategoryMixin {
    @Inject(
            method = "registerRecipeCatalysts",
            at = @At("TAIL")
    )
    private static void onRegisterRecipeCatalysts(IRecipeCatalystRegistration registration, CallbackInfo ci) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ADVANCED_UNPACK_TABLE), AnvilCraftJeiPlugin.UNPACK);
    }
}
