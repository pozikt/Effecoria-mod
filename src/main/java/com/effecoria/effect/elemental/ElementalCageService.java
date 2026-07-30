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
import net.minecraft.world.phys.Vec3;

/**
 * Timed elemental cages: water prison (drowning shell) and vacuum (asphyxiating air void).
 */
public final class ElementalCageService {
    private static final List<Cage> CAGES = new ArrayList<>();

    public enum Kind {
        WATER,
        VACUUM
    }

    private ElementalCageService() {}

    private record Cage(
            Kind kind,
            UUID targetId,
            UUID casterId,
            Vec3 center,
            float radius,
            long untilTick,
            float tickDamage) {}

    public static void imprisonWater(
            ServerLevel level, LivingEntity target, UUID casterId, float radius, int durationTicks, float damagePerSecond) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int duration = Math.max(40, durationTicks);
        placeWaterShell(level, BlockPos.containing(center), Math.max(1, Math.round(radius)), duration);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 5, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, true, true));
        CAGES.add(new Cage(
                Kind.WATER,
                target.getUUID(),
                casterId,
                center,
                radius,
                level.getGameTime() + duration,
                damagePerSecond / 20f));
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

    public static void imprisonVacuum(
            ServerLevel level, LivingEntity target, UUID casterId, float radius, int durationTicks, float damagePerSecond) {
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int duration = Math.max(40, durationTicks);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 5, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, Math.min(40, duration / 2), 0, false, true, true));
        CAGES.add(new Cage(
                Kind.VACUUM,
                target.getUUID(),
                casterId,
                center,
                radius,
                level.getGameTime() + duration,
                damagePerSecond / 20f));
        level.playSound(null, target.blockPosition(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1f, 0.55f);
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                center.x,
                center.y,
                center.z,
                30,
                radius * 0.35,
                radius * 0.35,
                radius * 0.35,
                0.02);
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
            Entity entity = level.getEntity(cage.targetId());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                it.remove();
                continue;
            }

            // Pull back toward the prison center if they try to leave.
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

            if (cage.tickDamage() > 0f && now % 10 == 0) {
                DamageSource source = switch (cage.kind()) {
                    case WATER -> level.damageSources().drown();
                    case VACUUM -> level.damageSources().magic();
                };
                target.hurt(source, cage.tickDamage() * 10f);
            }

            if (now % 4 == 0) {
                spawnCageFx(level, cage);
            }
        }
    }

    private static void placeWaterShell(ServerLevel level, BlockPos center, int radius, int durationTicks) {
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int manh = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    // Hollow-ish shell / filled core so the head stays submerged.
                    boolean shell = manh >= radius && manh <= radius + 1;
                    boolean core = Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1;
                    if (!shell && !core) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    ElementalBlockService.placeTemporary(level, pos, water, durationTicks);
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
                        ModParticleTypes.WATER_DROP.get(),
                        c.x,
                        c.y,
                        c.z,
                        6,
                        r * 0.4,
                        r * 0.4,
                        r * 0.4,
                        0.02);
                level.sendParticles(
                        ModParticleTypes.WATER_WAVE.get(),
                        c.x,
                        c.y,
                        c.z,
                        2,
                        r * 0.3,
                        0.1,
                        r * 0.3,
                        0.01);
            }
            case VACUUM -> {
                level.sendParticles(
                        ModParticleTypes.PHI_GUST.get(),
                        c.x,
                        c.y,
                        c.z,
                        5,
                        r * 0.35,
                        r * 0.35,
                        r * 0.35,
                        0.03);
                level.sendParticles(
                        ParticleTypes.SMOKE,
                        c.x,
                        c.y,
                        c.z,
                        3,
                        r * 0.25,
                        r * 0.25,
                        r * 0.25,
                        0.01);
            }
        }
    }
}
