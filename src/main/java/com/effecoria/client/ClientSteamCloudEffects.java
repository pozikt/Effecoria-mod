package com.effecoria.client;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.effect.elemental.SteamCloudService;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client-side steam fog: density fog while inside a cloud + local particle fill. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSteamCloudEffects {
    private static final List<SteamCloudService.CloudSnapshot> CLOUDS = new ArrayList<>();

    private ClientSteamCloudEffects() {}

    public static void setClouds(List<SteamCloudService.CloudSnapshot> clouds) {
        CLOUDS.clear();
        if (clouds != null) {
            CLOUDS.addAll(clouds);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        long now = minecraft.level.getGameTime();
        CLOUDS.removeIf(c -> c.expireAt() <= now);

        if (minecraft.player.tickCount % 3 != 0) {
            return;
        }

        Vec3 eye = minecraft.player.getEyePosition(1f);
        SteamCloudService.CloudSnapshot inside = findContaining(eye, now);
        if (inside == null) {
            return;
        }

        double x = inside.x();
        double y = inside.y();
        double z = inside.z();
        float r = inside.radius();
        var mode = inside.mode();
        for (int i = 0; i < 5; i++) {
            double px = x + (minecraft.level.random.nextDouble() - 0.5) * r * 1.4;
            double py = y + (minecraft.level.random.nextDouble() - 0.35) * r;
            double pz = z + (minecraft.level.random.nextDouble() - 0.5) * r * 1.4;
            if (mode == SteamCloudService.Mode.FROST && i % 2 == 0) {
                minecraft.level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, px, py, pz, 0, 0.01, 0);
            } else {
                minecraft.level.addParticle(ModParticleTypes.STEAM_FOG.get(), px, py, pz, 0, 0.015, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        Vec3 eye = minecraft.player.getEyePosition((float) event.getPartialTick());
        SteamCloudService.CloudSnapshot inside = findContaining(eye, now);
        if (inside == null) {
            return;
        }

        float radius = Math.max(1f, inside.radius());
        float depth = (float) Math.sqrt(eye.distanceToSqr(inside.x(), inside.y(), inside.z()));
        float edge = Mth.clamp(1f - depth / radius, 0.25f, 1f);

        float near = 0.15f;
        float far = Mth.lerp(edge, 8f, 2.2f);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        Vec3 eye = minecraft.player.getEyePosition((float) event.getPartialTick());
        SteamCloudService.CloudSnapshot inside = findContaining(eye, now);
        if (inside == null) {
            return;
        }
        switch (inside.mode()) {
            case SCALDING -> {
                event.setRed(0.82f);
                event.setGreen(0.72f);
                event.setBlue(0.68f);
            }
            case FROST -> {
                event.setRed(0.62f);
                event.setGreen(0.78f);
                event.setBlue(0.92f);
            }
            default -> {
                event.setRed(0.78f);
                event.setGreen(0.82f);
                event.setBlue(0.86f);
            }
        }
    }

    private static SteamCloudService.CloudSnapshot findContaining(Vec3 pos, long now) {
        SteamCloudService.CloudSnapshot best = null;
        float bestEdge = -1f;
        for (SteamCloudService.CloudSnapshot cloud : CLOUDS) {
            if (cloud.expireAt() <= now) {
                continue;
            }
            double distSq = pos.distanceToSqr(cloud.x(), cloud.y(), cloud.z());
            float r = cloud.radius();
            if (distSq > (double) r * r) {
                continue;
            }
            float edge = 1f - (float) Math.sqrt(distSq) / r;
            if (edge > bestEdge) {
                bestEdge = edge;
                best = cloud;
            }
        }
        return best;
    }
}
