package com.effecoria.effect.elemental;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Timed elemental cages: water / ice prisons and vacuum voids.
 */
public final class ElementalCageService {
    private static final List<Cage> CAGES = new ArrayList<>();

    public enum Kind {
        WATER,
        VACUUM,
        ICE
    }

    private ElementalCageService() {}

    /**
     * @param targetId primary pinned target; for VACUUM may be null when the cage is pure AoE
     */
    private record Cage(
            Kind kind,
            UUID targetId,
            UUID casterId,
            Vec3 center,
            float radius,
            long untilTick,
            float damagePerSecond,
            boolean aoe) {}

    public static void imprisonWater(
            ServerLevel level, LivingEntity target, UUID casterId, float radius, int durationTicks, float damagePerSecond) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int duration = Math.max(40, durationTicks);
        placeWaterShell(level, BlockPos.containing(center), Math.max(1, Math.round(radius)), duration);
        pinTarget(target, duration);
        CAGES.add(new Cage(
                Kind.WATER,
                target.getUUID(),
                casterId,
                center,
                radius,
                level.getGameTime() + duration,
                damagePerSecond,
                false));
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1f, 0.7f);
        level.sendParticles(
                ModParticleTypes.WATER_SPLASH.get(),
                center.x,
                center.y,
                center.z,
                24,
                radius * 0.4,
                radius * 0.4,
                radius * 0.4,
                0.04);
    }

    public static void imprisonIce(
            ServerLevel level, LivingEntity target, UUID casterId, float radius, int durationTicks, float damagePerSecond) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int duration = Math.max(40, durationTicks);
        int r = Math.max(1, Math.round(radius));
        placeIceShell(level, BlockPos.containing(center), r, duration);
        pinTarget(target, duration);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6, false, true, true));
        CAGES.add(new Cage(
                Kind.ICE,
                target.getUUID(),
                casterId,
                center,
                radius,
                level.getGameTime() + duration,
                damagePerSecond,
                false));
        level.playSound(null, target.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
        ElementalEffects.spawnIceParticles(level, center);
    }

    /** Absolute vacuum sphere — damages and pins everyone inside the radius. */
    public static void imprisonVacuumAoE(
            ServerLevel level, Vec3 center, UUID casterId, float radius, int durationTicks, float damagePerSecond) {
        int duration = Math.max(40, durationTicks);
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1, false, true, true));
        }
        CAGES.add(new Cage(
                Kind.VACUUM,
                null,
                casterId,
                center,
                radius,
                level.getGameTime() + duration,
                damagePerSecond,
                true));
        level.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.BREEZE_WHIRL,
                SoundSource.PLAYERS,
                1f,
                0.55f);
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                center.x,
                center.y,
                center.z,
                40,
                radius * 0.4,
                radius * 0.4,
                radius * 0.4,
                0.03);
    }

    /** Legacy single-target vacuum — still creates an AoE sphere around the target. */
    public static void imprisonVacuum(
            ServerLevel level, LivingEntity target, UUID casterId, float radius, int durationTicks, float damagePerSecond) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        imprisonVacuumAoE(level, center, casterId, radius, durationTicks, damagePerSecond);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Cage> it = CAGES.iterator();
        while (it.hasNext()) {
            Cage cage = it.next();
            if (now > cage.untilTick()) {
                it.remove();
                continue;
            }

            if (cage.aoe()) {
                tickAoeCage(level, cage, now);
            } else {
                if (!tickPinnedCage(level, cage, now)) {
                    it.remove();
                }
            }
        }
    }

    private static boolean tickPinnedCage(ServerLevel level, Cage cage, long now) {
        Entity entity = level.getEntity(cage.targetId());
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return false;
        }
        restrainTowardCenter(target, cage);
        if (cage.damagePerSecond() > 0f && now % 10 == 0) {
            DamageSource source = switch (cage.kind()) {
                case WATER -> level.damageSources().drown();
                case ICE -> level.damageSources().freeze();
                case VACUUM -> level.damageSources().magic();
            };
            target.hurt(source, cage.damagePerSecond() * 0.5f);
        }
        if (now % 4 == 0) {
            spawnCageFx(level, cage);
        }
        return true;
    }

    private static void tickAoeCage(ServerLevel level, Cage cage, long now) {
        AABB box = new AABB(cage.center(), cage.center()).inflate(cage.radius());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (cage.casterId() != null && target.getUUID().equals(cage.casterId())) {
                continue;
            }
            restrainTowardCenter(target, cage);
            if (cage.damagePerSecond() > 0f && now % 10 == 0) {
                target.hurt(level.damageSources().magic(), cage.damagePerSecond() * 0.5f);
                target.setAirSupply(Math.max(-20, target.getAirSupply() - 40));
            }
        }
        if (now % 4 == 0) {
            spawnCageFx(level, cage);
        }
    }

    private static void restrainTowardCenter(LivingEntity target, Cage cage) {
        Vec3 mid = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 offset = mid.subtract(cage.center());
        double max = cage.radius() * 0.85;
        if (offset.lengthSqr() > max * max) {
            Vec3 clamped = cage.center().add(offset.normalize().scale(max * 0.7));
            target.teleportTo(clamped.x, clamped.y - target.getBbHeight() * 0.5, clamped.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
        } else {
            Vec3 pull = cage.center().subtract(mid).scale(0.08);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;
        }
        target.fallDistance = 0f;
    }

    private static void pinTarget(LivingEntity target, int duration) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 5, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, true, true));
    }

    private static void placeWaterShell(ServerLevel level, BlockPos center, int radius, int durationTicks) {
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int manh = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    boolean shell = manh >= radius && manh <= radius + 1;
                    boolean core = Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1;
                    if (!shell && !core) {
                        continue;
                    }
                    ElementalBlockService.placeTemporary(level, center.offset(dx, dy, dz), water, durationTicks);
                }
            }
        }
    }

    private static void placeIceShell(ServerLevel level, BlockPos center, int radius, int durationTicks) {
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int manh = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    // Hollow cocoon 3x3-ish — leave interior air so target is visible but trapped.
                    if (manh < radius || manh > radius + 1) {
                        continue;
                    }
                    ElementalBlockService.placeTemporary(level, center.offset(dx, dy, dz), ice, durationTicks);
                }
            }
        }
    }

    private static void spawnCageFx(ServerLevel level, Cage cage) {
        Vec3 c = cage.center();
        float r = cage.radius();
        switch (cage.kind()) {
            case WATER -> {
                level.sendParticles(
                        ModParticleTypes.WATER_DROP.get(), c.x, c.y, c.z, 6, r * 0.4, r * 0.4, r * 0.4, 0.02);
                level.sendParticles(
                        ModParticleTypes.WATER_WAVE.get(), c.x, c.y, c.z, 2, r * 0.3, 0.1, r * 0.3, 0.01);
            }
            case ICE -> ElementalEffects.spawnIceParticles(level, c);
            case VACUUM -> {
                level.sendParticles(
                        ModParticleTypes.PHI_GUST.get(), c.x, c.y, c.z, 5, r * 0.35, r * 0.35, r * 0.35, 0.03);
                level.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 3, r * 0.25, r * 0.25, r * 0.25, 0.01);
            }
        }
    }
}
