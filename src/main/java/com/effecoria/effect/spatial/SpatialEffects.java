package com.effecoria.effect.spatial;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpatialEffects {
    private SpatialEffects() {}

    public static void warpBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        target.hurt(SpellCombat.magic(caster), damage);
        target.hurtMarked = true;
        finishHit(level, target.position());
    }

    public static void spatialWard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        int absorb = effect.params().has("absorption_amplifier") ? effect.params().get("absorption_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, false, true));
        spawnSpatialParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void foldRepulse(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float force = effect.params().has("force") ? effect.params().get("force").getAsFloat() : 2.2f;
        Vec3 away = target.position().subtract(caster.position()).normalize();
        double strength = force * (power / 50f);
        target.setDeltaMovement(target.getDeltaMovement().add(away.scale(strength)));
        target.hurtMarked = true;
        float damage = DiceDamage.fromParams(effect.params(), power, 2f);
        target.hurt(SpellCombat.magic(caster), damage);
        finishHit(level, target.position());
    }

    public static void riftSlash(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 60;
        target.hurt(SpellCombat.magic(caster), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
        target.hurtMarked = true;
        finishHit(level, target.position());
    }

    public static void gravitySnare(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 80;
        float pull = effect.params().has("pull_strength") ? effect.params().get("pull_strength").getAsFloat() : 0.35f;
        Vec3 center = caster.position();
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 2));
            Vec3 toward = center.subtract(entity.position()).normalize().scale(pull);
            entity.setDeltaMovement(entity.getDeltaMovement().add(toward));
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        spawnSpatialParticles(level, center.add(0, 1, 0));
    }

    public static void gravityField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 8f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 180;
        float pull = params.has("pull_strength") ? params.get("pull_strength").getAsFloat() : 0.25f;
        float dps = params.has("damage_dice_per_round")
                ? SpatialFieldService.dpsFromParams(params, power)
                : 0f;
        SpatialFieldService.spawnGravityWell(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                pull,
                dps);
    }

    public static void dimensionalAnchor(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 5));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        finishHit(caster.serverLevel(), target.position());
    }

    public static void voidLance(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 9f);
        target.hurt(SpellCombat.magic(caster), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        target.hurtMarked = true;
        finishHit(level, target.position());
        level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    public static void warpExchange(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        Vec3 casterPos = caster.position();
        Vec3 targetPos = target.position();
        spawnSpatialParticles(level, casterPos.add(0, 1, 0));
        spawnSpatialParticles(level, targetPos.add(0, 1, 0));
        caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);
        caster.fallDistance = 0f;
        target.teleportTo(casterPos.x, casterPos.y, casterPos.z);
        target.hurtMarked = true;
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        target.hurt(SpellCombat.magic(caster), damage);
        level.playSound(null, caster.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.9f);
    }

    public static void spatialSurge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(caster), damage);
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        spawnSpatialParticles(level, caster.position().add(0, 1, 0));
    }

    public static void farBlink(ServerPlayer caster, SpellEffectEntry effect, float power) {
        blinkAlongLook(caster, effect, power, 1.0, defaultMaxRange(effect, 200));
    }

    public static void standardBlink(ServerPlayer caster, SpellEffectEntry effect, float power) {
        blinkAlongLook(caster, effect, power, 1.0, defaultMaxRange(effect, 24));
    }

    public static void riftBurst(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float damage = DiceDamage.fromParams(effect.params(), power, 7f);
        Vec3 center = target.position();
        hurtRadius(level, center, radius, damage, caster);
        spawnSpatialParticles(level, center.add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 0.5f);
    }

    public static void spatialSingularity(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 7f;
        float pull = effect.params().has("pull_strength") ? effect.params().get("pull_strength").getAsFloat() : 0.9f;
        float damage = DiceDamage.fromParams(effect.params(), power, 8f);
        Vec3 center = target.position();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            Vec3 toward = center.subtract(entity.position()).normalize().scale(pull);
            entity.setDeltaMovement(entity.getDeltaMovement().add(toward));
            entity.hurt(SpellCombat.magic(caster), damage);
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
        spawnSpatialParticles(level, center.add(0, 1, 0));
    }

    public static void absoluteFold(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int veilTicks = effect.params().has("veil_ticks") ? effect.params().get("veil_ticks").getAsInt() : 100;
        blinkAlongLook(caster, effect, power, 1.05, defaultMaxRange(effect, 220));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, veilTicks, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, veilTicks, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, veilTicks, 1, false, false, true));
    }

    /** Open / advance a subspace voyage gate. */
    public static void subspaceVoyage(ServerPlayer caster, SpellEffectEntry effect, float power) {
        SubspaceVoyageService.cast(caster);
        spawnSpatialParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    private static double defaultMaxRange(SpellEffectEntry effect, double fallback) {
        return effect.params().has("max_range") ? effect.params().get("max_range").getAsDouble() : fallback;
    }

    private static void blinkAlongLook(
            ServerPlayer caster, SpellEffectEntry effect, float power, double rangeScale, double maxCap) {
        ServerLevel level = caster.serverLevel();
        double range = effect.params().has("range") ? effect.params().get("range").getAsDouble() : 10;
        double minRange = effect.params().has("min_range") ? effect.params().get("min_range").getAsDouble() : 2;
        range = Math.min(maxCap, range * rangeScale * (0.85 + power / 120f));

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 origin = caster.position();
        Vec3 best = null;
        for (double dist = range; dist >= minRange; dist -= 0.5) {
            Vec3 candidate = origin.add(look.scale(dist));
            BlockPos feet = BlockPos.containing(candidate.x, candidate.y, candidate.z);
            BlockPos head = feet.above();
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
                continue;
            }
            if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                continue;
            }
            best = new Vec3(candidate.x, feet.getY(), candidate.z);
            break;
        }
        if (best == null) {
            return;
        }
        Vec3 from = origin.add(0, 1, 0);
        Vec3 to = best.add(0, 1, 0);
        spawnBlinkTrail(level, from, to);
        spawnSpatialParticles(level, from);
        caster.teleportTo(best.x, best.y, best.z);
        caster.fallDistance = 0f;
        spawnSpatialParticles(level, to);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.1f);
    }

    private static void spawnBlinkTrail(ServerLevel level, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        if (dist < 1.5) {
            return;
        }
        int steps = Math.min(48, Math.max(4, (int) (dist / 2.5)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = from.lerp(to, t);
            level.sendParticles(ModParticleTypes.SPATIAL_WARP.get(), p.x, p.y, p.z, 2, 0.08, 0.12, 0.08, 0.01);
            if (i % 2 == 0) {
                level.sendParticles(ModParticleTypes.SPATIAL_RIFT.get(), p.x, p.y, p.z, 1, 0.05, 0.08, 0.05, 0.02);
            }
        }
    }

    private static void hurtRadius(ServerLevel level, Vec3 center, float radius, float damage, ServerPlayer skip) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == skip) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(skip), damage);
            entity.hurtMarked = true;
            spawnSpatialParticles(level, entity.position().add(0, 1, 0));
        }
    }

    private static void finishHit(ServerLevel level, Vec3 pos) {
        spawnSpatialParticles(level, pos.add(0, 1, 0));
        level.playSound(null, BlockPos.containing(pos), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    public static void spawnSpatialParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.SPATIAL_RIFT.get(), pos.x, pos.y, pos.z, 16, 0.35, 0.5, 0.35, 0.03);
        level.sendParticles(ModParticleTypes.SPATIAL_WARP.get(), pos.x, pos.y, pos.z, 10, 0.25, 0.35, 0.25, 0.02);
    }
}
