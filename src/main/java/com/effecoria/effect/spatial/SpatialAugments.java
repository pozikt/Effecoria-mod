package com.effecoria.effect.spatial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/** Timed spatial buffs stored on living entities (lens, cocoon, wall-walk, time-loop). */
public final class SpatialAugments {
    public static final String LENS_UNTIL = "effecoria:spatial_lens_until";
    public static final String COCOON_UNTIL = "effecoria:spatial_cocoon_until";
    public static final String WALL_UNTIL = "effecoria:spatial_wall_until";
    public static final String LOOP_UNTIL = "effecoria:spatial_loop_until";
    public static final String LOOP_X = "effecoria:spatial_loop_x";
    public static final String LOOP_Y = "effecoria:spatial_loop_y";
    public static final String LOOP_Z = "effecoria:spatial_loop_z";
    public static final String LOOP_YAW = "effecoria:spatial_loop_yaw";
    public static final String LOOP_PITCH = "effecoria:spatial_loop_pitch";

    private SpatialAugments() {}

    public static void setLens(LivingEntity entity, long untilGameTime) {
        entity.getPersistentData().putLong(LENS_UNTIL, untilGameTime);
    }

    public static boolean hasLens(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(LENS_UNTIL) > gameTime;
    }

    public static void setCocoon(LivingEntity entity, long untilGameTime) {
        entity.getPersistentData().putLong(COCOON_UNTIL, untilGameTime);
    }

    public static boolean hasCocoon(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(COCOON_UNTIL) > gameTime;
    }

    public static void setWallWalk(LivingEntity entity, long untilGameTime) {
        entity.getPersistentData().putLong(WALL_UNTIL, untilGameTime);
    }

    public static boolean hasWallWalk(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(WALL_UNTIL) > gameTime;
    }

    public static void beginTimeLoop(LivingEntity entity, long untilGameTime) {
        CompoundTag data = entity.getPersistentData();
        data.putLong(LOOP_UNTIL, untilGameTime);
        data.putDouble(LOOP_X, entity.getX());
        data.putDouble(LOOP_Y, entity.getY());
        data.putDouble(LOOP_Z, entity.getZ());
        data.putFloat(LOOP_YAW, entity.getYRot());
        data.putFloat(LOOP_PITCH, entity.getXRot());
    }

    public static boolean hasTimeLoop(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(LOOP_UNTIL) > gameTime;
    }

    public static void tickTimeLoop(LivingEntity entity, long gameTime) {
        if (!hasTimeLoop(entity, gameTime)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        double x = data.getDouble(LOOP_X);
        double y = data.getDouble(LOOP_Y);
        double z = data.getDouble(LOOP_Z);
        // Snap every few ticks — "stuck repeating one moment".
        if (entity.tickCount % 8 == 0) {
            entity.teleportTo(x, y, z);
            entity.setYRot(data.getFloat(LOOP_YAW));
            entity.setXRot(data.getFloat(LOOP_PITCH));
            entity.setDeltaMovement(0, 0, 0);
            entity.hurtMarked = true;
        }
    }
}
