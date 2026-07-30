package com.effecoria.effect.spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Gravity wells and spatial distortion zones. */
public final class SpatialFieldService {
    private static final List<GravityWell> WELLS = new CopyOnWriteArrayList<>();

    private static final class GravityWell {
        ServerLevel level;
        Vec3 center;
        float radius;
        long expireAt;
        UUID owner;
        float pullStrength;
        float damagePerSecond;
    }

    private SpatialFieldService() {}

    public static void spawnGravityWell(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float pullStrength,
            float damagePerSecond) {
        GravityWell well = new GravityWell();
        well.level = level;
        well.center = center;
        well.radius = Math.max(1f, radius);
        well.expireAt = level.getGameTime() + Math.max(1, durationTicks);
        well.owner = owner;
        well.pullStrength = Math.max(0.02f, pullStrength);
        well.damagePerSecond = Math.max(0f, damagePerSecond);
        WELLS.add(well);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 0.5f);
    }

    public static void clearFor(UUID owner) {
        WELLS.removeIf(w -> owner.equals(w.owner));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<GravityWell> toRemove = new ArrayList<>();
        for (GravityWell well : WELLS) {
            if (well.level != level) {
                continue;
            }
            if (now >= well.expireAt) {
                toRemove.add(well);
                continue;
            }
            if (now % 20 == 0) {
                tickWell(level, well);
            }
            if (now % 8 == 0) {
                level.sendParticles(
                        ModParticleTypes.SPATIAL_WARP.get(),
                        well.center.x,
                        well.center.y + 0.5,
                        well.center.z,
                        8,
                        well.radius * 0.35,
                        0.4,
                        well.radius * 0.35,
                        0.02);
            }
        }
        WELLS.removeAll(toRemove);
    }

    private static void tickWell(ServerLevel level, GravityWell well) {
        AABB box = new AABB(well.center, well.center).inflate(well.radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity instanceof ServerPlayer player && player.getUUID().equals(well.owner)) {
                continue;
            }
            if (entity.position().distanceToSqr(well.center) > (double) well.radius * well.radius) {
                continue;
            }
            Vec3 pull = well.center.subtract(entity.position()).normalize().scale(well.pullStrength);
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
            entity.hurtMarked = true;
            if (well.damagePerSecond > 0f) {
                entity.hurt(level.damageSources().magic(), well.damagePerSecond);
            }
        }
    }

    public static float dpsFromParams(com.google.gson.JsonObject params, float power) {
        return DiceDamage.perSecondFromParams(params, power, 1f);
    }
}
