package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.EmeraldCanopyService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Emerald Canopy — ultramarine night sparks and green mist tint under the vault. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEmeraldCanopyEffects {
    private ClientEmeraldCanopyEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || player.tickCount % 3 != 0) {
            return;
        }
        if (!EmeraldCanopyService.isIn(level, player.position())) {
            return;
        }
        RandomSource random = level.random;
        double x = player.getX() + (random.nextDouble() - 0.5) * 12;
        double y = player.getY() + 1.0 + random.nextDouble() * 6;
        double z = player.getZ() + (random.nextDouble() - 0.5) * 12;
        level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0, 0.02, 0);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || !EmeraldCanopyService.isIn(level, player.position())) {
            return;
        }
        float night = level.isDay() ? 0.35f : 0.7f;
        event.setRed(event.getRed() * (1f - 0.35f * night) + 0.08f * night);
        event.setGreen(event.getGreen() * (1f - 0.2f * night) + 0.28f * night);
        event.setBlue(event.getBlue() * (1f - 0.15f * night) + 0.42f * night);
    }
}
