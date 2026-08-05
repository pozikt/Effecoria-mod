package com.effecoria.effect.organic;

import com.effecoria.core.formula.SpellCombat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** Timed parasite host — periodic drain to owner and rare same-type spread. */
public final class ParasiteHostService {
    public static final String UNTIL_TAG = "effecoria:organic_parasite_until";
    public static final String OWNER_TAG = "effecoria:organic_parasite_owner";
    public static final int DRAIN_INTERVAL = 20;
    private static final double SPREAD_RADIUS = 4.0;

    private ParasiteHostService() {}

    public static void apply(ServerPlayer owner, LivingEntity host, int durationTicks) {
        if (host == null || !host.isAlive()) {
            return;
        }
        long until = host.level().getGameTime() + Math.max(40, durationTicks);
        var data = host.getPersistentData();
        if (data.contains(UNTIL_TAG)) {
            until = Math.max(until, data.getLong(UNTIL_TAG));
        }
        data.putLong(UNTIL_TAG, until);
        data.putUUID(OWNER_TAG, owner.getUUID());
    }

    public static boolean hasActive(LivingEntity entity, long gameTime) {
        return entity.getPersistentData().getLong(UNTIL_TAG) > gameTime;
    }

    public static boolean isHostedBy(LivingEntity entity, UUID ownerId, long gameTime) {
        if (!hasActive(entity, gameTime) || ownerId == null) {
            return false;
        }
        var data = entity.getPersistentData();
        return data.hasUUID(OWNER_TAG) && data.getUUID(OWNER_TAG).equals(ownerId);
    }

    public static void extend(LivingEntity entity, int extraTicks) {
        if (extraTicks <= 0 || !hasActive(entity, entity.level().getGameTime())) {
            return;
        }
        var data = entity.getPersistentData();
        data.putLong(UNTIL_TAG, data.getLong(UNTIL_TAG) + extraTicks);
    }

    public static void clear(LivingEntity entity) {
        entity.getPersistentData().remove(UNTIL_TAG);
        entity.getPersistentData().remove(OWNER_TAG);
    }

    /** Spread from a seed host to one same-type peer nearby. */
    public static void spreadFrom(ServerLevel level, ServerPlayer owner, LivingEntity seed, int spreadDurationTicks) {
        if (seed == null || !seed.isAlive()) {
            return;
        }
        long now = level.getGameTime();
        EntityType<?> type = seed.getType();
        AABB box = seed.getBoundingBox().inflate(SPREAD_RADIUS);
        for (LivingEntity peer : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (peer == seed || peer == owner) {
                continue;
            }
            if (peer.getType() != type) {
                continue;
            }
            if (hasActive(peer, now)) {
                continue;
            }
            if (peer.distanceToSqr(seed) > SPREAD_RADIUS * SPREAD_RADIUS) {
                continue;
            }
            apply(owner, peer, spreadDurationTicks);
            OrganicEffects.spawnParasites(level, peer.position().add(0, 1, 0));
            return;
        }
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % DRAIN_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            AABB box = player.getBoundingBox().inflate(48);
            UUID ownerId = player.getUUID();
            for (LivingEntity host : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (!isHostedBy(host, ownerId, now)) {
                    continue;
                }
                float drain = Math.min(2.8f, 0.25f + host.getMaxHealth() / 24f);
                if (host.hurt(SpellCombat.magic(player), drain)) {
                    player.heal(drain * 0.55f);
                }
                spreadFrom(level, player, host, 80);
            }
        }
    }
}
