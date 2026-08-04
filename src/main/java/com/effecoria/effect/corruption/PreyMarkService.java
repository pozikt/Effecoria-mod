package com.effecoria.effect.corruption;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** Timed prey mark: hostiles in a large radius forcibly chase the marked living. */
public final class PreyMarkService {
    public static final String UNTIL = "effecoria:prey_mark_until";
    public static final String CASTER = "effecoria:prey_mark_caster";
    public static final String RADIUS = "effecoria:prey_mark_radius";
    public static final int DEFAULT_DURATION_TICKS = 500;
    public static final float DEFAULT_HUNT_RADIUS = 64f;
    public static final int RETARGET_INTERVAL = 20;

    private PreyMarkService() {}

    public static void activate(LivingEntity target, UUID casterId, int durationTicks, float huntRadius) {
        long until = target.level().getGameTime() + Math.max(1, durationTicks);
        var data = target.getPersistentData();
        data.putLong(UNTIL, until);
        data.putFloat(RADIUS, Math.max(8f, huntRadius));
        if (casterId != null) {
            data.putUUID(CASTER, casterId);
        }
    }

    public static boolean isMarked(LivingEntity entity) {
        return entity.getPersistentData().getLong(UNTIL) > entity.level().getGameTime();
    }

    public static float huntRadius(LivingEntity entity) {
        float r = entity.getPersistentData().getFloat(RADIUS);
        return r > 0f ? r : DEFAULT_HUNT_RADIUS;
    }

    /** Force nearby hostiles onto the marked prey. */
    public static void forceHunt(LivingEntity marked) {
        if (!(marked.level() instanceof ServerLevel level) || !isMarked(marked) || !marked.isAlive()) {
            return;
        }
        float radius = huntRadius(marked);
        double r2 = radius * radius;
        AABB box = marked.getBoundingBox().inflate(radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, PreyMarkService::isHostileHunter)) {
            if (mob == marked) {
                continue;
            }
            if (mob.distanceToSqr(marked) > r2) {
                continue;
            }
            if (mob.getTarget() != marked) {
                mob.setTarget(marked);
            }
        }
    }

    public static boolean isHostileHunter(Mob mob) {
        return mob.isAlive() && mob instanceof Enemy;
    }

    /**
     * If a hunter tries to look away while a marked prey is in range, snap back.
     *
     * @return true if the event should keep / force the marked target
     */
    public static LivingEntity redirectTarget(Mob hunter, LivingEntity proposed) {
        if (!isHostileHunter(hunter) || !(hunter.level() instanceof ServerLevel level)) {
            return proposed;
        }
        float bestR = DEFAULT_HUNT_RADIUS;
        LivingEntity best = null;
        AABB box = hunter.getBoundingBox().inflate(DEFAULT_HUNT_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, PreyMarkService::isMarked)) {
            if (living == hunter) {
                continue;
            }
            float r = huntRadius(living);
            if (hunter.distanceToSqr(living) > r * r) {
                continue;
            }
            if (best == null || hunter.distanceToSqr(living) < hunter.distanceToSqr(best)) {
                best = living;
                bestR = r;
            }
        }
        if (best == null) {
            return proposed;
        }
        if (proposed == best) {
            return proposed;
        }
        // Prefer marked prey over any other target while in hunt radius.
        if (hunter.distanceToSqr(best) <= bestR * bestR) {
            return best;
        }
        return proposed;
    }
}
