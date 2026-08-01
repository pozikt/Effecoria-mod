package com.effecoria.effect.elemental;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
        long startedAt;
        long expireAt;
        UUID owner;
        float maintainDrainPerTick;
        float damagePerSecond;
        float tornadoKnock;
        float tornadoLiftMaxHealth;
        float moveSpeed;
        /** Desired heading; {@link #moveDir} steers toward this each tick for smooth travel. */
        Vec3 steerTarget = Vec3.ZERO;

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

        /** 0 at spawn → 1 at expiry. */
        float lifeProgress(long now) {
            long span = Math.max(1L, expireAt - startedAt);
            return Mth.clamp((now - startedAt) / (float) span, 0f, 1f);
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
            float damagePerSecond,
            Vec3 moveDir,
            float moveSpeed) {
        Vec3 dir = moveDir.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : moveDir.normalize();
        addField(
                level,
                Kind.QUASAR,
                center,
                dir,
                radius,
                durationTicks,
                owner,
                0f,
                damagePerSecond,
                0f,
                0f,
                Math.max(0.05f, moveSpeed));
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 0.5f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.45f, 0.55f);
        ElementalQuasarFx.playSpawn(level, center, radius, durationTicks);
        burstQuasarLayers(level, center, radius);
        QuasarTerrainService.tear(level, center, radius, level.random, 10);
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
        FIELDS.removeIf(f -> {
            if (!owner.equals(f.owner)) {
                return false;
            }
            if (f.kind == Kind.QUASAR) {
                QuasarTerrainService.release(f.level, f.center, f.radius);
            }
            return true;
        });
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        List<ActiveField> toRemove = new ArrayList<>();

        for (ActiveField field : FIELDS) {
            if (field.level != level) {
                continue;
            }
            if (now >= field.expireAt) {
                if (field.kind == Kind.QUASAR) {
                    QuasarTerrainService.release(level, field.center, field.radius);
                }
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
        field.startedAt = level.getGameTime();
        field.expireAt = field.startedAt + Math.max(1, durationTicks);
        field.owner = owner;
        field.maintainDrainPerTick = Math.max(0f, maintainDrainPerTick);
        field.damagePerSecond = Math.max(0f, damagePerSecond);
        field.tornadoKnock = tornadoKnock;
        field.tornadoLiftMaxHealth = tornadoLiftMaxHealth;
        field.moveSpeed = moveSpeed;
        field.steerTarget = moveDir.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : moveDir.normalize();
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
        updateQuasarMotion(level, field, now);
        float life = field.lifeProgress(now);
        // Visual / combat radius thins a bit as mass sheds.
        float massScale = life < QuasarTerrainService.SHED_START
                ? 1f
                : Mth.lerp((life - QuasarTerrainService.SHED_START) / (1f - QuasarTerrainService.SHED_START), 1f, 0.62f);
        float effectiveRadius = field.radius * massScale;

        if (now % 5 == 0) {
            ElementalQuasarFx.playPulse(level, field.center, effectiveRadius);
        }
        QuasarTerrainService.tickTerrain(level, field.center, field.radius, now, life, level.random);

        if (now % 20 == 0 && field.damagePerSecond > 0f) {
            DamageSource magic = level.damageSources().magic();
            DamageSource fire = level.damageSources().onFire();
            AABB box = new AABB(
                    field.center.x - effectiveRadius,
                    field.center.y - effectiveRadius * 0.5,
                    field.center.z - effectiveRadius,
                    field.center.x + effectiveRadius,
                    field.center.y + effectiveRadius * 1.2,
                    field.center.z + effectiveRadius);
            float r = effectiveRadius;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(field.owner);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity.getUUID().equals(field.owner)) {
                    continue;
                }
                Vec3 hit = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                double distSq = hit.distanceToSqr(field.center);
                if (distSq > (double) r * r) {
                    continue;
                }
                float n = (float) (Math.sqrt(distSq) / Math.max(0.001, r));
                float dps = field.damagePerSecond;
                if (n <= 0.2f) {
                    entity.hurt(magic, dps * 1.35f);
                    entity.hurt(fire, dps * 0.55f);
                    entity.igniteForSeconds(4);
                    BreathDebuffs.apply(
                            owner, entity, new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
                } else if (n <= 0.4f) {
                    entity.hurt(magic, dps * 0.7f);
                    entity.hurt(fire, dps * 0.2f);
                    entity.setTicksFrozen(Math.min(300, entity.getTicksFrozen() + 80));
                    BreathDebuffs.apply(
                            owner,
                            entity,
                            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
                } else if (n <= 0.65f) {
                    entity.hurt(magic, dps * 0.85f);
                    entity.hurt(fire, dps * 0.25f);
                    entity.igniteForSeconds(2);
                    BreathDebuffs.apply(
                            owner,
                            entity,
                            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1, false, false, true));
                    entity.setAirSupply(Math.max(-20, entity.getAirSupply() - 40));
                } else {
                    entity.hurt(fire, dps * 0.7f);
                    entity.hurt(magic, dps * 0.3f);
                    entity.igniteForSeconds(3);
                    Vec3 away = hit.subtract(field.center);
                    double len = away.length();
                    if (len > 1e-4) {
                        Vec3 push = away.scale(0.55 / len);
                        Vec3 tang = new Vec3(-away.z, 0, away.x).normalize().scale(0.35);
                        entity.push(push.x + tang.x, 0.12, push.z + tang.z);
                    }
                }
                entity.hurtMarked = true;
            }
        }
        if (now % 2 == 0) {
            emitQuasarLayers(level, field.center, effectiveRadius, now, level.random);
        }
        if (now % 35 == 0) {
            level.playSound(
                    null,
                    field.center.x,
                    field.center.y,
                    field.center.z,
                    SoundEvents.BREEZE_WHIRL,
                    SoundSource.PLAYERS,
                    0.35f,
                    0.55f + level.random.nextFloat() * 0.25f);
        }
        if (now % 40 == 0 && life >= QuasarTerrainService.SHED_START) {
            level.playSound(
                    null,
                    field.center.x,
                    field.center.y,
                    field.center.z,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.PLAYERS,
                    0.55f,
                    0.7f);
        }
    }

    private static void updateQuasarMotion(ServerLevel level, ActiveField field, long now) {
        // Retarget slowly — never snap the velocity vector.
        if (now % 45 == 0 || field.steerTarget.lengthSqr() < 1.0e-6) {
            LivingEntity prey = null;
            double best = 28.0 * 28.0;
            AABB hunt = new AABB(
                    field.center.x - 28,
                    field.center.y - 12,
                    field.center.z - 28,
                    field.center.x + 28,
                    field.center.y + 12,
                    field.center.z + 28);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, hunt, LivingEntity::isAlive)) {
                if (entity.getUUID().equals(field.owner) || entity instanceof ServerPlayer) {
                    continue;
                }
                double d = entity.distanceToSqr(field.center);
                if (d < best) {
                    best = d;
                    prey = entity;
                }
            }

            if (prey != null) {
                Vec3 to = prey.position().add(0, prey.getBbHeight() * 0.5, 0).subtract(field.center);
                if (to.lengthSqr() > 1.0e-4) {
                    field.steerTarget = to.normalize();
                }
            } else {
                // Gentle wander: blend current heading with a soft random bias.
                float yaw = level.random.nextFloat() * Mth.TWO_PI;
                float pitch = (level.random.nextFloat() - 0.5f) * 0.35f;
                Vec3 wander = new Vec3(
                                Mth.cos(yaw) * Mth.cos(pitch),
                                Mth.sin(pitch),
                                Mth.sin(yaw) * Mth.cos(pitch))
                        .normalize();
                Vec3 current = field.moveDir.lengthSqr() < 1.0e-6 ? wander : field.moveDir.normalize();
                field.steerTarget = current.lerp(wander, 0.55).normalize();
            }
        }

        BlockPos feet = BlockPos.containing(field.center.x, field.center.y - 1.2, field.center.z);
        if (!level.getBlockState(feet).isAir() && level.getBlockState(feet).isSolidRender(level, feet)) {
            field.steerTarget = new Vec3(field.steerTarget.x, Math.max(0.45, Math.abs(field.steerTarget.y)), field.steerTarget.z)
                    .normalize();
        }

        Vec3 desired = field.steerTarget.lengthSqr() < 1.0e-6 ? field.moveDir : field.steerTarget;
        if (desired.lengthSqr() < 1.0e-6) {
            desired = new Vec3(1, 0, 0);
        }
        // Low steer rate → smooth arcs instead of teleports.
        field.moveDir = field.moveDir.lerp(desired.normalize(), 0.06).normalize();
        double bob = Math.sin((now - field.startedAt) * 0.07) * 0.012;
        field.center = field.center.add(
                field.moveDir.x * field.moveSpeed,
                field.moveDir.y * field.moveSpeed + bob,
                field.moveDir.z * field.moveSpeed);
    }

    private static void burstQuasarLayers(ServerLevel level, Vec3 center, float radius) {
        level.sendParticles(
                ModParticleTypes.PHI_FLAME.get(), center.x, center.y, center.z, 28, 0.25, 0.35, 0.25, 0.04);
        level.sendParticles(
                ParticleTypes.END_ROD, center.x, center.y, center.z, 36, 0.35, 0.45, 0.35, 0.08);
        level.sendParticles(
                ModParticleTypes.ICE_CRYSTAL.get(),
                center.x,
                center.y,
                center.z,
                24,
                radius * 0.28,
                0.4,
                radius * 0.28,
                0.02);
        level.sendParticles(
                ModParticleTypes.WATER_WAVE.get(),
                center.x,
                center.y,
                center.z,
                32,
                radius * 0.45,
                0.35,
                radius * 0.45,
                0.03);
        level.sendParticles(
                ModParticleTypes.STEAM_FOG.get(),
                center.x,
                center.y + 0.5,
                center.z,
                48,
                radius * 0.7,
                0.8,
                radius * 0.7,
                0.02);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y + 0.5,
                center.z,
                20,
                radius * 0.75,
                0.6,
                radius * 0.75,
                0.12);
    }

    /** Orbiting steam / water / cryo / plasma shells — reads as a spinning eye. */
    private static void emitQuasarLayers(
            ServerLevel level, Vec3 center, float radius, long now, RandomSource random) {
        float spin = now * 0.18f;
        int spokes = 8;
        for (int i = 0; i < spokes; i++) {
            float a = spin + (Mth.TWO_PI * i) / spokes;
            float sa = Mth.sin(a);
            float ca = Mth.cos(a);

            // Compact violet plasma epicenter (~1 block)
            if (i == 0) {
                level.sendParticles(
                        ModParticleTypes.PHI_FLAME.get(), center.x, center.y, center.z, 2, 0.12, 0.12, 0.12, 0.01);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 2, 0.08, 0.1, 0.08, 0.012);
                level.sendParticles(ModParticleTypes.PHI_SPARK.get(), center.x, center.y, center.z, 1, 0.06, 0.06, 0.06, 0.02);
            }

            // Cryosphere shell
            double cx = center.x + ca * radius * 0.32;
            double cz = center.z + sa * radius * 0.32;
            level.sendParticles(ModParticleTypes.ICE_CRYSTAL.get(), cx, center.y - 0.1, cz, 1, 0.04, 0.05, 0.04, 0.005);
            if (i % 3 == 0) {
                level.sendParticles(ModParticleTypes.NECRO_SHADOW.get(), cx, center.y, cz, 1, 0.05, 0.05, 0.05, 0.0);
            }

            // Water ring
            double wx = center.x + ca * radius * 0.52;
            double wz = center.z + sa * radius * 0.52;
            level.sendParticles(ModParticleTypes.WATER_SPLASH.get(), wx, center.y, wz, 1, 0.05, 0.04, 0.05, 0.02);
            if ((now + i) % 4 == 0) {
                level.sendParticles(ModParticleTypes.WATER_WAVE.get(), wx, center.y + 0.1, wz, 1, 0.08, 0.03, 0.08, 0.01);
            }

            // Steam mantle + wind blades (tangential gusts)
            double sx = center.x + ca * radius * 0.82;
            double sz = center.z + sa * radius * 0.82;
            level.sendParticles(ModParticleTypes.STEAM_FOG.get(), sx, center.y + 0.4, sz, 2, 0.15, 0.2, 0.15, 0.01);
            if (i % 2 == 0) {
                level.sendParticles(ModParticleTypes.PHI_GUST.get(), sx, center.y + 0.6, sz, 1, 0.1, 0.1, 0.1, 0.04);
            }
            if ((now + i) % 6 == 0) {
                level.sendParticles(ParticleTypes.FLAME, sx, center.y + 0.5, sz, 1, 0.08, 0.08, 0.08, 0.01);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, sx, center.y + 0.7, sz, 1, 0.12, 0.12, 0.12, 0.05);
            }
        }

        // Short violet axis spark — stays near the epicenter, not a tall beam
        if (now % 4 == 0) {
            double yOff = (random.nextDouble() - 0.5) * 0.7;
            level.sendParticles(
                    ParticleTypes.END_ROD, center.x, center.y + yOff, center.z, 1, 0.04, 0.08, 0.04, 0.01);
            level.sendParticles(
                    ModParticleTypes.PHI_SPARK.get(), center.x, center.y + yOff * 0.4, center.z, 1, 0.03, 0.05, 0.03, 0.015);
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
