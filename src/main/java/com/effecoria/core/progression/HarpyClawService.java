package com.effecoria.core.progression;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.config.BalanceConfig;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Innate harpy claws — iron-spear-like jab baseline via attribute, plus charge damage
 * that scales with relative speed (melee swings and glide dives).
 */
public final class HarpyClawService {
    /** Per-target dive hit cooldowns: attacker → (victim → gameTime of last hit). */
    private static final Map<UUID, Map<UUID, Long>> DIVE_HITS = new ConcurrentHashMap<>();
    /** True while applying a glide dive hit so melee speed bonus is not stacked twice. */
    private static final ThreadLocal<Boolean> DIVE_HIT = ThreadLocal.withInitial(() -> false);

    private HarpyClawService() {}

    public static boolean isDiveHit() {
        return Boolean.TRUE.equals(DIVE_HIT.get());
    }

    /** Relative speed in blocks/second. */
    public static double relativeSpeedBlocksPerSec(Entity attacker, Entity target) {
        Vec3 rel = attacker.getDeltaMovement().subtract(target.getDeltaMovement());
        return rel.length() * 20.0;
    }

    /**
     * Iron-spear-style charge bonus: {@code factor × relative speed (b/s)}.
     * Zero when nearly still relative to the target.
     */
    public static float speedBonusDamage(ServerPlayer attacker, LivingEntity target) {
        double speed = relativeSpeedBlocksPerSec(attacker, target);
        double min = BalanceConfig.HARPY_CLAW_MIN_SPEED_BPS.get();
        if (speed < min) {
            return 0f;
        }
        float factor = BalanceConfig.HARPY_CLAW_SPEED_FACTOR.get().floatValue();
        float bonus = (float) (speed * factor);
        float max = BalanceConfig.HARPY_CLAW_SPEED_BONUS_CAP.get().floatValue();
        if (max > 0f) {
            bonus = Math.min(bonus, max);
        }
        return bonus;
    }

    /** Glide/dive collision: ram living entities ahead while fall-flying fast. */
    public static void tickDive(ServerPlayer player) {
        if (!HarpyFlightService.isHarpy(player) || !player.isFallFlying() || player.level().isClientSide()) {
            return;
        }
        double minTickSpeed = BalanceConfig.HARPY_DIVE_MIN_SPEED.get();
        if (player.getDeltaMovement().length() < minTickSpeed) {
            return;
        }

        double reach = BalanceConfig.HARPY_DIVE_REACH.get();
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        AABB box = player.getBoundingBox().inflate(reach * 0.35, reach * 0.2, reach * 0.35).expandTowards(look.scale(reach));

        long now = player.level().getGameTime();
        int cooldown = BalanceConfig.HARPY_DIVE_HIT_COOLDOWN_TICKS.get();
        Map<UUID, Long> hits = DIVE_HITS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());

        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive() && player.hasLineOfSight(e))) {
            Long last = hits.get(target.getUUID());
            if (last != null && now - last < cooldown) {
                continue;
            }
            // Prefer targets roughly in front.
            Vec3 to = target.getEyePosition().subtract(eye);
            if (to.lengthSqr() < 1.0E-4 || look.dot(to.normalize()) < 0.35) {
                continue;
            }

            float damage = speedBonusDamage(player, target);
            float jabFloor = BalanceConfig.HARPY_CLAW_DIVE_FLOOR.get().floatValue();
            damage = Math.max(damage, jabFloor);
            if (damage < 0.5f) {
                continue;
            }

            hits.put(target.getUUID(), now);
            DIVE_HIT.set(true);
            boolean hurt;
            try {
                hurt = target.hurt(player.damageSources().playerAttack(player), damage);
            } finally {
                DIVE_HIT.set(false);
            }
            if (hurt && player.level() instanceof ServerLevel level) {
                Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.25, 0.2, 0.15);
                level.playSound(
                        null,
                        at.x,
                        at.y,
                        at.z,
                        SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS,
                        0.7f,
                        1.15f);
            }
        }

        // Prune stale cooldown entries.
        if (player.tickCount % 100 == 0) {
            hits.entrySet().removeIf(e -> now - e.getValue() > 200);
        }
    }

    public static void clear(ServerPlayer player) {
        DIVE_HITS.remove(player.getUUID());
    }
}
