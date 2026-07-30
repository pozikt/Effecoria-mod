package com.effecoria.effect.necromancy;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.ShadeService;
import com.effecoria.core.magic.SpellEffectEntry;
import com.google.gson.JsonObject;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NecromancyEffects {
    private NecromancyEffects() {}

    public static void boneChill(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 80;
        target.hurt(level.damageSources().wither(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
        target.hurtMarked = true;
        finishHit(level, target);
    }

    public static void deathSense(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 12f;
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 60;
        float threshold = effect.params().has("health_fraction") ? effect.params().get("health_fraction").getAsFloat() : 0.5f;
        int count = 0;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.getHealth() / entity.getMaxHealth() > threshold) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
            count++;
        }
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.necro.death_sense", count), true);
        spawnNecroParticles(level, caster.position().add(0, 1, 0));
    }

    public static void graveWhisper(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        finishHit(caster.serverLevel(), target);
    }

    public static void siphonPulse(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        float healRatio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.35f;
        float damage = DiceDamage.fromParams(effect.params(), power, 4f);
        float healed = 0f;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
            entity.hurtMarked = true;
            healed += damage * healRatio;
            spawnNecroParticles(level, entity.position().add(0, 1, 0));
        }
        if (healed > 0f) {
            caster.heal(healed);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 0.75f);
    }

    public static void boneArmor(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 240;
        int resist = effect.params().has("resistance_amplifier") ? effect.params().get("resistance_amplifier").getAsInt() : 0;
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resist, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, false, true));
        spawnNecroParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void lifeTap(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        float healRatio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.75f;
        target.hurt(level.damageSources().magic(), damage);
        caster.heal(damage * healRatio);
        target.hurtMarked = true;
        finishHit(level, target);
    }

    public static void witherWave(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 80;
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().wither(), damage);
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
            entity.hurtMarked = true;
            spawnNecroParticles(level, entity.position().add(0, 1, 0));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 1.1f);
    }

    public static void darkPact(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        int exhaust = effect.params().has("exhaustion_ticks") ? effect.params().get("exhaustion_ticks").getAsInt() : 120;
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, exhaust, 1, false, false, true));
        spawnNecroParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void soulShackle(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int rootTicks = effect.params().has("root_ticks") ? effect.params().get("root_ticks").getAsInt() : 100;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 4));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, rootTicks, 0));
        finishHit(caster.serverLevel(), target);
    }

    public static void phantomStep(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, true));
        spawnNecroParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void graveField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 8f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 160;
        float dps = NecroFieldService.dpsFromParams(params, power);
        NecroFieldService.spawn(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                dps);
    }

    public static void raiseSkeleton(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int lifetime = effect.params().has("lifetime_ticks") ? effect.params().get("lifetime_ticks").getAsInt() : 300;
        lifetime = Math.round(lifetime * (0.85f + power / 150f));
        Vec3 look = caster.getLookAngle().normalize();
        double x = caster.getX() + look.x * 2;
        double z = caster.getZ() + look.z * 2;
        double y = caster.getY();
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (skeleton == null) {
            return;
        }
        skeleton.moveTo(x, y, z, caster.getYRot(), 0f);
        skeleton.setPersistenceRequired();
        level.addFreshEntity(skeleton);
        skeleton.setTarget(target);
        NecroSummonService.register(skeleton, caster, target, level.getGameTime() + lifetime);
        spawnNecroParticles(level, new Vec3(x, y + 1, z));
        level.playSound(null, caster.blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 0.7f, 0.8f);
    }

    public static void shadeBrood(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int count = effect.params().has("count") ? effect.params().get("count").getAsInt() : 2;
        int lifetime = effect.params().has("lifetime_ticks") ? effect.params().get("lifetime_ticks").getAsInt() : 200;
        lifetime = Math.round(lifetime * (0.9f + power / 150f));
        count = Math.min(4, Math.max(1, count));
        Vec3 look = caster.getLookAngle().normalize();
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double ox = Math.cos(angle) * 1.2;
            double oz = Math.sin(angle) * 1.2;
            double spawnX = caster.getX() + look.x * 1.2 + ox;
            double spawnZ = caster.getZ() + look.z * 1.2 + oz;
            double spawnY = caster.getY() + 1.0;
            Vex shade = EntityType.VEX.create(level);
            if (shade == null) {
                continue;
            }
            shade.moveTo(spawnX, spawnY, spawnZ, caster.getYRot(), 0f);
            shade.setLimitedLife(lifetime);
            shade.setPersistenceRequired();
            shade.setAggressive(true);
            level.addFreshEntity(shade);
            shade.setTarget(target);
            ShadeService.registerShade(shade, caster, target);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    public static void lichWard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 500;
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, false, true));
        spawnNecroParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void deathCoil(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float burst = DiceDamage.fromParams(effect.params(), power, 8f);
        float spread = effect.params().has("spread_radius") ? effect.params().get("spread_radius").getAsFloat() : 4f;
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 60;
        applyCoilHit(level, target, burst, witherTicks);
        AABB box = target.getBoundingBox().inflate(spread);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == target || entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(target) > spread * spread) {
                continue;
            }
            applyCoilHit(level, entity, burst * 0.5f, witherTicks / 2);
        }
    }

    public static void soulCataclysm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 14f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 220;
        float dps = NecroFieldService.dpsFromParams(params, power);
        NecroFieldService.spawn(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                dps * 1.35f);
    }

    public static void deathApotheosis(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 180;
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 2, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
        spawnNecroParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.35f, 1.4f);
    }

    private static void applyCoilHit(ServerLevel level, LivingEntity entity, float damage, int witherTicks) {
        entity.hurt(level.damageSources().wither(), damage);
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        entity.hurtMarked = true;
        spawnNecroParticles(level, entity.position().add(0, 1, 0));
    }

    private static void finishHit(ServerLevel level, LivingEntity target) {
        spawnNecroParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6f, 1.1f);
    }

    public static void spawnNecroParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_SHADOW.get(), pos.x, pos.y, pos.z, 10, 0.3, 0.35, 0.3, 0.01);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.25, 0.4, 0.25, 0.008);
    }
}
