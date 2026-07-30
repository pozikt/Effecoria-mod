package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.gui.SchoolSelectScreen;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opens school selection when joining a world while not yet initiated.
 * See docs/MAGIC_PLAN.md — first-join UX.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientFirstJoinSchoolPrompt {
    private static boolean pendingPrompt;
    private static int delayTicks;

    private ClientFirstJoinSchoolPrompt() {}

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        pendingPrompt = true;
        delayTicks = 0;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        pendingPrompt = false;
        delayTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (PsiHelper.get(minecraft.player).initiated()) {
            pendingPrompt = false;
            return;
        }
        if (!pendingPrompt) {
            return;
        }
        if (++delayTicks < 25) {
            return;
        }
        if (minecraft.screen instanceof SchoolSelectScreen) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        minecraft.setScreen(new SchoolSelectScreen(true));
    }
}
