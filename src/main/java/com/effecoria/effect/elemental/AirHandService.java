package com.effecoria.effect.elemental;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Wind-formed hand that grabs a living target and drags it along the caster's look. */
public final class AirHandService {
    private static final Map<UUID, Grab> GRABS = new ConcurrentHashMap<>();

    private AirHandService() {}

    private record Grab(UUID targetId, long untilTick, float holdDistance, float drainPerTick, boolean hadNoGravity) {}

    public static boolean isHolding(ServerPlayer caster) {
        return GRABS.containsKey(caster.getUUID());
    }

    /** @return true if a grab was started or released successfully */
    public static boolean toggleOrGrab(
            ServerPlayer caster, LivingEntity target, float holdDistance, int durationTicks, float drainPerSecond) {
        if (isHolding(caster)) {
            release(caster);
            return true;
        }
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target == caster && !com.effecoria.core.formula.BreathDebuffs.allowSelfTarget()) {
            return false;
        }
        float drainPerTick = Math.max(0.02f, drainPerSecond / 20f);
        long until = caster.level().getGameTime() + Math.max(20, durationTicks);
        boolean hadNoGravity = target.isNoGravity();
        target.setNoGravity(true);
        target.fallDistance = 0f;
        GRABS.put(caster.getUUID(), new Grab(target.getUUID(), until, holdDistance, drainPerTick, hadNoGravity));

        ServerLevel level = caster.serverLevel();
        level.playSound(null, caster.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 0.9f, 1.35f);
        spawnWind(level, target.position().add(0, target.getBbHeight() * 0.5, 0));
        return true;
    }

    public static void release(ServerPlayer caster) {
        Grab grab = GRABS.remove(caster.getUUID());
        if (grab == null) {
            return;
        }
        Entity entity = caster.serverLevel().getEntity(grab.targetId());
        if (entity instanceof LivingEntity living) {
            living.setNoGravity(grab.hadNoGravity());
            living.hurtMarked = true;
        }
        caster.serverLevel().playSound(
                null, caster.blockPosition(), SoundEvents.BREEZE_LAND, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    public static void clearFor(ServerPlayer caster) {
        release(caster);
    }

    public static void tick(ServerPlayer caster) {
        Grab grab = GRABS.get(caster.getUUID());
        if (grab == null) {
            return;
        }

        ServerLevel level = caster.serverLevel();
        long now = level.getGameTime();
        Entity entity = level.getEntity(grab.targetId());
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || now > grab.untilTick()) {
            release(caster);
            return;
        }

        boolean god = CreativeGodMode.isActive(caster);
        if (!god) {
            PlayerPsiData data = PsiHelper.get(caster);
            float drain = grab.drainPerTick();
            if (data.currentPsi() < drain) {
                release(caster);
                return;
            }
            data.setCurrentPsi(data.currentPsi() - drain);
            PsiHelper.set(caster, data);
            if (caster.tickCount % 10 == 0) {
                caster.syncData(ModAttachments.PSI.get());
            }
        }

        float dist = grab.holdDistance();
        Vec3 desired = caster.getEyePosition().add(caster.getLookAngle().normalize().scale(dist));
        // Keep a little clear of the caster's body.
        Vec3 fromCaster = desired.subtract(caster.position());
        if (fromCaster.lengthSqr() < 1.2 * 1.2) {
            desired = caster.position().add(caster.getLookAngle().normalize().scale(1.4));
            desired = new Vec3(desired.x, caster.getEyeY() - 0.2, desired.z);
        }

        Vec3 delta = desired.subtract(target.position());
        // Smooth drag — strong enough to feel like a hand, soft enough not to rubber-band wildly.
        target.setDeltaMovement(delta.scale(0.55));
        target.hurtMarked = true;
        target.hasImpulse = true;
        target.fallDistance = 0f;
        target.setNoGravity(true);

        if (caster.tickCount % 2 == 0) {
            spawnWind(level, target.position().add(0, target.getBbHeight() * 0.5, 0));
        }
    }

    private static void spawnWind(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.PHI_GUST.get(), pos.x, pos.y, pos.z, 4, 0.25, 0.25, 0.25, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.01);
    }
}
