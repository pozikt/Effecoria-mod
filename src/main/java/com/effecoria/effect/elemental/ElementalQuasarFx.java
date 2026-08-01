package com.effecoria.effect.elemental;

import com.effecoria.network.ModNetworking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server → client Veil routing for the elemental Quasar field. */
public final class ElementalQuasarFx {
    private ElementalQuasarFx() {}

    public static void playSpawn(ServerLevel level, Vec3 center, float radius, int durationTicks) {
        float intensity = 1.15f;
        // Veil uses a fixed ~1-block core; gameplay radius stays for particles/terrain.
        float visualCore = 1.0f;
        int clientLife = Math.min(durationTicks, 160);
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                64.0,
                new ModNetworking.QuasarFxPayload(
                        center.x, center.y, center.z, intensity, visualCore, clientLife, false));
    }

    public static void playPulse(ServerLevel level, Vec3 center, float radius) {
        float intensity = 1.15f;
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                64.0,
                new ModNetworking.QuasarFxPayload(center.x, center.y, center.z, intensity, 1.0f, 40, true));
    }
}
