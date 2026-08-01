package com.effecoria.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Look-based cast resolution: optional living under crosshair + always a world aim point. */
public final class CastAim {
    private CastAim() {}

    public record Result(Vec3 point, @Nullable LivingEntity living, @Nullable BlockPos block) {}

    public static Result resolve(ServerPlayer caster, double range) {
        LivingEntity living = raycastLiving(caster, range);
        if (living == null) {
            living = livingInCone(caster, range, 0.65);
        }

        HitResult blockHit = caster.pick(range, 0f, false);
        BlockPos block = null;
        Vec3 point;
        if (blockHit.getType() == HitResult.Type.BLOCK && blockHit instanceof BlockHitResult bhr) {
            block = bhr.getBlockPos();
            // Aim at the face center so bolts hit the surface, not block origin.
            point = Vec3.atCenterOf(block).subtract(Vec3.atLowerCornerOf(bhr.getDirection().getNormal()).scale(0.45));
        } else if (living != null) {
            point = living.getBoundingBox().getCenter();
        } else {
            point = caster.getEyePosition().add(caster.getLookAngle().normalize().scale(range));
        }

        // Prefer living position when we actually locked a creature.
        if (living != null) {
            point = living.getBoundingBox().getCenter();
        }
        return new Result(point, living, block);
    }

    @Nullable
    public static LivingEntity raycastLiving(ServerPlayer caster, double range) {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        AABB search = caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                caster.level(),
                caster,
                start,
                end,
                search,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && living != caster
                        && !living.isSpectator());
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Nullable
    public static LivingEntity livingInCone(ServerPlayer caster, double range, double minDot) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 eye = caster.getEyePosition();
        AABB box = new AABB(eye, eye).inflate(range);
        LivingEntity best = null;
        double bestDist = range + 1;
        for (LivingEntity entity : caster.serverLevel().getEntitiesOfClass(
                LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator())) {
            Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eye);
            double dist = toEntity.length();
            if (dist > range || dist < 0.5) {
                continue;
            }
            double dot = toEntity.normalize().dot(look);
            if (dot < minDot) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }
}
