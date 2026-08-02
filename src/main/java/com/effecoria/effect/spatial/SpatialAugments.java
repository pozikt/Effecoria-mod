package com.effecoria.effect.spatial;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/** Timed spatial buffs stored on living entities (lens, cocoon, wall-walk, time-loop). */
public final class SpatialAugments {
    public static final String LENS_UNTIL = "effecoria:spatial_lens_until";
    public static final String COCOON_UNTIL = "effecoria:spatial_cocoon_until";
    public static final String WALL_UNTIL = "effecoria:spatial_wall_until";
    public static final String LOOP_UNTIL = "effecoria:spatial_loop_until";
    public static final String LOOP_X = "effecoria:spatial_loop_x";
    public static final String LOOP_Y = "effecoria:spatial_loop_y";
    public static final String LOOP_Z = "effecoria:spatial_loop_z";
    public static final String LOOP_AIM_X = "effecoria:spatial_loop_aim_x";
    public static final String LOOP_AIM_Y = "effecoria:spatial_loop_aim_y";
    public static final String LOOP_AIM_Z = "effecoria:spatial_loop_aim_z";
    public static final String LOOP_ECHO = "effecoria:spatial_loop_echo";
    public static final String ECHO_TAG = "effecoria_chrono_echo";

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

    /**
     * Chronal anomaly: freeze body pose and lock actions toward a frozen aim point
     * (e.g. where the caster stood when the loop began).
     */
    public static void beginTimeLoop(LivingEntity entity, long untilGameTime, Vec3 aimPoint) {
        clearTimeLoop(entity);
        CompoundTag data = entity.getPersistentData();
        data.putLong(LOOP_UNTIL, untilGameTime);
        data.putDouble(LOOP_X, entity.getX());
        data.putDouble(LOOP_Y, entity.getY());
        data.putDouble(LOOP_Z, entity.getZ());
        data.putDouble(LOOP_AIM_X, aimPoint.x);
        data.putDouble(LOOP_AIM_Y, aimPoint.y);
        data.putDouble(LOOP_AIM_Z, aimPoint.z);

        if (entity.level() instanceof ServerLevel level) {
            ArmorStand echo = new ArmorStand(level, aimPoint.x, aimPoint.y, aimPoint.z);
            echo.setInvisible(true);
            echo.setNoGravity(true);
            echo.setInvulnerable(true);
            echo.setSilent(true);
            echo.setCustomNameVisible(false);
            echo.noPhysics = true;
            // Marker via save data — setMarker() is private on 1.21 ArmorStand.
            CompoundTag standTag = new CompoundTag();
            echo.addAdditionalSaveData(standTag);
            standTag.putBoolean("Marker", true);
            echo.readAdditionalSaveData(standTag);
            echo.addTag(ECHO_TAG);
            level.addFreshEntity(echo);
            data.putUUID(LOOP_ECHO, echo.getUUID());
            if (entity instanceof Mob mob) {
                mob.setTarget(echo);
            }
        }

        lookAtAim(entity, aimPoint);
    }

    public static boolean hasTimeLoop(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(LOOP_UNTIL) > gameTime;
    }

    public static Vec3 getLoopAim(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(LOOP_AIM_X)) {
            return null;
        }
        return new Vec3(data.getDouble(LOOP_AIM_X), data.getDouble(LOOP_AIM_Y), data.getDouble(LOOP_AIM_Z));
    }

    public static void tickTimeLoop(LivingEntity entity, long gameTime) {
        CompoundTag data = entity.getPersistentData();
        long until = data.getLong(LOOP_UNTIL);
        if (until <= 0L) {
            return;
        }
        if (gameTime >= until) {
            clearTimeLoop(entity);
            return;
        }

        double x = data.getDouble(LOOP_X);
        double y = data.getDouble(LOOP_Y);
        double z = data.getDouble(LOOP_Z);
        Vec3 aim = getLoopAim(entity);
        if (aim == null) {
            aim = new Vec3(x, y + entity.getEyeHeight(), z);
        }

        // Soft body lock — keep repeating the same stance.
        if (entity.tickCount % 10 == 0) {
            entity.teleportTo(x, y, z);
            entity.setDeltaMovement(0, 0, 0);
            entity.hurtMarked = true;
        }

        lookAtAim(entity, aim);

        if (entity instanceof Mob mob && entity.level() instanceof ServerLevel level) {
            LivingEntity echo = resolveEcho(level, data);
            if (echo != null && echo.isAlive()) {
                if (mob.getTarget() != echo) {
                    mob.setTarget(echo);
                }
            }
        }
    }

    public static void clearTimeLoop(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.hasUUID(LOOP_ECHO) && entity.level() instanceof ServerLevel level) {
            Entity echo = level.getEntity(data.getUUID(LOOP_ECHO));
            if (echo != null) {
                echo.discard();
            }
        }
        data.remove(LOOP_UNTIL);
        data.remove(LOOP_X);
        data.remove(LOOP_Y);
        data.remove(LOOP_Z);
        data.remove(LOOP_AIM_X);
        data.remove(LOOP_AIM_Y);
        data.remove(LOOP_AIM_Z);
        data.remove(LOOP_ECHO);
        if (entity instanceof Mob mob
                && mob.getTarget() != null
                && mob.getTarget().getTags().contains(ECHO_TAG)) {
            mob.setTarget(null);
        }
    }

    private static LivingEntity resolveEcho(ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(LOOP_ECHO)) {
            return null;
        }
        Entity entity = level.getEntity(data.getUUID(LOOP_ECHO));
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void lookAtAim(LivingEntity entity, Vec3 aim) {
        Vec3 eye = entity.getEyePosition();
        Vec3 delta = aim.subtract(eye);
        double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        float pitch = (float) (Math.toDegrees(-Math.atan2(delta.y, horiz)));
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.yRotO = yaw;
        entity.xRotO = pitch;
    }
}
