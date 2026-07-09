package com.renzaifei.asp.event;

import com.renzaifei.asp.AnvilCraftSimpleProcessing;
import com.renzaifei.asp.util.RecipeUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = AnvilCraftSimpleProcessing.MOD_ID)
public final class RecipeCacheListener {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RecipeUtil.rebuild(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        RecipeUtil.rebuild(event.getRecipeManager());
    }
}