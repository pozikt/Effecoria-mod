package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.VitrifiedWastesService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Blue-gold Φ haze for the Vitrified Wastes. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientVitrifiedWastesEffects {
    private static float immersion;

    private ClientVitrifiedWastesEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            immersion = 0f;
            return;
        }
        Level level = minecraft.level;
        boolean inside = VitrifiedWastesService.isIn(level, player.position());
        float stormBoost = VitrifiedWastesService.isStormActive(level) ? 0.15f : 0f;
        float target = inside ? 0.78f + stormBoost : 0f;
        immersion = Mth.lerp(0.1f, immersion, target);
        if (immersion < 0.02f) {
            immersion = 0f;
            return;
        }

        if (inside && player.tickCount % 2 == 0) {
            RandomSource random = level.random;
            int count = VitrifiedWastesService.isStormActive(level) ? 8 : 4;
            for (int i = 0; i < count; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 16.0;
                double y = player.getY() + random.nextDouble() * 4.0;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 16.0;
                level.addParticle(
                        ModParticleTypes.PHI_SPARK.get(),
                        x,
                        y,
                        z,
                        (random.nextDouble() - 0.5) * 0.01,
                        0.02 + random.nextDouble() * 0.02,
                        (random.nextDouble() - 0.5) * 0.01);
            }
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (immersion <= 0.01f) {
            return;
        }
        float far = Mth.lerp(immersion, event.getFarPlaneDistance(), 36f);
        float near = Mth.lerp(immersion, event.getNearPlaneDistance(), 2.0f);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(Math.max(near + 4f, far));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (immersion <= 0.01f) {
            return;
        }
        // Deep indigo with gold bleed
        event.setRed(Mth.lerp(immersion, event.getRed(), 0.12f));
        event.setGreen(Mth.lerp(immersion, event.getGreen(), 0.10f));
        event.setBlue(Mth.lerp(immersion, event.getBlue(), 0.22f));
    }
}
