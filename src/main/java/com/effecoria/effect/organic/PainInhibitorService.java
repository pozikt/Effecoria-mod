package com.effecoria.effect.organic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Timed pain-inhibitor buff: HP can drop without knockback / hurt-stagger feedback. */
public final class PainInhibitorService {
    public static final String UNTIL = "effecoria:pain_inhibitor_until";
    private static final String PRE_VX = "effecoria:pain_pre_vx";
    private static final String PRE_VY = "effecoria:pain_pre_vy";
    private static final String PRE_VZ = "effecoria:pain_pre_vz";
    private static final String PRE_MARK = "effecoria:pain_pre_mark";

    private PainInhibitorService() {}

    public static void activate(LivingEntity entity, int durationTicks) {
        long until = entity.level().getGameTime() + Math.max(1, durationTicks);
        entity.getPersistentData().putLong(UNTIL, until);
    }

    public static boolean isActive(LivingEntity entity) {
        return entity.getPersistentData().getLong(UNTIL) > entity.level().getGameTime();
    }

    /** Snapshot motion before vanilla hurt applies knockback. */
    public static void captureMotion(LivingEntity entity) {
        Vec3 m = entity.getDeltaMovement();
        var data = entity.getPersistentData();
        data.putDouble(PRE_VX, m.x);
        data.putDouble(PRE_VY, m.y);
        data.putDouble(PRE_VZ, m.z);
        data.putBoolean(PRE_MARK, true);
    }

    /** Restore pre-hit motion and clear hurt stagger animation. */
    public static void suppressHitFeedback(LivingEntity entity) {
        var data = entity.getPersistentData();
        if (data.getBoolean(PRE_MARK)) {
            entity.setDeltaMovement(data.getDouble(PRE_VX), data.getDouble(PRE_VY), data.getDouble(PRE_VZ));
            entity.hasImpulse = true;
            entity.hurtMarked = true;
            data.putBoolean(PRE_MARK, false);
        }
        clearStagger(entity);
    }

    public static void clearStagger(LivingEntity entity) {
        entity.hurtTime = 0;
        entity.hurtDuration = 0;
    }
}
