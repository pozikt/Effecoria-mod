package com.effecoria.effect;

import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SpellEffectExecutor {
    private SpellEffectExecutor() {}

    public static void applyAll(ServerPlayer caster, SpellDefinition spell, float power) {
        for (SpellEffectEntry effect : spell.effects()) {
            apply(caster, effect, power);
        }
    }

    private static void apply(ServerPlayer caster, SpellEffectEntry effect, float power) {
        switch (effect.type().getPath()) {
            case "telekinesis" -> telekinesis(caster, effect, power);
            case "mind_sting" -> mindSting(caster, effect, power);
            case "phi_sense" -> phiSense(caster, effect);
            case "fire_burst" -> fireBurst(caster, effect, power);
            case "knockback" -> knockback(caster, effect, power);
            case "absorption" -> absorption(caster, effect, power);
            default -> {}
        }
    }

    private static void telekinesis(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float force = effect.params().get("force").getAsFloat();
        double range = effect.params().get("range").getAsDouble();
        Entity target = raycastEntity(caster, range);
        if (target == null) {
            return;
        }
        Vec3 look = caster.getLookAngle().normalize();
        double strength = force * (power / 50f);
        target.setDeltaMovement(target.getDeltaMovement().add(look.scale(strength)));
        target.hurtMarked = true;
        spawnCastParticles(caster.serverLevel(), target.position());
    }

    private static void mindSting(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float damage = effect.params().get("damage").getAsFloat();
        int slowTicks = effect.params().get("slow_duration_ticks").getAsInt();
        LivingEntity target = raycastLiving(caster, 12);
        if (target == null) {
            return;
        }
        float scaledDamage = damage * (power / 50f);
        DamageSource source = caster.level().damageSources().magic();
        target.hurt(source, scaledDamage);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slowTicks, 1));
        spawnCastParticles(caster.serverLevel(), target.position());
    }

    private static void phiSense(ServerPlayer caster, SpellEffectEntry effect) {
        int duration = effect.params().get("duration_ticks").getAsInt();
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.phi_sense_active"), true);
    }

    private static void fireBurst(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float damage = effect.params().get("damage").getAsFloat();
        int igniteTicks = effect.params().get("ignite_ticks").getAsInt();
        double radius = effect.params().get("radius").getAsDouble();
        Vec3 center = caster.position();
        AABB box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
        List<LivingEntity> targets = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != caster && entity.isAlive());
        float scaledDamage = damage * (power / 50f);
        DamageSource source = caster.level().damageSources().magic();
        for (LivingEntity target : targets) {
            if (target.position().distanceTo(center) <= radius) {
                target.hurt(source, scaledDamage);
                target.setRemainingFireTicks(igniteTicks);
            }
        }
        spawnCastParticles(caster.serverLevel(), center);
    }

    private static void knockback(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float force = effect.params().get("force").getAsFloat();
        double range = effect.params().get("range").getAsDouble();
        LivingEntity target = raycastLiving(caster, range);
        if (target == null) {
            return;
        }
        target.knockback(force * (power / 50f), caster.getX() - target.getX(), caster.getZ() - target.getZ());
        spawnCastParticles(caster.serverLevel(), target.position());
    }

    private static void absorption(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int hearts = effect.params().get("absorption_hearts").getAsInt();
        int duration = effect.params().get("duration_ticks").getAsInt();
        int amplifier = Math.max(0, Math.round(hearts * (power / 50f)) - 1);
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        spawnCastParticles(caster.serverLevel(), caster.position());
    }

    private static Entity raycastEntity(ServerPlayer caster, double range) {
        HitResult hit = caster.pick(range, 0f, false);
        if (hit instanceof EntityHitResult entityHit) {
            return entityHit.getEntity();
        }
        return null;
    }

    private static LivingEntity raycastLiving(ServerPlayer caster, double range) {
        Entity entity = raycastEntity(caster, range);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void spawnCastParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 1, pos.z, 12, 0.2, 0.3, 0.2, 0.01);
    }
}
