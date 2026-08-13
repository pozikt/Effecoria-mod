package com.effecoria.client.hud;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class TowerHealthHudEvents {
    private TowerHealthHudEvents() {}

    @SubscribeEvent
    public static void cancelVanillaHearts(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();
        if (VanillaGuiLayers.PLAYER_HEALTH.equals(name) && TowerHealthHud.shouldReplaceVanillaHearts()) {
            event.setCanceled(true);
        }
    }
}
