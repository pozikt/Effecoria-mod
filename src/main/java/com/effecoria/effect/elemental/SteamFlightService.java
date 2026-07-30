package com.effecoria.effect.elemental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Steam/gas propulsion: elytra-style fall-flying with firework-like boosts and continuous Ψ drain.
 */
public final class SteamFlightService {
    private SteamFlightService() {}

    public static void activate(ServerPlayer player, float drainPerTick, float boostStrength) {
        PlayerPsiData data = PsiHelper.get(player);
        data.setSteamFlightActive(true);
        data.setSteamFlightDrainPerTick(Math.max(0.01f, drainPerTick));
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());

        if (!player.isFallFlying()) {
            // Jump off ground if needed so fall-flying can engage.
            if (player.onGround()) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, 0.85, 0));
                player.hurtMarked = true;
                player.hasImpulse = true;
            }
            player.startFallFlying();
        }
        applyFireworkStyleBoost(player, boostStrength);
    }

    /** Matches vanilla FireworkRocketEntity boost while fall-flying. */
    public static void applyFireworkStyleBoost(ServerPlayer player, float strength) {
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        double s = Math.max(0.6, strength);
        player.setDeltaMovement(motion.add(
                look.x * 0.1 + (look.x * s - motion.x) * 0.5,
                look.y * 0.1 + (look.y * s - motion.y) * 0.5,
                look.z * 0.1 + (look.z * s - motion.z) * 0.5));
        player.hurtMarked = true;
        player.hasImpulse = true;
    }

    public static void tick(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.steamFlightActive()) {
            return;
        }

        if (player.onGround() || player.isInWater() || player.isPassenger()) {
            stop(player, data);
            return;
        }

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        boolean god = CreativeGodMode.isActive(player);
        if (!god) {
            float drain = data.steamFlightDrainPerTick();
            if (data.currentPsi() < drain) {
                stop(player, data);
                return;
            }
            data.setCurrentPsi(data.currentPsi() - drain);
            PsiHelper.set(player, data);
            if (player.tickCount % 10 == 0) {
                player.syncData(ModAttachments.PSI.get());
            }
        }

        // Gentle continuous thrust so flight does not stall between boosts.
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.add(look.scale(0.035)));
        player.hurtMarked = true;

        if (player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
            Vec3 behind = player.position().add(look.scale(-0.8));
            level.sendParticles(
                    ModParticleTypes.STEAM_FOG.get(),
                    behind.x,
                    behind.y + 0.4,
                    behind.z,
                    4,
                    0.15,
                    0.12,
                    0.15,
                    0.01);
            if (player.tickCount % 20 == 0) {
                SteamCloudService.spawn(level, behind, 1.2f, 25, player.getUUID(), false);
            }
        }
    }

    public static void stop(ServerPlayer player, PlayerPsiData data) {
        data.setSteamFlightActive(false);
        data.setSteamFlightDrainPerTick(0f);
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }

    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (data.steamFlightActive()) {
            event.setCanceled(true);
            stop(player, data);
        }
    }
}
