package com.effecoria.client;

import com.effecoria.network.ModNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import org.joml.Vector3f;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientTelegraphFx {
    private ClientTelegraphFx() {}

    public static void pulse(ModNetworking.TelegraphPulsePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.55f, 0.8f, 1.0f), 1.1f);
        spawn(level, payload.x1() + 0.5, payload.y1() + 0.9, payload.z1() + 0.5, dust);
        spawn(level, payload.x2() + 0.5, payload.y2() + 0.9, payload.z2() + 0.5, dust);
    }

    private static void spawn(Level level, double x, double y, double z, DustParticleOptions dust) {
        for (int i = 0; i < 8; i++) {
            level.addParticle(dust, x, y, z, (level.random.nextDouble() - 0.5) * 0.08, 0.02, (level.random.nextDouble() - 0.5) * 0.08);
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
        }
    }
}
