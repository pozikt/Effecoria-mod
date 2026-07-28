package com.effecoria.core.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** Keeps player-summoned vex shades locked onto their cast target. */
public final class ShadeService {
    public static final String OWNER_TAG = "effecoria:shade_owner";
    public static final String TARGET_TAG = "effecoria:shade_target";

    private ShadeService() {}

    public static void registerShade(Vex shade, ServerPlayer owner, LivingEntity target) {
        shade.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        shade.getPersistentData().putUUID(TARGET_TAG, target.getUUID());
    }

    public static void tick(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        AABB box = owner.getBoundingBox().inflate(48);
        for (Vex shade : level.getEntitiesOfClass(Vex.class, box, Vex::isAlive)) {
            if (!shade.getPersistentData().hasUUID(OWNER_TAG)) {
                continue;
            }
            if (!owner.getUUID().equals(shade.getPersistentData().getUUID(OWNER_TAG))) {
                continue;
            }
            UUID targetId = shade.getPersistentData().getUUID(TARGET_TAG);
            Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                shade.setTarget(living);
                shade.setAggressive(true);
            } else {
                shade.discard();
            }
        }
    }
}
