package com.effecoria.effect.corruption;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CorruptionEffects {
    private CorruptionEffects() {}

    public static void corruptMark(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage;
        if (effect.params().has("damage_dice")) {
            damage = DiceDamage.fromParams(effect.params(), power, 3f);
        } else {
            damage = effect.params().get("damage").getAsFloat() * (power / 50f);
        }
        int poisonTicks = effect.params().get("poison_ticks").getAsInt();
        int weaknessTicks = effect.params().get("weakness_ticks").getAsInt();
        poisonTicks = Math.round(poisonTicks * (0.85f + power / 100f));
        weaknessTicks = Math.round(weaknessTicks * (0.85f + power / 100f));

        target.hurt(level.damageSources().magic(), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, weaknessTicks, 0));
        spawnCorruptionParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    public static void bindingSeal(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int rootTicks = effect.params().get("root_ticks").getAsInt();
        int glowTicks = effect.params().has("glow_ticks") ? effect.params().get("glow_ticks").getAsInt() : rootTicks;
        rootTicks = Math.round(rootTicks * (0.8f + power / 100f));
        glowTicks = Math.round(glowTicks * (0.8f + power / 100f));

        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 5));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, rootTicks, 2));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, glowTicks, 0));
        spawnCorruptionParticles(level, target.position().add(0, 0.5, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    public static void blightPulse(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 5;
        float damage = effect.params().has("damage_dice")
                ? DiceDamage.fromParams(effect.params(), power, 3f)
                : (effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 3f) * (power / 50f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 60;
        radius *= 0.9 + power / 150f;
        poisonTicks = Math.round(poisonTicks * (0.85f + power / 100f));

        pulseBlight(level, caster, radius, damage, poisonTicks, true);
        level.playSound(null, caster.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    public static void rotTouch(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 2.5f);
        int hungerTicks = effect.params().has("hunger_ticks") ? effect.params().get("hunger_ticks").getAsInt() : 100;
        target.hurt(level.damageSources().wither(), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.HUNGER, hungerTicks, 1));
        finishHit(level, target);
    }

    public static void entropyLash(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 4f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 60;
        target.hurt(level.damageSources().magic(), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, poisonTicks, 1));
        finishHit(level, target);
    }

    public static void plagueBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 80;
        int weaknessTicks = effect.params().has("weakness_ticks") ? effect.params().get("weakness_ticks").getAsInt() : 60;
        target.hurt(level.damageSources().magic(), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, weaknessTicks, 0));
        finishHit(level, target);
    }

    public static void festeringWound(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 80;
        int amp = effect.params().has("wither_amplifier") ? effect.params().get("wither_amplifier").getAsInt() : 1;
        target.hurt(level.damageSources().wither(), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, witherTicks, amp));
        finishHit(level, target);
    }

    public static void miasmaCloak(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float dps = effect.params().has("damage_per_second") ? effect.params().get("damage_per_second").getAsFloat() : 1.5f;
        radius *= 0.9f + power / 120f;
        dps *= 0.85f + power / 100f;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        CorruptionFieldService.spawnMiasma(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                dps,
                0);
        caster.serverLevel()
                .playSound(null, caster.blockPosition(), SoundEvents.WARDEN_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.2f);
    }

    public static void blightSurge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 7;
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 100;
        radius *= 0.95 + power / 130f;
        pulseBlight(caster.serverLevel(), caster, radius, damage, poisonTicks, true);
    }

    public static void decayBind(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        bindingSeal(caster, effect, power, target);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 80;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, poisonTicks, 1));
    }

    public static void blightField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5.5f;
        float dps = effect.params().has("damage_per_second") ? effect.params().get("damage_per_second").getAsFloat() : 2f;
        int amp = effect.params().has("poison_amplifier") ? effect.params().get("poison_amplifier").getAsInt() : 1;
        Vec3 at = caster.position().add(caster.getLookAngle().scale(2));
        CorruptionFieldService.spawnMiasma(
                caster.serverLevel(), at, caster.getUUID(), radius * (0.9f + power / 120f), duration, dps * (0.85f + power / 100f), amp);
        spawnCorruptionPulse(caster.serverLevel(), at, radius);
    }

    public static void entropyAegis(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 240;
        int resist = effect.params().has("resistance_amplifier") ? effect.params().get("resistance_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resist, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.HUNGER, duration / 2, 0, false, false, false));
        spawnCorruptionParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void taintedLeech(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        float ratio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.4f;
        target.hurt(level.damageSources().magic(), damage);
        target.hurtMarked = true;
        caster.heal(damage * ratio);
        finishHit(level, target);
    }

    public static void virulentWave(ServerPlayer caster, SpellEffectEntry effect, float power) {
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 8;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 120;
        int amp = effect.params().has("poison_amplifier") ? effect.params().get("poison_amplifier").getAsInt() : 2;
        ServerLevel level = caster.serverLevel();
        radius *= 0.9 + power / 140f;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, amp));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.CONFUSION, poisonTicks / 2, 0));
            spawnCorruptionParticles(level, entity.position().add(0, 1, 0));
        }
        spawnCorruptionPulse(level, caster.position().add(0, 0.2, 0), radius);
    }

    public static void plagueCrown(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 120;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, true, true));
        blightSurge(caster, effect, power);
    }

    public static void omegaBlight(ServerPlayer caster, SpellEffectEntry effect, float power) {
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 10;
        float damage = DiceDamage.fromParams(effect.params(), power, 8f);
        int fieldTicks = effect.params().has("field_ticks") ? effect.params().get("field_ticks").getAsInt() : 160;
        float dps = effect.params().has("field_dps") ? effect.params().get("field_dps").getAsFloat() : 3f;
        pulseBlight(caster.serverLevel(), caster, radius * (0.95 + power / 100f), damage, 140, true);
        CorruptionFieldService.spawnMiasma(
                caster.serverLevel(),
                caster.position(),
                caster.getUUID(),
                (float) radius,
                fieldTicks,
                dps,
                2);
        caster.serverLevel()
                .playSound(null, caster.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 0.8f);
    }

    private static void pulseBlight(
            ServerLevel level,
            ServerPlayer caster,
            double radius,
            float damage,
            int poisonTicks,
            boolean weakness) {
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator())) {
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
            if (weakness) {
                BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WEAKNESS, poisonTicks / 2, 0));
            }
            spawnCorruptionParticles(level, entity.position().add(0, 1, 0));
        }
        spawnCorruptionPulse(level, caster.position().add(0, 0.2, 0), radius);
    }

    private static void finishHit(ServerLevel level, LivingEntity target) {
        spawnCorruptionParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.8f, 0.85f);
    }

    public static void spawnCorruptionParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), pos.x, pos.y, pos.z, 8, 0.2, 0.3, 0.2, 0.02);
        level.sendParticles(ModParticleTypes.CORRUPTION_BLOOD.get(), pos.x, pos.y + 0.2, pos.z, 6, 0.15, 0.2, 0.15, 0.04);
        level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), pos.x, pos.y + 0.5, pos.z, 4, 0.1, 0.15, 0.1, 0.01);
    }

    public static void spawnCorruptionPulse(ServerLevel level, Vec3 center, double radius) {
        int steps = Math.max(8, (int) (radius * 4));
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2 * i) / steps;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), x, center.y + 0.3, z, 2, 0.05, 0.12, 0.05, 0.02);
            level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), x, center.y + 0.5, z, 1, 0.02, 0.05, 0.02, 0.0);
        }
        level.sendParticles(
                ModParticleTypes.CORRUPTION_BLOOD.get(), center.x, center.y + 0.4, center.z, 6, 0.3, 0.2, 0.3, 0.05);
    }
}
