package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.gui.ShellDeathScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Swap vanilla death UI for the shell-protocol cinematic (hardcore keeps vanilla). */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ShellDeathEvents {
    private ShellDeathEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void replaceDeathScreen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof DeathScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getLevelData().isHardcore()) {
            return;
        }
        event.setNewScreen(new ShellDeathScreen());
    }
}
