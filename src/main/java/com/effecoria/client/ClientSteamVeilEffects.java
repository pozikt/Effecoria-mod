package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSteamVeilEffects {
    private ClientSteamVeilEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (minecraft.player.tickCount % 4 != 0) {
            return;
        }

        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        long gameTime = minecraft.level.getGameTime();
        if (!data.isSteamVeilActive(gameTime)) {
            return;
        }

        double x = minecraft.player.getX();
        double y = minecraft.player.getY() + 1.0;
        double z = minecraft.player.getZ();
        for (int i = 0; i < 4; i++) {
            minecraft.level.addParticle(
                    ModParticleTypes.STEAM_FOG.get(),
                    x + (minecraft.level.random.nextDouble() - 0.5) * 1.2,
                    y + minecraft.level.random.nextDouble() * 0.8,
                    z + (minecraft.level.random.nextDouble() - 0.5) * 1.2,
                    0,
                    0.01,
                    0);
        }
    }
}
