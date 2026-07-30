package com.effecoria.effect.organic;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Organic / Orkanum tissue magic from the D&D doc. */
public final class OrganicEffects {
    private OrganicEffects() {}

    public static void diagnosticGlimpse(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int labelTicks = effect.params().has("label_ticks") ? effect.params().get("label_ticks").getAsInt() : 80;
        OrganicDiagnosticService.DiagnosticReadout readout = OrganicDiagnosticService.readoutFor(target);
        OrganicDiagnosticService.applyLabel(level, target, labelTicks);

        int hp = Math.round(target.getHealth());
        int maxHp = Math.round(target.getMaxHealth());
        caster.displayClientMessage(
                Component.translatable(
                        "message.effecoria.organic.diagnostic.readout",
                        target.getDisplayName(),
                        hp,
                        maxHp,
                        readout.healthPercent(),
                        Component.translatable(readout.statusKey())),
                true);
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                target.getX(),
                target.getY() + target.getBbHeight() + 0.35,
                target.getZ(),
                6,
                0.25,
                0.15,
                0.25,
                0.02);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    public static void bloodStasis(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float heal = DiceDamage.healFromParams(effect.params(), power, 2f);
        caster.heal(heal);
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true, true));
        caster.clearFire();
        ServerLevel level = caster.serverLevel();
        spawnOrganicParticles(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    public static void lifeSense(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 40;
        AABB box = caster.getBoundingBox().inflate(radius);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
            count++;
        }
        caster.displayClientMessage(Component.translatable("message.effecoria.organic.life_sense", count), true);
        spawnOrganicParticles(level, caster.position().add(0, 1, 0));
    }

    public static void applyVitality(
            ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        LivingEntity subject = target != null ? target : caster;
        ServerLevel level = caster.serverLevel();
        float heal = DiceDamage.healFromParams(effect.params(), power, 4f);
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 60;
        subject.heal(heal);
        subject.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0));
        spawnOrganicParticles(level, subject.position().add(0, 1, 0));
        level.playSound(null, subject.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    public static void bioStrike(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        target.hurt(level.damageSources().wither(), damage);
        target.hurtMarked = true;
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    public static void boneNeedle(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        Vec3 look = caster.getLookAngle().normalize();
        float speed = effect.params().has("speed") ? effect.params().get("speed").getAsFloat() : 1.6f;
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        Snowball needle = new Snowball(level, caster);
        needle.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        needle.shoot(look.x, look.y, look.z, speed, 0.4f);
        tagProjectile(needle, damage);
        level.addFreshEntity(needle);
        level.playSound(null, caster.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    public static void foreignAgent(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 100;
        target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
        spawnOrganicParticles(caster.serverLevel(), target.position().add(0, 1, 0));
        caster.serverLevel()
                .playSound(null, target.blockPosition(), SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    public static void muscleSpasm(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 30;
        target.hurt(level.damageSources().wither(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 3));
        target.hurtMarked = true;
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
    }

    public static void chitinPlates(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 12000;
        duration = Math.round(duration * (0.85f + power / 120f));
        int absorb = power >= 45f ? 1 : 0;
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        ServerLevel level = caster.serverLevel();
        spawnOrganicParticles(level, caster.position().add(0, 1, 0));
        level.playSound(
                null,
                caster.blockPosition(),
                SoundEvents.ARMOR_EQUIP_NETHERITE.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.3f);
    }

    public static void acidGland(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        double range = params.has("range") ? params.get("range").getAsDouble() : 6;
        float damage = DiceDamage.fromParams(params, power, 5f);
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.getEyePosition();
        DamageSource source = level.damageSources().magic();
        AABB sweep = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != caster && e.isAlive())) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(start);
            double along = to.dot(look);
            if (along < 0 || along > range || to.subtract(look.scale(along)).lengthSqr() > 2.2) {
                continue;
            }
            entity.hurt(source, damage);
            entity.hurtMarked = true;
        }
        for (int i = 0; i <= (int) (range * 3); i++) {
            Vec3 p = start.add(look.scale(i * 0.35));
            level.sendParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    public static void parasiticInfection(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        JsonObject params = effect.params();
        float burst = DiceDamage.fromParams(params, power, 5f);
        int witherTicks = params.has("wither_ticks") ? params.get("wither_ticks").getAsInt() : 60;
        int witherAmp = params.has("wither_amplifier") ? params.get("wither_amplifier").getAsInt() : 0;
        target.hurt(level.damageSources().wither(), burst);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, witherAmp));
        target.hurtMarked = true;
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
    }

    public static void metabolicShock(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int stunTicks = effect.params().has("stun_ticks") ? effect.params().get("stun_ticks").getAsInt() : 20;
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, stunTicks, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 2));
        spawnOrganicParticles(caster.serverLevel(), target.position().add(0, 1, 0));
    }

    public static void biologicalField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 10f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 200;
        float drain = params.has("maintain_drain_per_second")
                ? params.get("maintain_drain_per_second").getAsFloat()
                : 4f;
        JsonObject healParams = new JsonObject();
        if (params.has("heal_dice_per_round")) {
            healParams.addProperty("damage_dice_per_round", params.get("heal_dice_per_round").getAsString());
        } else {
            healParams.addProperty("damage_dice_per_round", "1d6");
        }
        float hps = DiceDamage.perSecondFromParams(healParams, power, 1.5f);
        OrganicFieldService.spawn(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                drain,
                hps);
    }

    private static void tagProjectile(Snowball projectile, float damage) {
        projectile.getPersistentData().putBoolean(OrganicTags.PROJECTILE, true);
        projectile.getPersistentData().putString(OrganicTags.KIND, OrganicTags.KIND_BONE_NEEDLE);
        projectile.getPersistentData().putFloat(OrganicTags.POWER, damage);
    }

    public static void spawnOrganicParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.35, 0.4, 0.35, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_ROOT.get(), pos.x, pos.y, pos.z, 4, 0.2, 0.05, 0.2, 0.01);
        level.sendParticles(ModParticleTypes.ORGANIC_FOG.get(), pos.x, pos.y + 0.8, pos.z, 6, 0.25, 0.3, 0.25, 0.01);
    }
}
