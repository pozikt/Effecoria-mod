package com.effecoria.effect.necromancy;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.SpellCombat;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Withering grave zones — external Ψ relay fields. */
public final class NecroFieldService {
    private static final List<GraveField> FIELDS = new CopyOnWriteArrayList<>();

    private static final class GraveField {
        ServerLevel level;
        Vec3 center;
        float radius;
        long expireAt;
        UUID owner;
        float damagePerSecond;
    }

    private NecroFieldService() {}

    public static void spawn(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float damagePerSecond) {
        GraveField field = new GraveField();
        field.level = level;
        field.center = center;
        field.radius = Math.max(1f, radius);
        field.expireAt = level.getGameTime() + Math.max(1, durationTicks);
        field.owner = owner;
        field.damagePerSecond = Math.max(0.05f, damagePerSecond);
        FIELDS.add(field);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.4f, 0.8f);
    }

    public static void clearFor(UUID owner) {
        FIELDS.removeIf(f -> owner.equals(f.owner));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<GraveField> toRemove = new ArrayList<>();
        for (GraveField field : FIELDS) {
            if (field.level != level) {
                continue;
            }
            if (now >= field.expireAt) {
                toRemove.add(field);
                continue;
            }
            if (now % 20 == 0 && field.damagePerSecond > 0f) {
                AABB box = new AABB(field.center, field.center).inflate(field.radius);
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                    if (entity instanceof ServerPlayer player && player.getUUID().equals(field.owner)) {
                        continue;
                    }
                    if (entity.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                        continue;
                    }
                    ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
                    if (owner != null) {
                        SpellCombat.hurtWither(owner, entity, field.damagePerSecond);
                    } else {
                        entity.hurt(level.damageSources().wither(), field.damagePerSecond);
                    }
                    entity.hurtMarked = true;
                    if (now % 40 == 0) {
                        BreathDebuffs.apply(level, field.owner, entity, new MobEffectInstance(MobEffects.WITHER, 40, 0));
                    }
                }
            }
            if (now % 8 == 0) {
                level.sendParticles(
                        ModParticleTypes.NECRO_FOG.get(),
                        field.center.x,
                        field.center.y + 0.4,
                        field.center.z,
                        8,
                        field.radius * 0.35,
                        0.35,
                        field.radius * 0.35,
                        0.01);
            }
        }
        FIELDS.removeAll(toRemove);
    }

    public static float dpsFromParams(com.google.gson.JsonObject params, float power) {
        return DiceDamage.perSecondFromParams(params, power, 2f);
    }
}
