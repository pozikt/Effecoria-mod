package com.effecoria.effect.necromancy;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.DiceDamage;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.entity.DeathShadowEntity;
import com.google.gson.JsonObject;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NecromancyEffects {
    private NecromancyEffects() {}

    public static void boneChill(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        if (target != null) {
            float damage = DiceDamage.fromParams(effect.params(), power, 3f);
            int slowTicks = effect.params().has("slow_ticks") ? effect.params().get("slow_ticks").getAsInt() : 80;
            target.hurt(SpellCombat.wither(caster), damage);
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
            target.hurtMarked = true;
            finishHit(level, target, HitFx.BONE);
        } else {
            finishHit(level, aim.add(0, 0.2, 0), HitFx.BONE);
        }
    }

    public static void deathSense(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 16f;
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        float threshold = effect.params().has("health_fraction") ? effect.params().get("health_fraction").getAsFloat() : 0.45f;
        long freshTicks = effect.params().has("fresh_ticks") ? effect.params().get("fresh_ticks").getAsLong() : 20L * 60L * 30L;
        int dying = 0;
        int echoes = 0;
        int marks = 0;
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.getHealth() / entity.getMaxHealth() <= threshold) {
                BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
                dying++;
            }
            if (entity instanceof Player player) {
                var death = player.getData(ModAttachments.LAST_DEATH.get());
                if (death.present()) {
                    var site = death.site().orElse(null);
                    if (site != null
                            && site.dimension().equals(level.dimension())
                            && site.pos().distanceToSqr(caster.position()) <= radius * radius) {
                        echoes++;
                        long age = level.getGameTime() - site.gameTime();
                        String freshness = age <= freshTicks
                                ? Component.translatable("message.effecoria.necro.death_sense_fresh").getString()
                                : Component.translatable("message.effecoria.necro.death_sense_old").getString();
                        caster.sendSystemMessage(Component.translatable(
                                "message.effecoria.necro.death_sense_echo",
                                player.getName().getString(),
                                freshness,
                                (int) site.pos().x,
                                (int) site.pos().y,
                                (int) site.pos().z));
                    }
                }
            }
        }
        for (var stand : level.getEntitiesOfClass(
                net.minecraft.world.entity.decoration.ArmorStand.class,
                box,
                DeathMarkService::isWorldMark)) {
            marks++;
            BreathDebuffs.apply(stand, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
        }
        // Echo of death accumulates b faster.
        PlayerPsiData data = PsiHelper.get(caster);
        data.setEntropyB(data.entropyB() + 0.015f + 0.008f * echoes);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(
                Component.translatable("message.effecoria.necro.death_sense", dying, echoes, marks), true);
        spawnGrave(level, caster.position().add(0, 1, 0));
    }

    public static void graveWhisper(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        if (target == null) {
            finishHit(level, aim.add(0, 0.2, 0), HitFx.GRAVE);
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        // Ψ-imprint of last moments.
        if (target instanceof Player player) {
            var death = player.getData(ModAttachments.LAST_DEATH.get());
            if (death.present()) {
                var site = death.site().orElse(null);
                String cause = death.cause().isBlank()
                        ? Component.translatable("message.effecoria.necro.echo_unknown").getString()
                        : death.cause();
                caster.sendSystemMessage(Component.translatable(
                        "message.effecoria.necro.echo_replay",
                        player.getName().getString(),
                        cause,
                        site != null ? (int) site.pos().x : 0,
                        site != null ? (int) site.pos().y : 0,
                        site != null ? (int) site.pos().z : 0));
            } else {
                caster.displayClientMessage(Component.translatable("message.effecoria.necro.echo_none"), true);
            }
        } else {
            float frac = target.getHealth() / Math.max(1f, target.getMaxHealth());
            caster.sendSystemMessage(Component.translatable(
                    "message.effecoria.necro.echo_creature",
                    target.getName().getString(),
                    (int) (frac * 100)));
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        // Loop risk — mental assault on nearby.
        if (caster.getRandom().nextFloat() < 0.28f) {
            float radius = effect.params().has("loop_radius") ? effect.params().get("loop_radius").getAsFloat() : 5f;
            AABB box = target.getBoundingBox().inflate(radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                BreathDebuffs.apply(e, new MobEffectInstance(MobEffects.CONFUSION, duration / 2, 1));
                BreathDebuffs.apply(e, new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.echo_loop"), true);
            PlayerPsiData data = PsiHelper.get(caster);
            data.setEntropyB(data.entropyB() + 0.05f);
            PsiHelper.set(caster, data);
        }
        finishHit(level, target, HitFx.GRAVE);
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
            entity.hurt(SpellCombat.magic(caster), damage);
            entity.hurtMarked = true;
            healed += damage * healRatio;
            spawnSoul(level, entity.position().add(0, 1, 0));
        }
        if (healed > 0f) {
            caster.heal(healed);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 0.75f);
    }

    public static void boneArmor(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 240;
        int resist = effect.params().has("resistance_amplifier") ? effect.params().get("resistance_amplifier").getAsInt() : 0;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resist, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, false, true));
        spawnBone(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void lifeTap(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        if (target == null) {
            finishHit(level, aim.add(0, 0.2, 0), HitFx.SOUL);
            return;
        }
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        float healRatio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.75f;
        target.hurt(SpellCombat.magic(caster), damage);
        caster.heal(damage * healRatio);
        target.hurtMarked = true;
        finishHit(level, target, HitFx.SOUL);
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
            entity.hurt(SpellCombat.wither(caster), damage);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
            entity.hurtMarked = true;
            spawnWither(level, entity.position().add(0, 1, 0));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 1.1f);
    }

    public static void darkPact(ServerPlayer caster, SpellEffectEntry effect, float power) {
        // Ω-eldritch contract approximation — temporary ally + heavy b.
        int lifetime = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 240;
        int exhaust = effect.params().has("exhaustion_ticks") ? effect.params().get("exhaustion_ticks").getAsInt() : 160;
        boolean ok = NecroSummonService.spawnEldritchAlly(caster, lifetime);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, lifetime, 1, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, lifetime, 0, false, false, true));
        BreathDebuffs.applyAfter(
                caster, new MobEffectInstance(MobEffects.WEAKNESS, exhaust, 1, false, false, true), lifetime);
        if (ok) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.eldritch_bound"), true);
        }
        spawnSoul(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void soulShackle(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.interrogate_need"), true);
            return;
        }
        int rootTicks = effect.params().has("root_ticks") ? effect.params().get("root_ticks").getAsInt() : 100;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 4));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, rootTicks, 0));
        // Extract a "memory" — approximate interrogation.
        boolean forced = effect.params().has("forced") && effect.params().get("forced").getAsBoolean();
        boolean lies = !forced && caster.getRandom().nextFloat() < 0.35f;
        BlockPos tip = target.blockPosition().offset(
                caster.getRandom().nextInt(17) - 8,
                caster.getRandom().nextInt(5) - 1,
                caster.getRandom().nextInt(17) - 8);
        if (lies) {
            tip = tip.offset(32, 0, -24);
            caster.sendSystemMessage(Component.translatable(
                    "message.effecoria.necro.interrogate_lie", target.getName().getString()));
        } else if (target instanceof Player player) {
            var death = player.getData(ModAttachments.LAST_DEATH.get());
            if (death.present() && death.site().isPresent()) {
                tip = BlockPos.containing(death.site().get().pos());
            }
            caster.sendSystemMessage(Component.translatable(
                    "message.effecoria.necro.interrogate_truth",
                    target.getName().getString(),
                    tip.getX(),
                    tip.getY(),
                    tip.getZ()));
        } else {
            caster.sendSystemMessage(Component.translatable(
                    "message.effecoria.necro.interrogate_truth",
                    target.getName().getString(),
                    tip.getX(),
                    tip.getY(),
                    tip.getZ()));
        }
        if (forced || caster.getRandom().nextFloat() < 0.4f) {
            PlayerPsiData data = PsiHelper.get(caster);
            data.setEntropyB(data.entropyB() + 0.06f);
            PsiHelper.set(caster, data);
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.interrogate_pollute"), true);
        }
        finishHit(caster.serverLevel(), target, HitFx.BIND);
    }

    public static void phantomStep(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, true));
        spawnShade(caster.serverLevel(), caster.position().add(0, 1, 0));
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

    public static void deathMark(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        int duration = effect.params().has("duration_ticks")
                ? effect.params().get("duration_ticks").getAsInt()
                : DeathMarkService.LIVING_MARK_TICKS;
        DeathMarkService.applyMark(caster, target, duration);
    }

    public static void lichWard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        // Eternal guard — husk at aim point.
        Vec3 look = caster.getEyePosition().add(caster.getLookAngle().scale(3.5));
        BlockPos feet = BlockPos.containing(look);
        ServerLevel level = caster.serverLevel();
        while (feet.getY() > level.getMinBuildHeight() + 1
                && level.getBlockState(feet.below()).isAir()) {
            feet = feet.below();
        }
        Vec3 post = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        boolean ok = NecroSummonService.spawnEternalGuard(caster, post);
        if (ok) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.guard_raised"), true);
        }
        spawnShade(level, post.add(0, 1, 0));
    }

    public static void soulCataclysm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        // Ψ-surgery — fuse thralls into a doll.
        boolean ok = NecroSummonService.fuseIntoDoll(caster);
        if (ok) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.doll_forged"), true);
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
        }
        spawnSoul(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void deathCoil(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float burst = DiceDamage.fromParams(effect.params(), power, 8f);
        float spread = effect.params().has("spread_radius") ? effect.params().get("spread_radius").getAsFloat() : 4f;
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 60;
        Vec3 center = target != null ? target.position() : aim;
        if (target != null) {
            applyCoilHit(level, caster, target, burst, witherTicks);
        } else {
            finishHit(level, aim.add(0, 0.2, 0), HitFx.WITHER);
        }
        AABB box = new AABB(center, center).inflate(spread);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == target || entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > spread * spread) {
                continue;
            }
            applyCoilHit(level, caster, entity, burst * 0.5f, witherTicks / 2);
        }
    }

    public static void deathApotheosis(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 180;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.ABSORPTION, duration, 2, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, false, true));
        spawnGrave(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.35f, 1.4f);
    }

    public static void necroticBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        if (target == null) {
            finishHit(level, aim.add(0, 0.2, 0), HitFx.WITHER);
            return;
        }
        float damage = DiceDamage.fromParams(effect.params(), power, 5f);
        int witherTicks = effect.params().has("wither_ticks") ? effect.params().get("wither_ticks").getAsInt() : 40;
        target.hurt(SpellCombat.wither(caster), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        target.hurtMarked = true;
        finishHit(level, target, HitFx.WITHER);
    }

    public static void graveBind(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int rootTicks = effect.params().has("root_ticks") ? effect.params().get("root_ticks").getAsInt() : 120;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, rootTicks, 5));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, rootTicks, 2));
        finishHit(caster.serverLevel(), target, HitFx.BIND);
    }

    public static void curseOfFrailty(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 140;
        int weakAmp = effect.params().has("weakness_amplifier") ? effect.params().get("weakness_amplifier").getAsInt() : 1;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, weakAmp));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 0));
        finishHit(caster.serverLevel(), target, HitFx.SHADE);
    }

    public static void hauntingVisage(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 100;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.BLINDNESS, duration / 2, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.DARKNESS, duration / 2, 0));
        finishHit(caster.serverLevel(), target, HitFx.SHADE);
    }

    public static void corpseBurst(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float damage = DiceDamage.fromParams(effect.params(), power, 6f);
        Vec3 center = target != null ? target.position() : aim;
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.wither(caster), damage);
            entity.hurtMarked = true;
            spawnWither(level, entity.position().add(0, 1, 0));
        }
        spawnWither(level, center.add(0, 1, 0));
        level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 0.7f);
    }

    public static void boneVolley(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        if (target == null) {
            finishHit(level, aim.add(0, 0.2, 0), HitFx.BONE);
            return;
        }
        int hits = effect.params().has("hits") ? effect.params().get("hits").getAsInt() : 3;
        float perHit = DiceDamage.fromParams(effect.params(), power, 4f) / Math.max(1, hits);
        for (int i = 0; i < hits; i++) {
            target.hurt(SpellCombat.wither(caster), perHit);
        }
        target.hurtMarked = true;
        finishHit(level, target, HitFx.BONE);
    }

    public static void necroticAura(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 4f;
        float pulse = DiceDamage.fromParams(effect.params(), power, 3f);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            entity.hurt(SpellCombat.wither(caster), pulse);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WITHER, 40, 0));
            entity.hurtMarked = true;
            spawnWither(level, entity.position().add(0, 1, 0));
        }
        spawnWither(level, caster.position().add(0, 1, 0));
    }

    public static void soulAnchor(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 80;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
        target.hurtMarked = true;
        finishHit(caster.serverLevel(), target, HitFx.BIND);
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
        spawnGrave(level, origin.add(0, 1, 0));
        caster.teleportTo(best.x, best.y, best.z);
        caster.fallDistance = 0f;
        spawnGrave(level, best.add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 0.6f);
    }

    public static void soulReaper(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        // Forbidden call — steal from Rest: revive a recently dead player, or assault living.
        ServerLevel level = caster.serverLevel();
        PlayerPsiData data = PsiHelper.get(caster);
        data.setEntropyB(data.entropyB() + 0.18f);
        PsiHelper.set(caster, data);

        if (target instanceof ServerPlayer deadPlayer && !deadPlayer.isAlive()) {
            // Unreachable via living target ray — handled below via nearby ghosts of death sites.
        }

        // Prefer: revive a player who died recently in range (offline body not available — heal if low / respawn-like).
        ServerPlayer revive = null;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 10f;
        long fresh = effect.params().has("fresh_ticks") ? effect.params().get("fresh_ticks").getAsLong() : 20L * 60L * 5L;
        for (ServerPlayer p : level.players()) {
            if (p == caster || !p.isAlive()) {
                continue;
            }
            var death = p.getData(ModAttachments.LAST_DEATH.get());
            if (!death.present()) {
                continue;
            }
            var site = death.site().orElse(null);
            if (site == null || !site.dimension().equals(level.dimension())) {
                continue;
            }
            if (level.getGameTime() - site.gameTime() > fresh) {
                continue;
            }
            if (site.pos().distanceToSqr(caster.position()) > radius * radius) {
                continue;
            }
            revive = p;
            break;
        }

        if (revive != null) {
            // Pull from Rest — imperfect return.
            float roll = caster.getRandom().nextFloat();
            revive.heal(revive.getMaxHealth());
            if (roll < 0.33f) {
                // Without b — numb.
                BreathDebuffs.apply(revive, new MobEffectInstance(MobEffects.WEAKNESS, 400, 1));
                BreathDebuffs.apply(revive, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 400, 1));
                caster.sendSystemMessage(Component.translatable("message.effecoria.necro.rest_numb", revive.getName().getString()));
            } else if (roll < 0.66f) {
                // Wrong b — mad.
                BreathDebuffs.apply(revive, new MobEffectInstance(MobEffects.CONFUSION, 300, 1));
                BreathDebuffs.apply(revive, new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                BreathDebuffs.apply(revive, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
                caster.sendSystemMessage(Component.translatable("message.effecoria.necro.rest_mad", revive.getName().getString()));
            } else {
                caster.sendSystemMessage(Component.translatable("message.effecoria.necro.rest_ok", revive.getName().getString()));
            }
            spawnSoul(level, revive.position().add(0, 1, 0));
            level.playSound(null, revive.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4f, 0.5f);
            return;
        }

        if (target == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.rest_none"), true);
            finishHit(level, aim.add(0, 0.2, 0), HitFx.SOUL);
            return;
        }
        // Living target: brutal reaping as fallback.
        float damage = DiceDamage.fromParams(effect.params(), power, 9f);
        target.hurt(SpellCombat.wither(caster), damage);
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, 100, 1));
        target.hurtMarked = true;
        finishHit(level, target, HitFx.SOUL);
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
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, true, true));
        if (data.isLichAscensionActive(gameTime)) {
            float bonus = effect.params().has("phyl_boost") ? effect.params().get("phyl_boost").getAsFloat() : 0.12f;
            data.boostPhylacteryEfficiency(gameTime, bonus);
            PsiHelper.set(caster, data);
        }
        spawnSoul(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 0.7f);
    }

    public static void deathShadow(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (!(target instanceof Player subject) || !subject.isAlive()) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.death_shadow_not_player"), true);
            return;
        }
        if (subject == caster) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.death_shadow_self"), true);
            return;
        }
        ServerLevel level = caster.serverLevel();
        var death = subject.getData(ModAttachments.LAST_DEATH.get()).site();
        if (death.isEmpty()) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.death_shadow_none"), true);
            return;
        }
        PlayerLastDeath.DeathSite site = death.get();
        if (!site.dimension().equals(level.dimension())) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.death_shadow_dim"), true);
            return;
        }
        DeathShadowEntity.spawn(level, subject, site.pos());
        spawnShade(level, subject.position().add(0, 1, 0));
        level.playSound(null, subject.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8f, 0.55f);
    }

    public static void lichAscension(ServerPlayer caster, SpellEffectEntry effect, float power) {
        // Phylactery surrogate: totem or nether star in inventory.
        if (!hasPhylacterySurrogate(caster)) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.lich_need_phyl"), true);
            return;
        }
        ServerLevel level = caster.serverLevel();
        long gameTime = level.getGameTime();
        PlayerPsiData data = PsiHelper.get(caster);
        if (data.isLichAscensionActive(gameTime)) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.lich_already"), true);
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 240;
        float phyl = effect.params().has("phyl_efficiency") ? effect.params().get("phyl_efficiency").getAsFloat() : 0.85f;
        float storedQ = Math.max(0.05f, data.biologyQ());
        data.beginLichAscension(gameTime + duration, storedQ, phyl);
        data.setEntropyB(data.entropyB() + 0.1f);
        PsiHelper.set(caster, data);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false, true));
        caster.displayClientMessage(Component.translatable("message.effecoria.necro.lich_ascension"), true);
        spawnSoul(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 0.55f);
    }

    public static void raiseSkeleton(ServerPlayer caster, SpellEffectEntry effect, float power) {
        boolean overcharge = caster.getRandom().nextFloat() < 0.22f
                || (effect.params().has("force_chaos") && effect.params().get("force_chaos").getAsBoolean());
        // Higher power → more chaos risk (drunk skeleton).
        if (power > 70f && caster.getRandom().nextFloat() < 0.35f) {
            overcharge = true;
        }
        boolean ok = NecroSummonService.spawnBoneServant(caster, overcharge);
        if (ok) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.bone_servant"), true);
        }
        spawnBone(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void shadeSummon(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        boolean ok = NecroSummonService.spawnSpiritContract(caster, target);
        if (ok) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.spirit_bound"), true);
            PlayerPsiData data = PsiHelper.get(caster);
            data.setEntropyB(data.entropyB() + 0.04f);
            PsiHelper.set(caster, data);
        }
        spawnShade(caster.serverLevel(), caster.position().add(0, 1.2, 0));
    }

    public static void armyOfDead(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int count = effect.params().has("count") ? effect.params().get("count").getAsInt() : 4;
        count = Math.min(8, Math.max(2, count + (int) (power / 80f)));
        int raised = NecroSummonService.spawnArmy(caster, count);
        PlayerPsiData data = PsiHelper.get(caster);
        data.setEntropyB(data.entropyB() + 0.05f * raised);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(Component.translatable("message.effecoria.necro.army_raised", raised), true);
        spawnGrave(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel()
                .playSound(null, caster.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.35f, 0.8f);
    }

    private static boolean hasPhylacterySurrogate(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)
                    || stack.is(net.minecraft.world.item.Items.NETHER_STAR)
                    || stack.is(net.minecraft.world.item.Items.END_CRYSTAL)) {
                return true;
            }
        }
        return false;
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
            entity.hurt(SpellCombat.wither(skip), damage);
            entity.hurtMarked = true;
            spawnWither(level, entity.position().add(0, 1, 0));
        }
    }

    private static void applyCoilHit(ServerLevel level, ServerPlayer caster, LivingEntity entity, float damage, int witherTicks) {
        entity.hurt(SpellCombat.wither(caster), damage);
        BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        entity.hurtMarked = true;
        spawnWither(level, entity.position().add(0, 1, 0));
    }

    private enum HitFx {
        BONE,
        SOUL,
        WITHER,
        GRAVE,
        SHADE,
        BIND
    }

    private static void finishHit(ServerLevel level, LivingEntity target, HitFx fx) {
        finishHit(level, target.position().add(0, 1, 0), fx);
    }

    private static void finishHit(ServerLevel level, Vec3 at, HitFx fx) {
        spawnFx(level, at, fx);
        level.playSound(null, BlockPos.containing(at), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6f, 1.1f);
    }

    private static void spawnFx(ServerLevel level, Vec3 pos, HitFx fx) {
        switch (fx) {
            case BONE -> spawnBone(level, pos);
            case SOUL -> spawnSoul(level, pos);
            case WITHER -> spawnWither(level, pos);
            case GRAVE -> spawnGrave(level, pos);
            case SHADE -> spawnShade(level, pos);
            case BIND -> spawnBind(level, pos);
        }
    }

    public static void spawnBone(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_BONE.get(), pos.x, pos.y, pos.z, 10, 0.3, 0.35, 0.3, 0.02);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.4, pos.z, 5, 0.2, 0.3, 0.2, 0.008);
    }

    public static void spawnSoul(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_SOUL.get(), pos.x, pos.y, pos.z, 12, 0.28, 0.4, 0.28, 0.02);
        level.sendParticles(ModParticleTypes.NECRO_SHADOW.get(), pos.x, pos.y + 0.3, pos.z, 6, 0.2, 0.25, 0.2, 0.01);
    }

    public static void spawnWither(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_WITHER.get(), pos.x, pos.y, pos.z, 12, 0.3, 0.35, 0.3, 0.015);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.45, pos.z, 8, 0.25, 0.35, 0.25, 0.01);
    }

    public static void spawnGrave(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_GRAVE.get(), pos.x, pos.y, pos.z, 10, 0.3, 0.3, 0.3, 0.015);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.4, pos.z, 7, 0.22, 0.3, 0.22, 0.008);
    }

    public static void spawnShade(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_SHADE.get(), pos.x, pos.y, pos.z, 12, 0.28, 0.4, 0.28, 0.015);
        level.sendParticles(ModParticleTypes.NECRO_SOUL.get(), pos.x, pos.y + 0.35, pos.z, 5, 0.18, 0.25, 0.18, 0.01);
    }

    public static void spawnBind(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_BIND.get(), pos.x, pos.y, pos.z, 10, 0.28, 0.3, 0.28, 0.02);
        level.sendParticles(ModParticleTypes.NECRO_SOUL.get(), pos.x, pos.y + 0.3, pos.z, 5, 0.18, 0.22, 0.18, 0.01);
    }

    /** Generic necro burst — kept for field/summon helpers and legacy callers. */
    public static void spawnNecroParticles(ServerLevel level, Vec3 pos) {
        spawnShade(level, pos);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.25, 0.4, 0.25, 0.008);
    }
}
