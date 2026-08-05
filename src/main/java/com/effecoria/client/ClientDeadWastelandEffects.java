package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.DeadWastelandService;

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

/** Gray ash haze for the Dead Wasteland — no blue/gold Φ tint. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientDeadWastelandEffects {
    private static float immersion;

    private ClientDeadWastelandEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            immersion = 0f;
            return;
        }
        Level level = minecraft.level;
        boolean inside = DeadWastelandService.isIn(level, player.position());
        float target = inside ? 0.82f : 0f;
        immersion = Mth.lerp(0.12f, immersion, target);
        if (immersion < 0.02f) {
            immersion = 0f;
            return;
        }

        if (inside && player.tickCount % 3 == 0) {
            RandomSource random = level.random;
            for (int i = 0; i < 4; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 14.0;
                double y = player.getY() + random.nextDouble() * 3.5 + 0.5;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 14.0;
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.WHITE_ASH,
                        x,
                        y,
                        z,
                        (random.nextDouble() - 0.5) * 0.02,
                        0.01,
                        (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (immersion <= 0.01f) {
            return;
        }
        float far = Mth.lerp(immersion, event.getFarPlaneDistance(), 28f);
        float near = Mth.lerp(immersion, event.getNearPlaneDistance(), 1.5f);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(Math.max(near + 4f, far));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (immersion <= 0.01f) {
            return;
        }
        // Bleached gray — no blue / gold
        event.setRed(Mth.lerp(immersion, event.getRed(), 0.62f));
        event.setGreen(Mth.lerp(immersion, event.getGreen(), 0.60f));
        event.setBlue(Mth.lerp(immersion, event.getBlue(), 0.55f));
    }
}
