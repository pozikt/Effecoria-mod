package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.weather.PhiWeatherKind;
import com.effecoria.world.weather.PhiWeatherService;

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

/** Client particles and fog tint for Φ/Ω weather overlays. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPhiWeatherEffects {
    private ClientPhiWeatherEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()) {
            return;
        }
        PhiWeatherService.Snapshot snap = PhiWeatherService.clientSnapshot();
        PhiWeatherKind kind = snap.kind();
        if (kind == PhiWeatherKind.CLEAR || player.tickCount % 2 != 0) {
            return;
        }
        RandomSource random = level.random;
        double x = player.getX();
        double y = player.getY() + 1.4;
        double z = player.getZ();
        switch (kind) {
            case ESSENCE_RAIN, ESSENCE_DEW -> {
                for (int i = 0; i < 6; i++) {
                    level.addParticle(
                            ModParticleTypes.PHI_MIST.get(),
                            x + (random.nextDouble() - 0.5) * 8.0,
                            y + 2.0 + random.nextDouble() * 3.0,
                            z + (random.nextDouble() - 0.5) * 8.0,
                            0.0,
                            -0.25 - random.nextDouble() * 0.15,
                            0.0);
                }
            }
            case ESSENCE_STORM, ESSENCE_LIGHTNING -> {
                for (int i = 0; i < 8; i++) {
                    level.addParticle(
                            ModParticleTypes.PHI_SPARK.get(),
                            x + (random.nextDouble() - 0.5) * 10.0,
                            y + random.nextDouble() * 4.0,
                            z + (random.nextDouble() - 0.5) * 10.0,
                            (random.nextDouble() - 0.5) * 0.2,
                            0.05,
                            (random.nextDouble() - 0.5) * 0.2);
                }
            }
            case ESSENCE_TORNADO -> {
                for (int i = 0; i < 10; i++) {
                    double ang = random.nextDouble() * Math.PI * 2;
                    double r = 1.5 + random.nextDouble() * 4.0;
                    level.addParticle(
                            ModParticleTypes.PHI_GUST.get(),
                            x + Math.cos(ang) * r,
                            y + random.nextDouble() * 5.0,
                            z + Math.sin(ang) * r,
                            -Math.sin(ang) * 0.15,
                            0.12,
                            Math.cos(ang) * 0.15);
                }
            }
            case OMEGA_FOG, OMEGA_RAIN -> {
                for (int i = 0; i < 7; i++) {
                    // Drift against vanilla rain direction — slight +X bias.
                    level.addParticle(
                            ModParticleTypes.CORRUPTION_MIASMA.get(),
                            x + (random.nextDouble() - 0.5) * 9.0,
                            y + random.nextDouble() * 3.0,
                            z + (random.nextDouble() - 0.5) * 9.0,
                            0.08 + random.nextDouble() * 0.05,
                            -0.02,
                            (random.nextDouble() - 0.5) * 0.04);
                }
            }
            case BLOOD_RAIN -> {
                for (int i = 0; i < 8; i++) {
                    level.addParticle(
                            ModParticleTypes.CORRUPTION_BLOOD.get(),
                            x + (random.nextDouble() - 0.5) * 8.0,
                            y + 3.0 + random.nextDouble() * 2.0,
                            z + (random.nextDouble() - 0.5) * 8.0,
                            0.0,
                            -0.3,
                            0.0);
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        PhiWeatherKind kind = PhiWeatherService.clientSnapshot().kind();
        float intensity = Mth.clamp(PhiWeatherService.clientSnapshot().intensity(), 0f, 1f);
        if (kind == PhiWeatherKind.CLEAR || intensity <= 0.01f) {
            return;
        }
        float r = event.getRed();
        float g = event.getGreen();
        float b = event.getBlue();
        switch (kind) {
            case ESSENCE_RAIN, ESSENCE_MIST, ESSENCE_DEW -> {
                event.setRed(Mth.lerp(intensity * 0.45f, r, 0.45f));
                event.setGreen(Mth.lerp(intensity * 0.45f, g, 0.72f));
                event.setBlue(Mth.lerp(intensity * 0.45f, b, 0.95f));
            }
            case ESSENCE_STORM, ESSENCE_LIGHTNING, ESSENCE_TORNADO -> {
                event.setRed(Mth.lerp(intensity * 0.55f, r, 0.25f));
                event.setGreen(Mth.lerp(intensity * 0.55f, g, 0.45f));
                event.setBlue(Mth.lerp(intensity * 0.55f, b, 0.85f));
            }
            case OMEGA_FOG, OMEGA_RAIN -> {
                event.setRed(Mth.lerp(intensity * 0.7f, r, 0.08f));
                event.setGreen(Mth.lerp(intensity * 0.7f, g, 0.02f));
                event.setBlue(Mth.lerp(intensity * 0.7f, b, 0.12f));
            }
            case BLOOD_RAIN -> {
                event.setRed(Mth.lerp(intensity * 0.65f, r, 0.55f));
                event.setGreen(Mth.lerp(intensity * 0.65f, g, 0.05f));
                event.setBlue(Mth.lerp(intensity * 0.65f, b, 0.18f));
            }
            default -> {
            }
        }
    }
}
