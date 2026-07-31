package com.effecoria.effect.elemental;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Mirage aura, ion storm zones, and moving tornado columns. */
public final class ElementalFieldService {
    private static final List<ActiveField> FIELDS = new CopyOnWriteArrayList<>();

    public enum Kind {
        MIRAGE,
        ION_STORM,
        TORNADO,
        HURRICANE,
        QUASAR,
        SUPREMACY
    }

    private static final class ActiveField {
        ServerLevel level;
        Kind kind;
        Vec3 center;
        Vec3 moveDir;
        float radius;
        long expireAt;
        UUID owner;
        float maintainDrainPerTick;
        float damagePerSecond;
        float tornadoKnock;
        float tornadoLiftMaxHealth;
        float moveSpeed;

        boolean contains(Vec3 pos) {
            return pos.distanceToSqr(center) <= (double) radius * radius;
        }

        AABB bounds() {
            return new AABB(
                    center.x - radius,
                    center.y - radius * 0.5,
                    center.z - radius,
                    center.x + radius,
                    center.y + radius * 1.2,
                    center.z + radius);
        }
    }

    private ElementalFieldService() {}

    public static void spawnMirage(
            ServerLevel level, ServerPlayer owner, float radius, int durationTicks, float maintainDrainPerSecond) {
        addField(
                level,
                Kind.MIRAGE,
                owner.position().add(0, 1, 0),
                Vec3.ZERO,
                radius,
                durationTicks,
                owner.getUUID(),
                maintainDrainPerSecond / 20f,
                0f,
                0f,
                0f,
                0f);
        level.playSound(null, owner.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    public static void spawnIonStorm(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond,
            float damagePerSecond) {
        addField(
                level,
                Kind.ION_STORM,
                center,
                Vec3.ZERO,
                radius,
                durationTicks,
                owner,
                maintainDrainPerSecond / 20f,
                damagePerSecond,
                0f,
                0f,
                0f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.35f, 1.8f);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y + 0.5,
                center.z,
                24,
                radius * 0.4,
                0.5,
                radius * 0.4,
                0.15);
    }

    public static void spawnTornado(
            ServerLevel level,
            Vec3 start,
            Vec3 direction,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond,
            float damagePerSecond,
            float knock,
            float liftMaxHealth,
            float moveSpeed) {
        Vec3 dir = direction.lengthSqr() < 1.0e-6 ? new Vec3(0, 0, 1) : direction.normalize();
        addField(
                level,
                Kind.TORNADO,
                start,
                dir,
                radius,
                durationTicks,
                owner,
                maintainDrainPerSecond / 20f,
                damagePerSecond,
                knock,
                liftMaxHealth,
                moveSpeed);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.9f, 0.6f);
    }

    public static void spawnHurricane(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond,
            float damagePerSecond,
            float knock) {
        addField(
                level,
                Kind.HURRICANE,
                center,
                Vec3.ZERO,
                radius,
                durationTicks,
                owner,
                maintainDrainPerSecond / 20f,
                damagePerSecond,
                knock,
                0f,
                0f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.1f, 0.4f);
    }

    public static void spawnQuasar(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float damagePerSecond) {
        addField(level, Kind.QUASAR, center, Vec3.ZERO, radius, durationTicks, owner, 0f, damagePerSecond, 0f, 0f, 0f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.5f);
        level.sendParticles(
                ParticleTypes.END_ROD,
                center.x,
                center.y + 1,
                center.z,
                40,
                radius * 0.2,
                1.0,
                radius * 0.2,
                0.05);
    }

    public static void spawnSupremacy(
            ServerLevel level,
            Vec3 center,
            UUID owner,
            float radius,
            int durationTicks,
            float maintainDrainPerSecond,
            float damagePerSecond) {
        addField(
                level,
                Kind.SUPREMACY,
                center,
                Vec3.ZERO,
                radius,
                durationTicks,
                owner,
                maintainDrainPerSecond / 20f,
                damagePerSecond,
                0f,
                0f,
                0f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    public static void clearFor(UUID owner) {
        FIELDS.removeIf(f -> owner.equals(f.owner));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<ActiveField> toRemove = new ArrayList<>();

        for (ActiveField field : FIELDS) {
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

            switch (field.kind) {
                case MIRAGE -> tickMirage(level, field, now);
                case ION_STORM -> tickIonStorm(level, field, now);
                case TORNADO -> tickTornado(level, field, now);
                case HURRICANE -> tickHurricane(level, field, now);
                case QUASAR -> tickQuasar(level, field, now);
                case SUPREMACY -> tickSupremacy(level, field, now);
            }
        }

        FIELDS.removeAll(toRemove);
    }

    private static void addField(
            ServerLevel level,
            Kind kind,
            Vec3 center,
            Vec3 moveDir,
            float radius,
            int durationTicks,
            UUID owner,
            float maintainDrainPerTick,
            float damagePerSecond,
            float tornadoKnock,
            float tornadoLiftMaxHealth,
            float moveSpeed) {
        ActiveField field = new ActiveField();
        field.level = level;
        field.kind = kind;
        field.center = center;
        field.moveDir = moveDir;
        field.radius = Math.max(0.75f, radius);
        field.expireAt = level.getGameTime() + Math.max(1, durationTicks);
        field.owner = owner;
        field.maintainDrainPerTick = Math.max(0f, maintainDrainPerTick);
        field.damagePerSecond = Math.max(0f, damagePerSecond);
        field.tornadoKnock = tornadoKnock;
        field.tornadoLiftMaxHealth = tornadoLiftMaxHealth;
        field.moveSpeed = moveSpeed;
        FIELDS.add(field);
    }

    private static boolean drainOwner(ActiveField field) {
        ServerPlayer owner = field.level.getServer().getPlayerList().getPlayer(field.owner);
        if (owner == null) {
            return field.kind != Kind.MIRAGE;
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

    private static void tickMirage(ServerLevel level, ActiveField field, long now) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
        if (owner == null || !owner.isAlive()) {
            return;
        }
        field.center = owner.position().add(0, 1, 0);

        BreathDebuffs.apply(owner, new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, false, false, true));

        AABB box = field.bounds();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.getUUID().equals(field.owner)) {
                continue;
            }
            if (!field.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                continue;
            }
            BreathDebuffs.apply(owner, entity, new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
            BreathDebuffs.apply(owner, entity, new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, true));
        }

        if (now % 4 == 0) {
            level.sendParticles(
                    ModParticleTypes.PHI_GUST.get(),
                    field.center.x,
                    field.center.y,
                    field.center.z,
                    6,
                    field.radius * 0.35,
                    0.3,
                    field.radius * 0.35,
                    0.02);
        }
    }

    private static void tickIonStorm(ServerLevel level, ActiveField field, long now) {
        if (now % 20 == 0 && field.damagePerSecond > 0f) {
            DamageSource source = level.damageSources().magic();
            float hit = field.damagePerSecond;
            AABB box = field.bounds();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity.getUUID().equals(field.owner)) {
                    continue;
                }
                if (!field.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                    continue;
                }
                entity.hurt(source, hit);
                entity.hurtMarked = true;
            }
        }

        if (now % 3 == 0) {
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    field.center.x,
                    field.center.y + 0.6,
                    field.center.z,
                    Math.max(8, Math.round(field.radius * 4)),
                    field.radius * 0.45,
                    0.6,
                    field.radius * 0.45,
                    0.08);
        }
    }

    private static void tickTornado(ServerLevel level, ActiveField field, long now) {
        field.center = field.center.add(field.moveDir.scale(field.moveSpeed));

        AABB box = field.bounds();
        DamageSource source = level.damageSources().magic();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.getUUID().equals(field.owner)) {
                continue;
            }
            if (!field.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                continue;
            }
            Vec3 toCenter = field.center.subtract(entity.position());
            Vec3 pull = new Vec3(toCenter.x, 0, toCenter.z);
            if (pull.lengthSqr() > 1.0e-4) {
                pull = pull.normalize().scale(0.12);
                entity.push(pull.x, 0.02, pull.z);
            }
            if (entity.getMaxHealth() <= field.tornadoLiftMaxHealth) {
                entity.push(0, 0.18, 0);
            }
            if (now % 10 == 0 && field.damagePerSecond > 0f) {
                entity.hurt(source, field.damagePerSecond * 0.5f);
                entity.hurtMarked = true;
            }
            if (field.tornadoKnock > 0f) {
                Vec3 away = entity.position().subtract(field.center);
                if (away.lengthSqr() > 1.0e-4) {
                    away = away.normalize();
                    entity.push(away.x * field.tornadoKnock * 0.05, 0.05, away.z * field.tornadoKnock * 0.05);
                }
            }
        }

        if (now % 2 == 0) {
            level.sendParticles(
                    ModParticleTypes.PHI_GUST.get(),
                    field.center.x,
                    field.center.y + 0.5,
                    field.center.z,
                    10,
                    field.radius * 0.25,
                    field.radius * 0.5,
                    field.radius * 0.25,
                    0.06);
        }
    }

    private static void tickHurricane(ServerLevel level, ActiveField field, long now) {
        AABB box = field.bounds();
        DamageSource source = level.damageSources().magic();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.getUUID().equals(field.owner)) {
                continue;
            }
            if (!field.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                continue;
            }
            Vec3 away = entity.position().subtract(field.center);
            if (away.lengthSqr() > 1.0e-4) {
                away = away.normalize();
                float knock = Math.max(0.2f, field.tornadoKnock);
                entity.push(away.x * knock * 0.12, 0.12, away.z * knock * 0.12);
            }
            if (now % 20 == 0 && field.damagePerSecond > 0f) {
                entity.hurt(source, field.damagePerSecond);
                entity.hurtMarked = true;
            }
        }
        if (now % 2 == 0) {
            level.sendParticles(
                    ModParticleTypes.PHI_GUST.get(),
                    field.center.x,
                    field.center.y + 1,
                    field.center.z,
                    Math.max(12, Math.round(field.radius)),
                    field.radius * 0.45,
                    0.8,
                    field.radius * 0.45,
                    0.08);
        }
    }

    private static void tickQuasar(ServerLevel level, ActiveField field, long now) {
        if (now % 20 == 0 && field.damagePerSecond > 0f) {
            DamageSource source = level.damageSources().magic();
            AABB box = field.bounds();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity.getUUID().equals(field.owner)) {
                    continue;
                }
                if (!field.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                    continue;
                }
                entity.hurt(source, field.damagePerSecond);
                entity.hurt(level.damageSources().onFire(), field.damagePerSecond * 0.25f);
                entity.hurtMarked = true;
            }
        }
        if (now % 2 == 0) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    field.center.x,
                    field.center.y + 1.2,
                    field.center.z,
                    10,
                    field.radius * 0.35,
                    0.7,
                    field.radius * 0.35,
                    0.04);
            level.sendParticles(
                    ModParticleTypes.PHI_FLAME.get(),
                    field.center.x,
                    field.center.y + 0.8,
                    field.center.z,
                    4,
                    0.2,
                    0.4,
                    0.2,
                    0.02);
        }
    }

    private static void tickSupremacy(ServerLevel level, ActiveField field, long now) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
        if (owner != null) {
            field.center = owner.position().add(0, 0.5, 0);
        }
        AABB box = field.bounds();
        DamageSource source = level.damageSources().magic();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.getUUID().equals(field.owner)) {
                continue;
            }
            if (!field.contains(entity.position().add(0, entity.getBbHeight() * 0.5, 0))) {
                continue;
            }
            BreathDebuffs.apply(owner, entity, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
            if (now % 20 == 0 && field.damagePerSecond > 0f) {
                // Mixed elemental pressure: alternate cold / fire / magic slices.
                float hit = field.damagePerSecond;
                long phase = (now / 20) % 3;
                if (phase == 0) {
                    entity.hurt(source, hit);
                    entity.setTicksFrozen(Math.min(200, entity.getTicksFrozen() + 40));
                } else if (phase == 1) {
                    entity.hurt(level.damageSources().onFire(), hit);
                    entity.igniteForSeconds(2);
                } else {
                    entity.hurt(source, hit * 0.85f);
                    entity.push(0, 0.25, 0);
                }
                entity.hurtMarked = true;
            }
        }
        if (now % 5 == 0) {
            level.sendParticles(
                    ModParticleTypes.STEAM_FOG.get(),
                    field.center.x,
                    field.center.y + 1,
                    field.center.z,
                    8,
                    field.radius * 0.3,
                    0.5,
                    field.radius * 0.3,
                    0.02);
            level.sendParticles(
                    ModParticleTypes.ICE_CRYSTAL.get(),
                    field.center.x,
                    field.center.y + 0.5,
                    field.center.z,
                    4,
                    field.radius * 0.25,
                    0.3,
                    field.radius * 0.25,
                    0.02);
        }
    }
}
