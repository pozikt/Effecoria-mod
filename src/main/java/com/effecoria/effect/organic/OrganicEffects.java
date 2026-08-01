package com.effecoria.effect.organic;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.core.formula.BreathDebuffs;
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
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true, true));
        caster.clearFire();
        ServerLevel level = caster.serverLevel();
        spawnHeal(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    public static void lifeSense(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 120;
        AABB box = caster.getBoundingBox().inflate(radius);
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
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
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 180;
        subject.heal(heal);
        BreathDebuffs.apply(subject, new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0));
        spawnHeal(level, subject.position().add(0, 1, 0));
        level.playSound(null, subject.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    public static void bioStrike(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 3f);
        target.hurt(SpellCombat.wither(caster), damage);
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
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
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
        target.hurt(SpellCombat.wither(caster), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 3));
        target.hurtMarked = true;
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
    }

    public static void chitinPlates(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 12000;
        duration = Math.round(duration * (0.85f + power / 120f));
        int absorb = power >= 45f ? 1 : 0;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        ServerLevel level = caster.serverLevel();
        spawnBloom(level, caster.position().add(0, 1, 0));
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
        DamageSource source = SpellCombat.magic(caster);
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
            level.sendParticles(ModParticleTypes.ORGANIC_SAP.get(), p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
        }
        spawnAcid(level, caster.getEyePosition().add(look.scale(range * 0.5)));
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
        target.hurt(SpellCombat.wither(caster), burst);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, witherTicks, witherAmp));
        target.hurtMarked = true;
        spawnSpores(level, target.position().add(0, 1, 0));
    }

    public static void metabolicShock(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int stunTicks = effect.params().has("stun_ticks") ? effect.params().get("stun_ticks").getAsInt() : 20;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, stunTicks, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 2));
        spawnOrganicParticles(caster.serverLevel(), target.position().add(0, 1, 0));
    }

    public static void biologicalField(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 10f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 600;
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
        OrganicFieldService.spawnHeal(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                drain,
                hps);
    }

    public static void boneSpur(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 300;
        int amp = effect.params().has("strength_amplifier") ? effect.params().get("strength_amplifier").getAsInt() : 0;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, true, true));
        spawnBloom(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void senseSharpening(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 600;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void painInhibitor(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 480;
        int absorb = effect.params().has("absorption_amplifier") ? effect.params().get("absorption_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
        spawnBloom(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void poisonThorns(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 3.5f;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 80;
        int selfTicks = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 600;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, selfTicks, 0, false, true, true));
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, 0));
            spawnThorns(level, entity.position().add(0, 1, 0));
        }
        spawnThorns(level, caster.position().add(0, 1, 0));
    }

    public static void bioMimicry(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 360;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void organismAdaptation(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 900;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void immuneSuppression(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        int weakAmp = effect.params().has("weakness_amplifier") ? effect.params().get("weakness_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, weakAmp));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, duration / 2, 0));
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
    }

    public static void metabolicBoost(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 600;
        int exhaustTicks = effect.params().has("exhaustion_ticks") ? effect.params().get("exhaustion_ticks").getAsInt() : 360;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, exhaustTicks, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void organicNecrosis(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 8f);
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 100;
        int weakTicks = effect.params().has("weakness_ticks") ? effect.params().get("weakness_ticks").getAsInt() : 80;
        target.hurt(SpellCombat.wither(caster), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, weakTicks, 1));
        target.hurtMarked = true;
        spawnAcid(level, target.position().add(0, 1, 0));
    }

    public static void fullRestructuring(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 1200;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.JUMP, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void scorchedEarth(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 100;
        Vec3 center = target.position();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, 1));
            entity.hurt(SpellCombat.magic(caster), DiceDamage.fromParams(effect.params(), power, 4f) * 0.5f);
            entity.hurtMarked = true;
        }
        spawnOrganicParticles(level, center.add(0, 0.5, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 0.7f, 0.6f);
    }

    public static void bioFission(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        BlockPos center = caster.blockPosition();
        int r = (int) Math.ceil(radius);
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (center.distSqr(pos) > radius * radius) {
                continue;
            }
            var state = level.getBlockState(pos);
            if (state.is(net.minecraft.tags.BlockTags.LEAVES)
                    || state.is(net.minecraft.tags.BlockTags.LOGS)
                    || state.is(net.minecraft.tags.BlockTags.WOOL)
                    || state.is(net.minecraft.tags.BlockTags.CROPS)) {
                if (level.destroyBlock(pos, true)) {
                    broken++;
                }
            }
        }
        caster.displayClientMessage(Component.translatable("message.effecoria.organic.bio_fission", broken), true);
        spawnOrganicParticles(level, caster.position().add(0, 1, 0));
    }

    public static void superRegeneration(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float heal = DiceDamage.healFromParams(effect.params(), power, 12f);
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 360;
        int regenAmp = effect.params().has("regen_amplifier") ? effect.params().get("regen_amplifier").getAsInt() : 1;
        caster.heal(heal);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false, true));
        spawnHeal(level, caster.position().add(0, 1, 0));
    }

    public static void populationControl(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 8f;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 120;
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 60;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, 1));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
            spawnSpores(level, entity.position().add(0, 1, 0));
        }
        spawnSpores(level, caster.position().add(0, 1, 0));
    }

    public static void biologicalPlague(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float burst = DiceDamage.fromParams(effect.params(), power, 10f);
        float spread = effect.params().has("spread_radius") ? effect.params().get("spread_radius").getAsFloat() : 5f;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 160;
        applyPlagueHit(level, caster, target, burst, poisonTicks);
        AABB box = target.getBoundingBox().inflate(spread);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == target || entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(target) > spread * spread) {
                continue;
            }
            applyPlagueHit(level, caster, entity, burst * 0.45f, poisonTicks / 2);
        }
        spawnSpores(level, target.position().add(0, 1, 0));
    }

    private static void applyPlagueHit(
            ServerLevel level, ServerPlayer caster, LivingEntity entity, float damage, int poisonTicks) {
        entity.hurt(SpellCombat.wither(caster), damage);
        BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, 1));
        entity.hurtMarked = true;
    }

    public static void livingArmor(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 1800;
        int resist = effect.params().has("resistance_amplifier") ? effect.params().get("resistance_amplifier").getAsInt() : 1;
        int absorb = effect.params().has("absorption_amplifier") ? effect.params().get("absorption_amplifier").getAsInt() : 2;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resist, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        spawnBloom(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void beastForm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 900;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.JUMP, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void bioCataclysm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 12f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 600;
        float dps = OrganicFieldService.cataclysmDpsFromParams(params, power);
        OrganicFieldService.spawnCataclysm(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                dps);
    }

    public static void absoluteRegeneration(ServerPlayer caster, SpellEffectEntry effect, float power) {
        caster.removeAllEffects();
        caster.heal(caster.getMaxHealth());
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, false, true));
        spawnHeal(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void cellularDominion(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 3600;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void evolutionaryLeap(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 600;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, 3, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    public static void symbioticGraft(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        LivingEntity subject = target != null ? target : caster;
        ServerLevel level = caster.serverLevel();
        float heal = DiceDamage.healFromParams(effect.params(), power, 10f);
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 360;
        int regenAmp = effect.params().has("regen_amplifier") ? effect.params().get("regen_amplifier").getAsInt() : 1;
        subject.heal(heal);
        BreathDebuffs.apply(subject, new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp, false, true, true));
        spawnHeal(level, subject.position().add(0, 1, 0));
        level.playSound(null, subject.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    /** Ally heal + regen — requires a living target under the crosshair. */
    public static void vitalInfusion(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float heal = DiceDamage.healFromParams(effect.params(), power, 6f);
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 360;
        int regenAmp = effect.params().has("regen_amplifier") ? effect.params().get("regen_amplifier").getAsInt() : 0;
        target.heal(heal);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp, false, true, true));
        spawnHeal(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.75f, 1.25f);
    }

    /** Cleanse poison/wither/hunger, then mild heal + short regen. Requires a living target. */
    public static void soothingSap(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.WITHER);
        target.removeEffect(MobEffects.HUNGER);
        float heal = DiceDamage.healFromParams(effect.params(), power, 3f);
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 180;
        target.heal(heal);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0, false, true, true));
        spawnHeal(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.65f, 1.45f);
    }

    /** Absorption + resistance ward on a living target. */
    public static void vitalWard(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 600;
        duration = Math.round(duration * (0.85f + power / 120f));
        int absorb = effect.params().has("absorption_amplifier")
                ? effect.params().get("absorption_amplifier").getAsInt()
                : (power >= 45f ? 1 : 0);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.ABSORPTION, duration, absorb, false, true, true));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.ARMOR_EQUIP_TURTLE.value(),
                SoundSource.PLAYERS,
                0.7f,
                1.2f);
    }

    /** Speed + strength gift on a living target. */
    public static void adrenalGift(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 600;
        duration = Math.round(duration * (0.85f + power / 120f));
        int speedAmp = effect.params().has("speed_amplifier") ? effect.params().get("speed_amplifier").getAsInt() : 0;
        int strengthAmp =
                effect.params().has("strength_amplifier") ? effect.params().get("strength_amplifier").getAsInt() : 0;
        BreathDebuffs.apply(
                target, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, speedAmp, false, true, true));
        BreathDebuffs.apply(
                target, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strengthAmp, false, false, true));
        spawnHeal(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.35f, 1.6f);
    }

    public static void limbRegeneration(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float heal = DiceDamage.healFromParams(effect.params(), power, 14f);
        int regenTicks = effect.params().has("regen_ticks") ? effect.params().get("regen_ticks").getAsInt() : 1200;
        int regenAmp = effect.params().has("regen_amplifier") ? effect.params().get("regen_amplifier").getAsInt() : 2;
        caster.heal(heal);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.SATURATION, 40, 0, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, 140, 0, false, false, true));
        spawnHeal(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5f, 1.3f);
    }

    public static void verdantBloom(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        float heal = DiceDamage.healFromParams(effect.params(), power, 4f);
        int bloomRadius = effect.params().has("bloom_radius") ? effect.params().get("bloom_radius").getAsInt() : 4;
        caster.heal(heal);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
            if (ally.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            ally.heal(heal * 0.5f);
            spawnBloom(level, ally.position().add(0, 1, 0));
        }
        bloomNearby(level, caster.blockPosition(), bloomRadius);
        spawnBloom(level, caster.position().add(0, 1, 0));
    }

    public static void geneticLock(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        target.removeEffect(MobEffects.REGENERATION);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, duration, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.HUNGER, duration, 2));
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
    }

    public static void biologicalCleaving(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 7f);
        int armorDamage = effect.params().has("armor_damage") ? effect.params().get("armor_damage").getAsInt() : 80;
        target.hurt(SpellCombat.magic(caster), damage);
        target.hurtMarked = true;
        shredOrganicArmor(target, armorDamage);
        spawnOrganicParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8f, 0.5f);
    }

    public static void fullTransformation(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 1200;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 2, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.JUMP, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));
        spawnOrganicParticles(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void sporeStorm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 7f;
        int poisonTicks = effect.params().has("poison_ticks") ? effect.params().get("poison_ticks").getAsInt() : 140;
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.magic(caster), damage);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.POISON, poisonTicks, 1));
            entity.hurtMarked = true;
            spawnSpores(level, entity.position().add(0, 1, 0));
        }
        spawnSpores(level, caster.position().add(0, 1, 0));
    }

    public static void biologicalSingularity(ServerPlayer caster, SpellEffectEntry effect, float power) {
        JsonObject params = effect.params();
        float radius = params.has("radius") ? params.get("radius").getAsFloat() : 10f;
        int duration = params.has("duration_ticks") ? params.get("duration_ticks").getAsInt() : 720;
        float healPerSecond;
        if (params.has("heal_dice_per_round")) {
            JsonObject healParams = new JsonObject();
            healParams.addProperty("damage_dice_per_round", params.get("heal_dice_per_round").getAsString());
            healPerSecond = DiceDamage.perSecondFromParams(healParams, power, 1.5f);
        } else if (params.has("heal_per_second")) {
            healPerSecond = params.get("heal_per_second").getAsFloat() * (0.75f + power / 100f);
        } else {
            healPerSecond = 1.5f * (0.75f + power / 100f);
        }
        float damagePerSecond = params.has("damage_dice_per_round")
                ? OrganicFieldService.cataclysmDpsFromParams(params, power)
                : OrganicFieldService.cataclysmDpsFromParams(params, power);
        OrganicFieldService.spawnSingularity(
                caster.serverLevel(),
                caster.position().add(0, 0.5, 0),
                caster.getUUID(),
                radius,
                duration,
                healPerSecond,
                damagePerSecond);
    }

    public static void lifeCreation(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int bloomRadius = effect.params().has("bloom_radius") ? effect.params().get("bloom_radius").getAsInt() : 5;
        Vec3 look = caster.getLookAngle().normalize();
        double x = caster.getX() + look.x * 2.5;
        double z = caster.getZ() + look.z * 2.5;
        double y = caster.getY();
        var chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(level);
        if (chicken != null) {
            chicken.moveTo(x, y, z, caster.getYRot(), 0f);
            chicken.setPersistenceRequired();
            level.addFreshEntity(chicken);
        }
        bloomNearby(level, BlockPos.containing(x, y, z), bloomRadius);
        caster.heal(DiceDamage.healFromParams(effect.params(), power, 3f));
        spawnHeal(level, new Vec3(x, y + 1, z));
        level.playSound(null, caster.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.8f, 1.1f);
    }

    public static void biologicalImmortality(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 1800;
        int aftermath = effect.params().has("aftermath_ticks") ? effect.params().get("aftermath_ticks").getAsInt() : 200;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, duration, 3, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, 3, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, aftermath, 2, false, false, true));
        spawnHeal(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 0.9f);
    }

    private static void shredOrganicArmor(LivingEntity target, int amount) {
        for (net.minecraft.world.entity.EquipmentSlot slot :
                new net.minecraft.world.entity.EquipmentSlot[] {
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET
                }) {
            net.minecraft.world.item.ItemStack stack = target.getItemBySlot(slot);
            if (stack.isEmpty() || !isOrganicArmor(stack)) {
                continue;
            }
            stack.hurtAndBreak(amount, target, slot);
        }
    }

    private static boolean isOrganicArmor(net.minecraft.world.item.ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.WOOL)) {
            return true;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem())
                .getPath();
        return path.contains("leather");
    }

    private static void bloomNearby(ServerLevel level, BlockPos center, int radius) {
        net.minecraft.util.RandomSource random = level.getRandom();
        for (BlockPos pos :
                BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 2, radius))) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock growable
                    && growable.isValidBonemealTarget(level, pos, state)
                    && growable.isBonemealSuccess(level, random, pos, state)
                    && random.nextFloat() < 0.45f) {
                growable.performBonemeal(level, random, pos, state);
            }
        }
    }

    private static void tagProjectile(Snowball projectile, float damage) {
        projectile.getPersistentData().putBoolean(OrganicTags.PROJECTILE, true);
        projectile.getPersistentData().putString(OrganicTags.KIND, OrganicTags.KIND_BONE_NEEDLE);
        projectile.getPersistentData().putFloat(OrganicTags.POWER, damage);
    }

    public static void spawnOrganicParticles(ServerLevel level, Vec3 pos) {
        spawnBloom(level, pos);
    }

    public static void spawnBloom(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.35, 0.4, 0.35, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_FOG.get(), pos.x, pos.y + 0.8, pos.z, 6, 0.25, 0.3, 0.25, 0.01);
    }

    public static void spawnRoots(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_ROOT.get(), pos.x, pos.y, pos.z, 12, 0.28, 0.08, 0.28, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.25, pos.z, 4, 0.2, 0.15, 0.2, 0.01);
    }

    public static void spawnThorns(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_THORN.get(), pos.x, pos.y + 0.4, pos.z, 10, 0.3, 0.35, 0.3, 0.05);
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.5, pos.z, 4, 0.2, 0.2, 0.2, 0.01);
    }

    public static void spawnSap(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_SAP.get(), pos.x, pos.y + 0.6, pos.z, 8, 0.2, 0.25, 0.2, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.4, pos.z, 5, 0.25, 0.3, 0.25, 0.01);
        level.sendParticles(ModParticleTypes.ORGANIC_FOG.get(), pos.x, pos.y + 0.7, pos.z, 4, 0.2, 0.2, 0.2, 0.008);
    }

    /** Tissue stabilize / heal — erythrocytes + leukocytes (no plant motifs). */
    public static void spawnHeal(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_BLOOD_CELL.get(), pos.x, pos.y + 0.55, pos.z, 12, 0.28, 0.35, 0.28, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_WHITE_CELL.get(), pos.x, pos.y + 0.65, pos.z, 6, 0.22, 0.28, 0.22, 0.015);
        level.sendParticles(ParticleTypes.HEART, pos.x, pos.y + 1.0, pos.z, 1, 0.12, 0.1, 0.12, 0.0);
    }

    public static void spawnSpores(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_SPORE.get(), pos.x, pos.y + 0.6, pos.z, 14, 0.4, 0.45, 0.4, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_FOG.get(), pos.x, pos.y + 0.5, pos.z, 6, 0.3, 0.3, 0.3, 0.01);
    }

    public static void spawnAcid(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_SAP.get(), pos.x, pos.y + 0.5, pos.z, 10, 0.25, 0.3, 0.25, 0.03);
        level.sendParticles(ModParticleTypes.ORGANIC_FOG.get(), pos.x, pos.y + 0.4, pos.z, 5, 0.2, 0.2, 0.2, 0.01);
    }
}
