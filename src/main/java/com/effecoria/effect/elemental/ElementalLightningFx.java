package com.effecoria.effect.elemental;

import com.effecoria.network.ModNetworking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server → client slanted lightning arcs (hand → strike). */
public final class ElementalLightningFx {
    private ElementalLightningFx() {}

    public static void playArc(ServerLevel level, Vec3 from, Vec3 to, float intensity, int durationTicks) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                to.x,
                to.y,
                to.z,
                64.0,
                new ModNetworking.LightningArcFxPayload(
                        from.x, from.y, from.z, to.x, to.y, to.z, intensity, durationTicks));
    }

    public static void playFromHand(ServerPlayer caster, Vec3 strike, float intensity) {
        Vec3 hand = ElementalEffects.handCastOrigin(caster);
        playArc(caster.serverLevel(), hand, strike, intensity, 10);
    }
}
