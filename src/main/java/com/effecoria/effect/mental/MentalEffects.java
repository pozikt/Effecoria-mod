package com.effecoria.effect.mental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
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

public final class MentalEffects {
    private MentalEffects() {}

    public static void mindBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 4f);
        target.hurt(level.damageSources().magic(), damage);
        target.hurtMarked = true;
        finishHit(level, target.position());
    }

    public static void psychicScream(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        int confuseTicks = effect.params().has("confusion_ticks") ? effect.params().get("confusion_ticks").getAsInt() : 80;
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, confuseTicks, 0));
            entity.hurtMarked = true;
            spawnMindParticles(level, entity.position());
        }
        spawnMindParticles(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    public static void thoughtLance(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 8f);
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 60;
        target.hurt(level.damageSources().magic(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 2));
        target.hurtMarked = true;
        finishHit(level, target.position());
        level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 0.5f);
    }

    public static void neuralLock(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        finishHit(caster.serverLevel(), target.position());
    }

    public static void telekineticCrush(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 7f);
        float lift = effect.params().has("lift_force") ? effect.params().get("lift_force").getAsFloat() : 0.65f;
        target.hurt(level.damageSources().magic(), damage);
        target.setDeltaMovement(target.getDeltaMovement().add(0, lift * (power / 50f), 0));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
        target.hurtMarked = true;
        finishHit(level, target.position());
    }

    public static void massConfusion(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 8f;
        int ticks = effect.params().has("confusion_ticks") ? effect.params().get("confusion_ticks").getAsInt() : 120;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 1));
            spawnMindParticles(level, entity.position().add(0, 1, 0));
        }
        spawnMindParticles(level, caster.position().add(0, 1, 0));
    }

    public static void psychicBarrier(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, false, true));
        spawnMindParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void mindProbe(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration / 2, 0));
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "message.effecoria.mental.mind_probe",
                        target.getDisplayName(),
                        Math.round(target.getHealth()),
                        Math.round(target.getMaxHealth())),
                true);
        finishHit(caster.serverLevel(), target.position());
    }

    public static void synapticOverload(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        int confuseTicks = effect.params().has("confusion_ticks") ? effect.params().get("confusion_ticks").getAsInt() : 100;
        target.hurt(level.damageSources().magic(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, confuseTicks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, confuseTicks / 2, 1));
        target.hurtMarked = true;
        finishHit(level, target.position());
    }

    public static void psychicDrain(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        float ratio = effect.params().has("psi_ratio") ? effect.params().get("psi_ratio").getAsFloat() : 0.15f;
        target.hurt(level.damageSources().magic(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        PlayerPsiData data = PsiHelper.get(caster);
        data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + damage * ratio));
        PsiHelper.set(caster, data);
        target.hurtMarked = true;
        finishHit(level, target.position());
    }

    public static void mentalFortress(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 400;
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, false, true));
        spawnMindParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void thoughtBomb(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        float damage = DiceDamage.fromParams(effect.params(), power, 10f);
        Vec3 center = target.position();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
            entity.hurtMarked = true;
            spawnMindParticles(level, entity.position().add(0, 1, 0));
        }
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    public static void psychicStorm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 7f;
        int pulses = effect.params().has("pulses") ? effect.params().get("pulses").getAsInt() : 3;
        float perPulse = DiceDamage.fromParams(effect.params(), power, 4f) / pulses;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (int p = 0; p < pulses; p++) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity == caster) {
                    continue;
                }
                if (entity.distanceToSqr(caster) > radius * radius) {
                    continue;
                }
                entity.hurt(level.damageSources().magic(), perPulse);
                entity.hurtMarked = true;
            }
        }
        spawnMindParticles(level, caster.position().add(0, 1, 0));
    }

    public static void psychicAmplify(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 400;
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration);
        PsiHelper.set(caster, data);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration / 2, 0, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration / 2, 0, false, false, true));
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.phi_sense_active"), true);
        spawnMindParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void omegaMind(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        int senseTicks = effect.params().has("phi_sense_ticks") ? effect.params().get("phi_sense_ticks").getAsInt() : 300;
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + senseTicks);
        PsiHelper.set(caster, data);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 2, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
        spawnMindParticles(caster.serverLevel(), caster.position().add(0, 1.2, 0));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    public static void mindTerror(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.TERROR, 140);
    }

    public static void cliffUrge(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.CLIFF, 160);
    }

    public static void psychicFrenzy(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.FRENZY, 120);
    }

    private static void applyCompulsion(
            ServerPlayer caster,
            SpellEffectEntry effect,
            float power,
            LivingEntity target,
            MentalCompulsionService.Type type,
            int defaultTicks) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : defaultTicks;
        duration = Math.round(duration * (0.85f + power / 120f));
        if (!MentalCompulsionService.apply(caster, target, type, duration)) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.mental.compel_invalid"),
                    true);
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(duration, 100), 0));
        finishHit(caster.serverLevel(), target.position());
        caster.serverLevel().playSound(
                null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.55f, 1.5f);
    }

    public static void telekineticSurge(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        float force = effect.params().has("force") ? effect.params().get("force").getAsFloat() : 3.5f;
        Vec3 look = caster.getLookAngle().normalize();
        double strength = force * (power / 50f);
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        target.setDeltaMovement(target.getDeltaMovement().add(look.scale(strength)));
        target.hurt(caster.serverLevel().damageSources().magic(), damage);
        target.hurtMarked = true;
        finishHit(caster.serverLevel(), target.position());
    }

    private static void finishHit(ServerLevel level, Vec3 pos) {
        spawnMindParticles(level, pos.add(0, 1, 0));
        level.playSound(null, net.minecraft.core.BlockPos.containing(pos), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.55f, 1.4f);
    }

    public static void spawnMindParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y + 1.5, pos.z, 10, 0.3, 0.2, 0.3, 0.005);
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y + 1.0, pos.z, 6, 0.2, 0.15, 0.2, 0.003);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y + 0.8, pos.z, 4, 0.15, 0.2, 0.15, 0.02);
    }
}
