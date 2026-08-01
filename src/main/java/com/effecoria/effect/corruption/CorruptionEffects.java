package com.effecoria.effect.corruption;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.google.gson.JsonObject;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        int poisonTicks = scaleTicks(effect.params().get("poison_ticks").getAsInt(), power);
        int weaknessTicks = scaleTicks(effect.params().get("weakness_ticks").getAsInt(), power);
        boolean permanent = isPermanent(effect.params());

        target.hurt(SpellCombat.magic(caster), damage);
        CorruptionCurse curse = CorruptionCurse.builder("corrupt_mark", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(0)
                .effect(MobEffects.POISON, 0, poisonTicks, false)
                .effect(MobEffects.WEAKNESS, 0, weaknessTicks, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        spawnMark(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    public static void bindingSeal(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int rootTicks = scaleTicks(effect.params().get("root_ticks").getAsInt(), power, 0.8f);
        int glowTicks = scaleTicks(
                effect.params().has("glow_ticks") ? effect.params().get("glow_ticks").getAsInt() : rootTicks,
                power,
                0.8f);
        boolean permanent = isPermanent(effect.params());

        CorruptionCurse curse = CorruptionCurse.builder("binding_seal", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(0)
                .effect(MobEffects.MOVEMENT_SLOWDOWN, 5, rootTicks, permanent)
                .effect(MobEffects.DIG_SLOWDOWN, 2, rootTicks, permanent)
                .effect(MobEffects.GLOWING, 0, glowTicks, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        spawnBind(level, target.position().add(0, 0.5, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    public static void blightPulse(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 5;
        float damage = effect.params().has("damage_dice")
                ? DiceDamage.fromParams(effect.params(), power, 3f)
                : (effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 3f) * (power / 50f);
        int poisonTicks = scaleTicks(
                effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 60, power);
        radius *= 0.9 + power / 150f;

        pulseBlight(level, caster, effect.params(), radius, damage, poisonTicks, true);
        level.playSound(null, caster.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    public static void rotTouch(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 2.5f);
        int hungerTicks = effect.params().has("hunger_ticks") ? effect.params().get("hunger_ticks").getAsInt() : 100;
        boolean permanent = isPermanent(effect.params());
        target.hurt(SpellCombat.wither(caster), damage);
        CorruptionCurse curse = CorruptionCurse.builder("rot_touch", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(0)
                .effect(MobEffects.HUNGER, 1, hungerTicks, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        finishHit(level, target, HitFx.ROT);
    }

    public static void entropyLash(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 4f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 60;
        boolean permanent = isPermanent(effect.params());
        target.hurt(SpellCombat.magic(caster), damage);
        CorruptionCurse curse = CorruptionCurse.builder("entropy_lash", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(0)
                .effect(MobEffects.POISON, 1, poisonTicks, false)
                .effect(MobEffects.WEAKNESS, 0, poisonTicks, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        finishHit(level, target, HitFx.ENTROPY);
    }

    public static void plagueBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 80;
        int weaknessTicks = effect.params().has("weakness_ticks") ? effect.params().get("weakness_ticks").getAsInt() : 60;
        boolean permanent = isPermanent(effect.params());
        target.hurt(SpellCombat.magic(caster), damage);
        CorruptionCurse curse = CorruptionCurse.builder("plague_bolt", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(0)
                .effect(MobEffects.POISON, 0, poisonTicks, false)
                .effect(MobEffects.WEAKNESS, 0, weaknessTicks, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        finishHit(level, target, HitFx.PLAGUE);
    }

    public static void festeringWound(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 80;
        int amp = effect.params().has("wither_amplifier") ? effect.params().get("wither_amplifier").getAsInt() : 1;
        boolean permanent = isPermanent(effect.params());
        target.hurt(SpellCombat.wither(caster), damage);
        // Mid: longer control via weakness/slow; avoid infinite wither — soft DoT optional via params.
        CorruptionCurse curse = CorruptionCurse.builder("festering_wound", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(1)
                .effect(MobEffects.WITHER, amp, witherTicks, false)
                .effect(MobEffects.WEAKNESS, 0, witherTicks, permanent)
                .effect(MobEffects.MOVEMENT_SLOWDOWN, 0, witherTicks / 2, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        finishHit(level, target, HitFx.ROTBLOOD);
    }

    public static void miasmaCloak(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float dps = effect.params().has("damage_per_second") ? effect.params().get("damage_per_second").getAsFloat() : 1.5f;
        radius *= 0.9f + power / 120f;
        dps *= 0.85f + power / 100f;
        BreathDebuffs.apply(caster, new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        CorruptionFieldService.spawnMiasma(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                dps,
                0);
        spawnMiasma(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel()
                .playSound(null, caster.blockPosition(), SoundEvents.WARDEN_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.2f);
    }

    public static void blightSurge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 7;
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 100;
        radius *= 0.95 + power / 130f;
        pulseBlight(caster.serverLevel(), caster, effect.params(), radius, damage, poisonTicks, true);
    }

    public static void decayBind(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int rootTicks = scaleTicks(effect.params().get("root_ticks").getAsInt(), power, 0.8f);
        int glowTicks = scaleTicks(
                effect.params().has("glow_ticks") ? effect.params().get("glow_ticks").getAsInt() : rootTicks,
                power,
                0.8f);
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 80;
        boolean permanent = isPermanent(effect.params());
        CorruptionCurse curse = CorruptionCurse.builder("decay_bind", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.COMMON)
                .contagionChunks(1)
                .effect(MobEffects.MOVEMENT_SLOWDOWN, 5, rootTicks, permanent)
                .effect(MobEffects.DIG_SLOWDOWN, 2, rootTicks, permanent)
                .effect(MobEffects.GLOWING, 0, glowTicks, permanent)
                .effect(MobEffects.POISON, 1, poisonTicks, false)
                .effect(MobEffects.WEAKNESS, 0, poisonTicks, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        spawnBind(level, target.position().add(0, 0.5, 0));
        spawnPlague(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    public static void blightField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5.5f;
        float dps = effect.params().has("damage_per_second") ? effect.params().get("damage_per_second").getAsFloat() : 2f;
        int amp = effect.params().has("poison_amplifier") ? effect.params().get("poison_amplifier").getAsInt() : 1;
        Vec3 at = caster.position().add(caster.getLookAngle().scale(2));
        CorruptionFieldService.spawnMiasma(
                caster.serverLevel(), at, caster.getUUID(), radius * (0.9f + power / 120f), duration, dps * (0.85f + power / 100f), amp);
        spawnMiasmaPulse(caster.serverLevel(), at, radius);
    }

    public static void entropyAegis(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 240;
        int resist = effect.params().has("resistance_amplifier") ? effect.params().get("resistance_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(caster, new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resist, false, true, true));
        BreathDebuffs.apply(caster, new net.minecraft.world.effect.MobEffectInstance(MobEffects.HUNGER, duration / 2, 0, false, false, false));
        spawnEntropy(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void taintedLeech(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        float ratio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.4f;
        boolean permanent = isPermanent(effect.params());
        target.hurt(SpellCombat.magic(caster), damage);
        target.hurtMarked = true;
        caster.heal(damage * ratio);
        CorruptionCurse curse = CorruptionCurse.builder("tainted_leech", caster.getUUID())
                .cureTier(CorruptionCurse.CureTier.RARE)
                .contagionChunks(2)
                .softDotPerSecond(0.5f)
                .effect(MobEffects.WEAKNESS, 1, 200, permanent)
                .effect(MobEffects.MOVEMENT_SLOWDOWN, 1, 200, permanent)
                .fromParams(effect.params())
                .build();
        CorruptionCurseService.apply(caster, target, curse, true);
        finishHit(level, target, HitFx.LEECH);
    }

    public static void virulentWave(ServerPlayer caster, SpellEffectEntry effect, float power) {
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 8;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 120;
        int amp = effect.params().has("poison_amplifier") ? effect.params().get("poison_amplifier").getAsInt() : 2;
        boolean permanent = isPermanent(effect.params());
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
            CorruptionCurse curse = CorruptionCurse.builder("virulent_wave", caster.getUUID())
                    .cureTier(CorruptionCurse.CureTier.RARE)
                    .contagionChunks(2)
                    .softDotPerSecond(0.5f)
                    .effect(MobEffects.POISON, amp, poisonTicks, false)
                    .effect(MobEffects.CONFUSION, 0, poisonTicks / 2, permanent)
                    .effect(MobEffects.WEAKNESS, 1, poisonTicks, permanent)
                    .fromParams(effect.params())
                    .build();
            CorruptionCurseService.apply(caster, entity, curse, true);
            spawnPlague(level, entity.position().add(0, 1, 0));
        }
        spawnPlaguePulse(level, caster.position().add(0, 0.2, 0), radius);
    }

    public static void plagueCrown(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 120;
        BreathDebuffs.apply(
                caster,
                new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, true, true));
        blightSurge(caster, effect, power);
    }

    public static void omegaBlight(ServerPlayer caster, SpellEffectEntry effect, float power) {
        double radius = effect.params().has("radius") ? effect.params().get("radius").getAsDouble() : 10;
        float damage = DiceDamage.fromParams(effect.params(), power, 8f);
        int fieldTicks = effect.params().has("field_ticks") ? effect.params().get("field_ticks").getAsInt() : 160;
        float dps = effect.params().has("field_dps") ? effect.params().get("field_dps").getAsFloat() : 3f;
        pulseBlight(caster.serverLevel(), caster, effect.params(), radius * (0.95 + power / 100f), damage, 140, true);
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
            JsonObject params,
            double radius,
            float damage,
            int poisonTicks,
            boolean weakness) {
        boolean permanent = isPermanent(params);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator())) {
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(caster), damage);
            var builder = CorruptionCurse.builder("blight_pulse", caster.getUUID())
                    .cureTier(CorruptionCurse.CureTier.COMMON)
                    .contagionChunks(0)
                    .effect(MobEffects.POISON, 0, poisonTicks, false);
            if (weakness) {
                builder.effect(MobEffects.WEAKNESS, 0, poisonTicks / 2, permanent);
            }
            CorruptionCurseService.apply(caster, entity, builder.fromParams(params).build(), true);
            spawnPlague(level, entity.position().add(0, 1, 0));
        }
        spawnPlaguePulse(level, caster.position().add(0, 0.2, 0), radius);
    }

    private static boolean isPermanent(JsonObject params) {
        return params.has("permanent") && params.get("permanent").getAsBoolean();
    }

    private static int scaleTicks(int base, float power) {
        return scaleTicks(base, power, 0.85f);
    }

    private static int scaleTicks(int base, float power, float floor) {
        return Math.round(base * (floor + power / 100f));
    }

    private enum HitFx {
        MARK,
        ROT,
        PLAGUE,
        ROTBLOOD,
        ENTROPY,
        LEECH,
        BIND
    }

    private static void finishHit(ServerLevel level, LivingEntity target, HitFx fx) {
        Vec3 at = target.position().add(0, 1, 0);
        switch (fx) {
            case MARK -> spawnMark(level, at);
            case ROT -> spawnRot(level, at);
            case PLAGUE -> spawnPlague(level, at);
            case ROTBLOOD -> spawnRotBlood(level, at);
            case ENTROPY -> spawnEntropy(level, at);
            case LEECH -> spawnLeech(level, at);
            case BIND -> spawnBind(level, at);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.8f, 0.85f);
    }

    public static void spawnMark(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), pos.x, pos.y, pos.z, 8, 0.2, 0.25, 0.2, 0.01);
        level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), pos.x, pos.y, pos.z, 5, 0.15, 0.2, 0.15, 0.02);
    }

    public static void spawnRot(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_ROT.get(), pos.x, pos.y, pos.z, 12, 0.25, 0.3, 0.25, 0.02);
        level.sendParticles(ModParticleTypes.CORRUPTION_BLOOD.get(), pos.x, pos.y, pos.z, 4, 0.12, 0.15, 0.12, 0.03);
    }

    public static void spawnPlague(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), pos.x, pos.y, pos.z, 12, 0.3, 0.35, 0.3, 0.03);
        level.sendParticles(ModParticleTypes.CORRUPTION_MIASMA.get(), pos.x, pos.y, pos.z, 4, 0.2, 0.2, 0.2, 0.005);
    }

    public static void spawnRotBlood(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_BLOOD.get(), pos.x, pos.y, pos.z, 10, 0.25, 0.3, 0.25, 0.04);
        level.sendParticles(ModParticleTypes.CORRUPTION_ROT.get(), pos.x, pos.y, pos.z, 8, 0.2, 0.25, 0.2, 0.02);
    }

    public static void spawnEntropy(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_ENTROPY.get(), pos.x, pos.y, pos.z, 14, 0.3, 0.35, 0.3, 0.03);
        level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), pos.x, pos.y, pos.z, 3, 0.1, 0.15, 0.1, 0.01);
    }

    public static void spawnBind(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_BIND.get(), pos.x, pos.y, pos.z, 12, 0.35, 0.3, 0.35, 0.02);
        level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), pos.x, pos.y, pos.z, 4, 0.15, 0.2, 0.15, 0.01);
    }

    public static void spawnMiasma(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_MIASMA.get(), pos.x, pos.y, pos.z, 14, 0.4, 0.35, 0.4, 0.008);
        level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), pos.x, pos.y, pos.z, 6, 0.25, 0.25, 0.25, 0.02);
    }

    public static void spawnLeech(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.CORRUPTION_BLOOD.get(), pos.x, pos.y, pos.z, 10, 0.25, 0.3, 0.25, 0.04);
        level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), pos.x, pos.y, pos.z, 6, 0.2, 0.2, 0.2, 0.02);
    }

    /** Contagion / generic — poison + blood + rune. */
    public static void spawnCorruptionParticles(ServerLevel level, Vec3 pos) {
        spawnPlague(level, pos);
        level.sendParticles(ModParticleTypes.CORRUPTION_BLOOD.get(), pos.x, pos.y + 0.1, pos.z, 4, 0.12, 0.15, 0.12, 0.03);
        level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), pos.x, pos.y + 0.3, pos.z, 3, 0.1, 0.12, 0.1, 0.01);
    }

    public static void spawnCorruptionPulse(ServerLevel level, Vec3 center, double radius) {
        spawnPlaguePulse(level, center, radius);
    }

    public static void spawnPlaguePulse(ServerLevel level, Vec3 center, double radius) {
        int steps = Math.max(8, (int) (radius * 4));
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2 * i) / steps;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), x, center.y + 0.3, z, 2, 0.05, 0.12, 0.05, 0.02);
            level.sendParticles(ModParticleTypes.CORRUPTION_MIASMA.get(), x, center.y + 0.45, z, 1, 0.04, 0.08, 0.04, 0.002);
        }
        level.sendParticles(
                ModParticleTypes.CORRUPTION_BLOOD.get(), center.x, center.y + 0.4, center.z, 5, 0.25, 0.2, 0.25, 0.04);
    }

    public static void spawnMiasmaPulse(ServerLevel level, Vec3 center, double radius) {
        int steps = Math.max(8, (int) (radius * 3));
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2 * i) / steps;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ModParticleTypes.CORRUPTION_MIASMA.get(), x, center.y + 0.35, z, 2, 0.08, 0.1, 0.08, 0.004);
        }
        spawnMiasma(level, center.add(0, 0.4, 0));
    }
}
