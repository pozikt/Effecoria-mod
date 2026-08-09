package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.gui.RaceSelectScreen;
import com.effecoria.client.gui.SchoolSelectScreen;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * First-join UX: race (mandatory) → school (deferrable).
 * Also prompts race for legacy saves that already initiated without a race.
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
        var data = PsiHelper.get(minecraft.player);
        boolean needsRace = data.race().isEmpty();
        boolean needsSchool = !data.initiated() && !data.schoolChoiceDeferred();
        if (!needsRace && !needsSchool) {
            pendingPrompt = false;
            return;
        }
        if (!pendingPrompt) {
            return;
        }
        if (++delayTicks < 25) {
            return;
        }
        if (minecraft.screen instanceof RaceSelectScreen || minecraft.screen instanceof SchoolSelectScreen) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        if (needsRace) {
            // Open school after race only if school is still needed.
            minecraft.setScreen(new RaceSelectScreen(true, needsSchool));
        } else {
            minecraft.setScreen(new SchoolSelectScreen(true));
        }
    }
}
