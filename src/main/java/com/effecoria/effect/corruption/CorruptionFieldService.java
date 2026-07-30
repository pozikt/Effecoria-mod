package com.effecoria.effect.corruption;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Lingering miasma and omega blight zones. */
public final class CorruptionFieldService {
    private static final List<BlightField> FIELDS = new CopyOnWriteArrayList<>();

    private static final class BlightField {
        ServerLevel level;
        Vec3 center;
        float radius;
        long expireAt;
        UUID owner;
        float damagePerSecond;
        int poisonAmplifier;
    }

    private CorruptionFieldService() {}

    public static void spawnMiasma(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float damagePerSecond,
            int poisonAmplifier) {
        BlightField field = new BlightField();
        field.level = level;
        field.center = center;
        field.owner = owner;
        field.radius = Math.max(2f, radius);
        field.expireAt = level.getGameTime() + durationTicks;
        field.damagePerSecond = Math.max(0.1f, damagePerSecond);
        field.poisonAmplifier = Math.max(0, poisonAmplifier);
        FIELDS.add(field);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<BlightField> toRemove = new ArrayList<>();
        for (BlightField field : FIELDS) {
            if (field.level != level) {
                continue;
            }
            if (now >= field.expireAt) {
                toRemove.add(field);
                continue;
            }
            if (now % 20 == 0) {
                tickDamage(field);
            }
            if (now % 8 == 0) {
                CorruptionEffects.spawnCorruptionPulse(field.level, field.center, field.radius * 0.85);
            }
        }
        FIELDS.removeAll(toRemove);
    }

    public static void clearFor(UUID playerId) {
        FIELDS.removeIf(f -> f.owner.equals(playerId));
    }

    private static void tickDamage(BlightField field) {
        ServerLevel level = field.level;
        AABB box = new AABB(field.center, field.center).inflate(field.radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity instanceof ServerPlayer player && player.getUUID().equals(field.owner)) {
                continue;
            }
            if (entity.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), field.damagePerSecond);
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, field.poisonAmplifier));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            entity.hurtMarked = true;
        }
    }
}
