package com.effecoria.effect;

import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.magic.SpellEffectEntry;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            case "fireball" -> fireball(caster, effect, power);
            case "wind_charge" -> windCharge(caster, effect, power);
            case "water_stream" -> waterStream(caster, effect, power);
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
        spawnMindParticles(caster.serverLevel(), target.position());
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
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
        spawnMindParticles(caster.serverLevel(), target.position());
    }

    private static void phiSense(ServerPlayer caster, SpellEffectEntry effect) {
        int duration = effect.params().get("duration_ticks").getAsInt();
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.phi_sense_active"), true);
    }

    /** Blaze-style small fireball — damages entities, does not break blocks. */
    private static void fireball(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 1.4f;
        speed *= 0.85f + (power / 100f);

        Vec3 velocity = look.scale(speed);
        SmallFireball fireball = new SmallFireball(level, caster, velocity);
        fireball.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        level.addFreshEntity(fireball);

        level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 1f);
        spawnFireCastParticles(level, caster.getEyePosition());
    }

    /** Breeze-style wind charge — knockback burst on impact, no block damage. */
    private static void windCharge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 1.25f;
        speed *= 0.9f + (power / 120f);

        WindCharge charge = new WindCharge(
                caster,
                level,
                caster.getX(),
                caster.getEyeY() - 0.1,
                caster.getZ());
        charge.shoot(look.x, look.y, look.z, speed, 0f);
        level.addFreshEntity(charge);

        level.playSound(null, caster.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 1f, 1f);
        spawnWindCastParticles(level, caster.getEyePosition(), look);
    }

    /** Directed water jet — damage + push, splash particles only. */
    private static void waterStream(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        double range = effect.params().get("range").getAsDouble();
        float damage = effect.params().get("damage").getAsFloat();
        float knockback = effect.params().has("knockback") ? effect.params().get("knockback").getAsFloat() : 1.2f;
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 40;

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        float scaledDamage = damage * (power / 50f);
        float scaledKnock = knockback * (power / 50f);
        DamageSource source = caster.level().damageSources().magic();

        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(start);
            double along = toTarget.dot(look);
            if (along < 0 || along > range) {
                continue;
            }
            Vec3 lateral = toTarget.subtract(look.scale(along));
            if (lateral.lengthSqr() > 2.5) {
                continue;
            }
            hit.add(target);
        }

        for (LivingEntity target : hit) {
            target.hurt(source, scaledDamage);
            target.push(look.x * scaledKnock, 0.15, look.z * scaledKnock);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 0));
            target.hurtMarked = true;
            spawnWaterHitParticles(level, target.position());
        }

        spawnWaterBeamParticles(level, start, look, range);
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8f, 1.1f);
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

    private static void spawnMindParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 1, pos.z, 10, 0.2, 0.3, 0.2, 0.01);
    }

    private static void spawnFireCastParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 16, 0.1, 0.1, 0.1, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 6, 0.05, 0.05, 0.05, 0.01);
    }

    private static void spawnWindCastParticles(ServerLevel level, Vec3 pos, Vec3 look) {
        for (int i = 1; i <= 6; i++) {
            Vec3 p = pos.add(look.scale(i * 0.35));
            level.sendParticles(ParticleTypes.GUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private static void spawnWaterBeamParticles(ServerLevel level, Vec3 start, Vec3 look, double range) {
        int steps = (int) (range * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = start.add(look.scale(i * 0.25));
            level.sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.05);
            level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
        }
    }

    private static void spawnWaterHitParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y + 1, pos.z, 12, 0.3, 0.4, 0.3, 0.1);
        level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y + 1.5, pos.z, 8, 0.2, 0.2, 0.2, 0.02);
    }
}
