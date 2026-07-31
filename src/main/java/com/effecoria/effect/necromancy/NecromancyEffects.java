package com.effecoria.effect.necromancy;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.google.gson.JsonObject;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
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
        spawnSkeletonThrall(caster, target, 0);
    }

    public static void shadeBrood(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int count = effect.params().has("count") ? effect.params().get("count").getAsInt() : 2;
        count = Math.min(4, Math.max(1, count));
        Vec3 look = caster.getLookAngle().normalize();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (!NecroSummonService.canAffordAnother(caster)) {
                break;
            }
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
            shade.setAggressive(true);
            level.addFreshEntity(shade);
            if (NecroSummonService.register(shade, caster, target)) {
                spawned++;
            }
        }
        if (spawned > 0) {
            level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.9f, 0.7f);
        } else {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.necro.summon_psi_reserve",
                            (int) com.effecoria.config.BalanceConfig.NECRO_SUMMON_PSI_RESERVE.get().floatValue()),
                    true);
        }
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

    public static void necroticBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 40;
        target.hurt(level.damageSources().wither(), damage);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        target.hurtMarked = true;
        finishHit(level, target);
    }

    public static void graveBind(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int rootTicks = effect.params().has("root_ticks") ? effect.params().get("root_ticks").getAsInt() : 120;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 5));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, rootTicks, 2));
        finishHit(caster.serverLevel(), target);
    }

    public static void curseOfFrailty(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 140;
        int weakAmp = effect.params().has("weakness_amplifier") ? effect.params().get("weakness_amplifier").getAsInt() : 1;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, weakAmp));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0));
        finishHit(caster.serverLevel(), target);
    }

    public static void hauntingVisage(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration / 2, 0));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration / 2, 0));
        finishHit(caster.serverLevel(), target);
    }

    public static void corpseBurst(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        Vec3 center = target.position();
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().wither(), damage);
            entity.hurtMarked = true;
            spawnNecroParticles(level, entity.position().add(0, 1, 0));
        }
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 0.7f);
    }

    public static void raiseZombie(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        spawnZombieThrall(caster, target, 0);
    }

    public static void boneVolley(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int hits = effect.params().has("hits") ? effect.params().get("hits").getAsInt() : 3;
        float perHit = DiceDamage.fromParams(effect.params(), power, 4f) / Math.max(1, hits);
        for (int i = 0; i < hits; i++) {
            target.hurt(level.damageSources().wither(), perHit);
        }
        target.hurtMarked = true;
        finishHit(level, target);
    }

    public static void necroticAura(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float pulse = DiceDamage.fromParams(effect.params(), power, 3f);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().wither(), pulse);
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            entity.hurtMarked = true;
            spawnNecroParticles(level, entity.position().add(0, 1, 0));
        }
        spawnNecroParticles(level, caster.position().add(0, 1, 0));
    }

    public static void soulAnchor(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 80;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
        target.hurtMarked = true;
        finishHit(caster.serverLevel(), target);
    }

    public static void armyOfDead(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int count = effect.params().has("count") ? effect.params().get("count").getAsInt() : 3;
        count = Math.min(4, Math.max(2, count));
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            int before = NecroSummonService.countOwned(caster);
            spawnSkeletonThrall(caster, target, i);
            if (NecroSummonService.countOwned(caster) > before) {
                spawned++;
            } else {
                break;
            }
        }
        if (spawned > 0) {
            caster.serverLevel().playSound(
                    null, caster.blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.PLAYERS, 0.9f, 0.6f);
        }
    }

    public static void deathGate(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        double range = effect.params().has("range") ? effect.params().get("range").getAsDouble() : 8;
        double minRange = effect.params().has("min_range") ? effect.params().get("min_range").getAsDouble() : 2;
        range = Math.min(18, range * (0.85 + power / 120f));
        float trailDamage = DiceDamage.fromParams(effect.params(), power, 4f);
        float trailRadius = effect.params().has("trail_radius") ? effect.params().get("trail_radius").getAsFloat() : 2.5f;

        Vec3 origin = caster.position();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 best = null;
        for (double dist = range; dist >= minRange; dist -= 0.5) {
            Vec3 candidate = origin.add(look.scale(dist));
            BlockPos feet = BlockPos.containing(candidate.x, candidate.y, candidate.z);
            BlockPos head = feet.above();
            if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
                continue;
            }
            if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                continue;
            }
            best = new Vec3(candidate.x, feet.getY(), candidate.z);
            break;
        }
        if (best == null) {
            return;
        }
        hurtInRadius(level, origin, trailRadius, trailDamage, caster);
        spawnNecroParticles(level, origin.add(0, 1, 0));
        caster.teleportTo(best.x, best.y, best.z);
        caster.fallDistance = 0f;
        spawnNecroParticles(level, best.add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 0.6f);
    }

    public static void soulReaper(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = DiceDamage.fromParams(effect.params(), power, 9f);
        float threshold = effect.params().has("execute_threshold") ? effect.params().get("execute_threshold").getAsFloat() : 0.35f;
        float healRatio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.5f;
        if (target.getHealth() / target.getMaxHealth() <= threshold) {
            damage *= 1.75f;
        }
        target.hurt(level.damageSources().wither(), damage);
        caster.heal(damage * healRatio);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        target.hurtMarked = true;
        finishHit(level, target);
    }

    public static void phylacterySurge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        long gameTime = level.getGameTime();
        PlayerPsiData data = PsiHelper.get(caster);
        float heal = DiceDamage.healFromParams(effect.params(), power, 8f);
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        caster.heal(heal);
        hurtInRadius(level, caster.position(), radius, damage, caster);
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, true, true));
        if (data.isLichAscensionActive(gameTime)) {
            float bonus = effect.params().has("phyl_boost") ? effect.params().get("phyl_boost").getAsFloat() : 0.12f;
            data.boostPhylacteryEfficiency(gameTime, bonus);
            PsiHelper.set(caster, data);
        }
        spawnNecroParticles(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 0.7f);
    }

    public static void lichAscension(ServerPlayer caster, SpellEffectEntry effect, float power) {
        // Gated until mage-tower / phylactery content (Stage IV). See docs/MAGIC_PLAN.md.
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.necro.lich_locked"), true);
    }

    private static void hurtInRadius(ServerLevel level, Vec3 center, float radius, float damage, ServerPlayer skip) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == skip) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(level.damageSources().wither(), damage);
            entity.hurtMarked = true;
            spawnNecroParticles(level, entity.position().add(0, 1, 0));
        }
    }

    private static void spawnSkeletonThrall(ServerPlayer caster, LivingEntity target, int index) {
        ServerLevel level = caster.serverLevel();
        if (!NecroSummonService.canAffordAnother(caster)) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.necro.summon_psi_reserve",
                            (int) com.effecoria.config.BalanceConfig.NECRO_SUMMON_PSI_RESERVE.get().floatValue()),
                    true);
            return;
        }
        Vec3 look = caster.getLookAngle().normalize();
        double angle = index * 1.2;
        double x = caster.getX() + look.x * 2 + Math.cos(angle);
        double z = caster.getZ() + look.z * 2 + Math.sin(angle);
        double y = caster.getY();
        Skeleton skeleton = EntityType.SKELETON.create(level);
        if (skeleton == null) {
            return;
        }
        skeleton.moveTo(x, y, z, caster.getYRot(), 0f);
        level.addFreshEntity(skeleton);
        if (NecroSummonService.register(skeleton, caster, target)) {
            spawnNecroParticles(level, new Vec3(x, y + 1, z));
        }
    }

    private static void spawnZombieThrall(ServerPlayer caster, LivingEntity target, int index) {
        ServerLevel level = caster.serverLevel();
        if (!NecroSummonService.canAffordAnother(caster)) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.effecoria.necro.summon_psi_reserve",
                            (int) com.effecoria.config.BalanceConfig.NECRO_SUMMON_PSI_RESERVE.get().floatValue()),
                    true);
            return;
        }
        Vec3 look = caster.getLookAngle().normalize();
        double x = caster.getX() + look.x * (2 + index * 0.5);
        double z = caster.getZ() + look.z * (2 + index * 0.5);
        double y = caster.getY();
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            return;
        }
        zombie.moveTo(x, y, z, caster.getYRot(), 0f);
        level.addFreshEntity(zombie);
        if (NecroSummonService.register(zombie, caster, target)) {
            spawnNecroParticles(level, new Vec3(x, y + 1, z));
            level.playSound(null, caster.blockPosition(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.9f);
        }
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
