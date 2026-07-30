package com.effecoria.effect.organic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Regeneration aura and biological cataclysm zones. */
public final class OrganicFieldService {
    private static final List<OrganicField> FIELDS = new CopyOnWriteArrayList<>();

    public enum Kind {
        HEAL,
        CATACLYSM,
        SINGULARITY
    }

    private static final class OrganicField {
        ServerLevel level;
        Kind kind;
        Vec3 center;
        float radius;
        long expireAt;
        UUID owner;
        float maintainDrainPerTick;
        float healPerSecond;
        float damagePerSecond;
    }

    private OrganicFieldService() {}

    public static void spawnHeal(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond,
            float healPerSecond) {
        OrganicField field = baseField(level, Kind.HEAL, center, owner, radius, durationTicks, maintainDrainPerSecond);
        field.healPerSecond = Math.max(0f, healPerSecond);
        FIELDS.add(field);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.1f);
    }

    public static void spawnCataclysm(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float damagePerSecond) {
        OrganicField field = baseField(level, Kind.CATACLYSM, center, owner, radius, durationTicks, 0f);
        field.damagePerSecond = Math.max(0.05f, damagePerSecond);
        FIELDS.add(field);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 0.7f);
    }

    public static void spawnSingularity(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float healPerSecond,
            float damagePerSecond) {
        OrganicField field = baseField(level, Kind.SINGULARITY, center, owner, radius, durationTicks, 0f);
        field.healPerSecond = Math.max(0f, healPerSecond);
        field.damagePerSecond = Math.max(0.05f, damagePerSecond);
        FIELDS.add(field);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 0.5f);
    }

    private static OrganicField baseField(
            ServerLevel level,
            Kind kind,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond) {
        OrganicField field = new OrganicField();
        field.level = level;
        field.kind = kind;
        field.center = center;
        field.radius = Math.max(1f, radius);
        field.expireAt = level.getGameTime() + Math.max(1, durationTicks);
        field.owner = owner;
        field.maintainDrainPerTick = Math.max(0f, maintainDrainPerSecond / 20f);
        return field;
    }

    public static void clearFor(UUID owner) {
        FIELDS.removeIf(f -> owner.equals(f.owner));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<OrganicField> toRemove = new ArrayList<>();
        for (OrganicField field : FIELDS) {
            if (field.level != level) {
                continue;
            }
            if (now >= field.expireAt) {
                toRemove.add(field);
                continue;
            }
            if (field.maintainDrainPerTick > 0f && !drainOwner(field)) {
                toRemove.add(field);
                continue;
            }
            if (field.kind == Kind.HEAL) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
                if (owner != null) {
                    field.center = owner.position().add(0, 0.5, 0);
                }
            }
            if (field.kind == Kind.SINGULARITY) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
                if (owner != null) {
                    field.center = owner.position().add(0, 0.5, 0);
                }
            }
            if (now % 20 == 0) {
                tickOncePerSecond(level, field, now);
            }
            if (now % 6 == 0) {
                spawnFieldParticles(level, field);
            }
        }
        FIELDS.removeAll(toRemove);
    }

    private static void tickOncePerSecond(ServerLevel level, OrganicField field, long now) {
        AABB box = new AABB(field.center, field.center).inflate(field.radius);
        if (field.kind == Kind.HEAL && field.healPerSecond > 0f) {
            for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
                if (ally.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                    continue;
                }
                ally.heal(field.healPerSecond);
                OrganicEffects.spawnOrganicParticles(level, ally.position().add(0, 1, 0));
            }
            return;
        }
        if (field.kind == Kind.SINGULARITY) {
            for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
                if (ally.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                    continue;
                }
                if (field.healPerSecond > 0f) {
                    ally.heal(field.healPerSecond);
                    OrganicEffects.spawnOrganicParticles(level, ally.position().add(0, 1, 0));
                }
            }
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity instanceof ServerPlayer) {
                    continue;
                }
                if (entity.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                    continue;
                }
                entity.hurt(level.damageSources().magic(), field.damagePerSecond);
                entity.hurtMarked = true;
            }
            return;
        }
        if (field.kind == Kind.CATACLYSM && field.damagePerSecond > 0f) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity instanceof ServerPlayer player && player.getUUID().equals(field.owner)) {
                    continue;
                }
                if (entity.position().distanceToSqr(field.center) > (double) field.radius * field.radius) {
                    continue;
                }
                entity.hurt(level.damageSources().wither(), field.damagePerSecond);
                entity.hurtMarked = true;
                if (now % 40 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
                }
            }
        }
    }

    private static void spawnFieldParticles(ServerLevel level, OrganicField field) {
        if (field.kind == Kind.CATACLYSM || field.kind == Kind.SINGULARITY) {
            level.sendParticles(
                    ModParticleTypes.ORGANIC_FOG.get(),
                    field.center.x,
                    field.center.y + 0.5,
                    field.center.z,
                    field.kind == Kind.SINGULARITY ? 14 : 10,
                    field.radius * 0.4,
                    0.5,
                    field.radius * 0.4,
                    0.02);
            if (field.kind == Kind.SINGULARITY) {
                level.sendParticles(
                        ModParticleTypes.ORGANIC_LEAF.get(),
                        field.center.x,
                        field.center.y + 0.8,
                        field.center.z,
                        8,
                        field.radius * 0.3,
                        0.4,
                        field.radius * 0.3,
                        0.01);
            }
            return;
        }
        level.sendParticles(
                ModParticleTypes.ORGANIC_FOG.get(),
                field.center.x,
                field.center.y + 0.5,
                field.center.z,
                6,
                field.radius * 0.35,
                0.4,
                field.radius * 0.35,
                0.01);
    }

    private static boolean drainOwner(OrganicField field) {
        ServerPlayer owner = field.level.getServer().getPlayerList().getPlayer(field.owner);
        if (owner == null) {
            return false;
        }
        if (CreativeGodMode.isActive(owner)) {
            return true;
        }
        PlayerPsiData data = PsiHelper.get(owner);
        if (data.currentPsi() < field.maintainDrainPerTick) {
            return false;
        }
        data.setCurrentPsi(data.currentPsi() - field.maintainDrainPerTick);
        PsiHelper.set(owner, data);
        owner.syncData(ModAttachments.PSI.get());
        return true;
    }

    /** Build DPS for cataclysm from dice-per-round notation. */
    public static float cataclysmDpsFromParams(com.google.gson.JsonObject params, float power) {
        return DiceDamage.perSecondFromParams(params, power, 2f);
    }
}
