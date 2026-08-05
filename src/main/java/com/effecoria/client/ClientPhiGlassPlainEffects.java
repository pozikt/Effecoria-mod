package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.PhiGlassPlainService;
import com.effecoria.world.PhiGlassStormService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Ochre–teal Φ-haze for the Glass Plain; thickens during storms. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPhiGlassPlainEffects {
    private static float immersion;
    private static float stormImmersion;

    private ClientPhiGlassPlainEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            immersion = 0f;
            stormImmersion = 0f;
            return;
        }
        Level level = minecraft.level;
        boolean inside = PhiGlassPlainService.isIn(level, player.position());
        float storm = inside ? PhiGlassStormService.clientStormIntensity(level, player.blockPosition()) : 0f;
        immersion = Mth.lerp(0.12f, immersion, inside ? 0.7f : 0f);
        stormImmersion = Mth.lerp(0.1f, stormImmersion, storm);
        if (immersion < 0.02f) {
            immersion = 0f;
            return;
        }

        if (inside && player.tickCount % 2 == 0) {
            RandomSource random = level.random;
            int count = stormImmersion > 0.5f ? 8 : 3;
            for (int i = 0; i < count; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 16.0;
                double y = player.getY() + random.nextDouble() * 4.0 + 0.4;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 16.0;
                level.addParticle(
                        ParticleTypes.END_ROD,
                        x,
                        y,
                        z,
                        (random.nextDouble() - 0.5) * 0.04,
                        0.02,
                        (random.nextDouble() - 0.5) * 0.04);
            }
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (immersion <= 0.01f) {
            return;
        }
        float farTarget = Mth.lerp(stormImmersion, 48f, 22f);
        float far = Mth.lerp(immersion, event.getFarPlaneDistance(), farTarget);
        float near = Mth.lerp(immersion, event.getNearPlaneDistance(), 2.0f);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(Math.max(near + 6f, far));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (immersion <= 0.01f) {
            return;
        }
        // Ochre / teal horizon haze
        float r = Mth.lerp(stormImmersion, 0.78f, 0.55f);
        float g = Mth.lerp(stormImmersion, 0.62f, 0.48f);
        float b = Mth.lerp(stormImmersion, 0.42f, 0.62f);
        event.setRed(Mth.lerp(immersion, event.getRed(), r));
        event.setGreen(Mth.lerp(immersion, event.getGreen(), g));
        event.setBlue(Mth.lerp(immersion, event.getBlue(), b));
    }
}
