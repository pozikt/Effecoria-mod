package com.effecoria.core.seal;

import com.effecoria.core.formula.BreathDebuffs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** One-shot action helpers for reactive seal programs. */
public final class SealProgramEffects {
    private SealProgramEffects() {}

    public static void hurtOnce(ServerLevel level, BlockPos pos, SealInstance seal, float damage) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            entity.hurt(level.damageSources().magic(), Math.max(0.5f, damage));
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    pos.getX() + 0.5,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.5,
                    4,
                    0.15,
                    0.05,
                    0.15,
                    0.01);
        }
    }

    public static void slowOnce(ServerLevel level, BlockPos pos, SealInstance seal, int amp) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.45, 0.25, 0.45);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, amp - 1)));
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, 40, 1));
        }
    }

    public static void pushOnce(ServerLevel level, BlockPos pos, SealInstance seal, float force) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.4, 0.2, 0.4);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, force, 0));
            entity.hurtMarked = true;
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    pos.getX() + 0.5,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5,
                    6,
                    0.2,
                    0.1,
                    0.2,
                    0.02);
        }
    }

    public static void applyStandingHurt(ServerLevel level, BlockPos pos, SealInstance seal, float damage) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
        }
    }

    public static void applyStandingSlow(ServerLevel level, BlockPos pos, SealInstance seal, int amp) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.45, 0.25, 0.45);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 25, Math.max(0, amp - 1)));
        }
    }

    public static void applyStandingPush(ServerLevel level, BlockPos pos, SealInstance seal, float force) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, force, 0));
            entity.hurtMarked = true;
        }
    }
}
