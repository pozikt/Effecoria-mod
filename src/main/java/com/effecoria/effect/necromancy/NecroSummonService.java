package com.effecoria.effect.necromancy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** Short-lived skeleton thralls bound to a necromancer's target. */
public final class NecroSummonService {
    public static final String OWNER_TAG = "effecoria:necro_owner";
    public static final String TARGET_TAG = "effecoria:necro_target";
    public static final String EXPIRE_TAG = "effecoria:necro_expire";

    private NecroSummonService() {}

    public static void register(Mob mob, ServerPlayer owner, LivingEntity target, long expireAtGameTime) {
        mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        mob.getPersistentData().putUUID(TARGET_TAG, target.getUUID());
        mob.getPersistentData().putLong(EXPIRE_TAG, expireAtGameTime);
    }

    public static void tick(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        AABB box = owner.getBoundingBox().inflate(64);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (!mob.getPersistentData().hasUUID(OWNER_TAG)) {
                continue;
            }
            if (!owner.getUUID().equals(mob.getPersistentData().getUUID(OWNER_TAG))) {
                continue;
            }
            long expire = mob.getPersistentData().getLong(EXPIRE_TAG);
            if (now >= expire) {
                mob.discard();
                continue;
            }
            UUID targetId = mob.getPersistentData().getUUID(TARGET_TAG);
            if (level.getEntity(targetId) instanceof LivingEntity living && living.isAlive()) {
                mob.setTarget(living);
            }
        }
    }
}
