package com.effecoria.effect.mental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.SpellCombat;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MentalEffects {
    private MentalEffects() {}

    private static final List<TagKey<Structure>> LOCUS_STRUCTURE_TAGS = List.of(
            StructureTags.VILLAGE,
            StructureTags.MINESHAFT,
            StructureTags.OCEAN_RUIN,
            StructureTags.SHIPWRECK,
            StructureTags.RUINED_PORTAL,
            StructureTags.EYE_OF_ENDER_LOCATED,
            StructureTags.ON_TREASURE_MAPS,
            StructureTags.DOLPHIN_LOCATED);

    public static void mindBolt(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target == null) {
            finishHit(caster.serverLevel(), aim, HitFx.SHARD);
            return;
        }
        int ticks = scaledTicks(effect, power, "confusion_ticks", 80);
        if (!gateAfflict(caster, target, ticks, true)) {
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        finishHit(caster.serverLevel(), target.position(), HitFx.SHARD);
    }

    public static void psychicScream(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 6f;
        float damage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 5f;
        int confuseTicks = scaledTicks(effect, power, "confusion_ticks", 80);
        float scaled = damage * (power / 50f);
        AABB box = caster.getBoundingBox().inflate(radius);
        boolean echoed = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            if (!gateAfflict(caster, entity, confuseTicks, false)) {
                continue;
            }
            entity.hurt(SpellCombat.magic(caster), scaled);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.CONFUSION, confuseTicks, 1));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WEAKNESS, confuseTicks / 2, 0));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.BLINDNESS, Math.min(40, confuseTicks / 2), 0));
            spawnSynapse(level, entity.position().add(0, 1, 0));
            // Echo if target will is stronger (high HP humanoid).
            if (!echoed
                    && MentalityService.of(entity) == MentalityService.Kind.HUMANOID
                    && entity.getMaxHealth() >= caster.getMaxHealth() * 1.25f) {
                caster.hurt(SpellCombat.magic(caster), scaled * 0.45f);
                BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 40, 0));
                echoed = true;
                caster.displayClientMessage(Component.translatable("message.effecoria.mental.echo_strike"), true);
            }
        }
        spawnSynapse(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.55f, 1.4f);
    }

    public static void thoughtLance(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target == null) {
            finishHit(caster.serverLevel(), aim, HitFx.SHARD);
            return;
        }
        ServerLevel level = caster.serverLevel();
        int slowTicks = scaledTicks(effect, power, "slow_ticks", 60);
        int confuseTicks = Math.max(slowTicks, scaledTicks(effect, power, "confusion_ticks", 100));
        if (!gateAfflict(caster, target, confuseTicks, true)) {
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, confuseTicks, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 2));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.BLINDNESS, Math.min(40, slowTicks / 2), 0));
        finishHit(level, target.position(), HitFx.SHARD);
        level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 0.5f);
    }

    public static void neuralLock(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = scaledTicks(effect, power, "duration_ticks", 100);
        if (!gateAfflict(caster, target, duration, true)) {
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, 1));
        finishHit(caster.serverLevel(), target.position(), HitFx.SYNAPSE);
    }

    public static void telekineticCrush(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target == null) {
            finishHit(caster.serverLevel(), aim, HitFx.FORCE);
            return;
        }
        ServerLevel level = caster.serverLevel();
        float lift = effect.params().has("lift_force") ? effect.params().get("lift_force").getAsFloat() : 0.65f;
        int fogTicks = scaledTicks(effect, power, "confusion_ticks", 90);
        if (!gateAfflict(caster, target, fogTicks, true)) {
            return;
        }
        target.setDeltaMovement(target.getDeltaMovement().add(0, lift * (power / 50f), 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, fogTicks, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, fogTicks, 1));
        target.hurtMarked = true;
        finishHit(level, target.position(), HitFx.FORCE);
    }

    public static void massConfusion(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 8f;
        int ticks = scaledTicks(effect, power, "confusion_ticks", 120);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            if (!gateAfflict(caster, entity, ticks, false)) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.CONFUSION, ticks, 1));
            spawnFog(level, entity.position().add(0, 1, 0));
        }
        spawnFog(level, caster.position().add(0, 1, 0));
    }

    public static void psychicBarrier(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        MentalityService.setShield(caster, caster.level().getGameTime() + duration);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        // Holding the shield tires the channel.
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.HUNGER, Math.min(80, duration / 2), 0, true, false, true));
        caster.displayClientMessage(Component.translatable("message.effecoria.mental.shield_on"), true);
        spawnWard(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void mindProbe(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        long now = caster.level().getGameTime();
        if (MentalityService.hasShield(target, now)) {
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.shield_blocks"), true);
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 40, 0));
            spawnWard(caster.serverLevel(), target.position().add(0, 1, 0));
            return;
        }
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 160;
        if (!gateAfflict(caster, target, duration, true)) {
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration / 2, 0));
        // Trauma risk for the reader.
        if (caster.getRandom().nextFloat() < 0.18f) {
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.probe_trauma"), true);
        }

        // Force a precise dump into chat (not only the action bar).
        deliverProbeConfession(caster, target);
        finishHit(caster.serverLevel(), target.position(), HitFx.SENSE);
    }

    private static void deliverProbeConfession(ServerPlayer caster, LivingEntity target) {
        var typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        MentalityService.Kind kind = MentalityService.of(target);
        caster.sendSystemMessage(Component.translatable(
                "message.effecoria.mental.probe_header",
                target.getDisplayName(),
                Component.translatable(
                        "message.effecoria.mental.kind."
                                + kind.name().toLowerCase(java.util.Locale.ROOT))));
        caster.sendSystemMessage(Component.translatable(
                "message.effecoria.mental.probe_identity",
                typeId != null ? typeId.toString() : "?",
                String.format("%.2f", target.getHealth()),
                String.format("%.2f", target.getMaxHealth()),
                String.format("%.0f", target.getYRot()),
                target.blockPosition().getX(),
                target.blockPosition().getY(),
                target.blockPosition().getZ()));

        if (target instanceof Mob mob) {
            LivingEntity aggro = mob.getTarget();
            LivingEntity lastHurt = mob.getLastHurtByMob();
            caster.sendSystemMessage(Component.translatable(
                    "message.effecoria.mental.probe_mob_ai",
                    aggro != null ? aggro.getDisplayName() : Component.literal("—"),
                    lastHurt != null ? lastHurt.getDisplayName() : Component.literal("—"),
                    mob.isAggressive()));
        }

        if (target instanceof net.minecraft.world.entity.player.Player victim) {
            var inv = victim.getInventory();
            StringBuilder bag = new StringBuilder();
            int listed = 0;
            for (int i = 0; i < inv.getContainerSize() && listed < 12; i++) {
                var stack = inv.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (listed > 0) {
                    bag.append(", ");
                }
                bag.append(stack.getHoverName().getString()).append("×").append(stack.getCount());
                listed++;
            }
            caster.sendSystemMessage(Component.translatable(
                    "message.effecoria.mental.probe_player_bag",
                    bag.isEmpty() ? "—" : bag.toString()));
        }

        String gear = String.join(
                " · ",
                slotLabel(target, net.minecraft.world.entity.EquipmentSlot.MAINHAND),
                slotLabel(target, net.minecraft.world.entity.EquipmentSlot.OFFHAND),
                slotLabel(target, net.minecraft.world.entity.EquipmentSlot.HEAD),
                slotLabel(target, net.minecraft.world.entity.EquipmentSlot.CHEST),
                slotLabel(target, net.minecraft.world.entity.EquipmentSlot.LEGS),
                slotLabel(target, net.minecraft.world.entity.EquipmentSlot.FEET));
        caster.sendSystemMessage(Component.translatable("message.effecoria.mental.probe_gear", gear));

        StringBuilder effects = new StringBuilder();
        int n = 0;
        for (var inst : target.getActiveEffects()) {
            if (n > 0) {
                effects.append(", ");
            }
            effects
                    .append(Component.translatable(inst.getDescriptionId()).getString())
                    .append(" ")
                    .append(inst.getAmplifier() + 1)
                    .append(" (")
                    .append(inst.getDuration() / 20)
                    .append("s)");
            n++;
            if (n >= 8) {
                break;
            }
        }
        caster.sendSystemMessage(Component.translatable(
                "message.effecoria.mental.probe_effects", effects.isEmpty() ? "—" : effects.toString()));

        // Compact action-bar summary for combat glance.
        caster.displayClientMessage(
                Component.translatable(
                        "message.effecoria.mental.mind_probe_deep",
                        target.getDisplayName(),
                        String.format("%.1f", target.getHealth()),
                        String.format("%.1f", target.getMaxHealth()),
                        slotLabel(target, net.minecraft.world.entity.EquipmentSlot.MAINHAND)),
                true);
    }

    private static String slotLabel(LivingEntity target, net.minecraft.world.entity.EquipmentSlot slot) {
        var stack = target.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return "—";
        }
        return stack.getHoverName().getString() + (stack.getCount() > 1 ? "×" + stack.getCount() : "");
    }

    public static void locusEcho(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int searchRadius = effect.params().has("search_radius") ? effect.params().get("search_radius").getAsInt() : 96;
        int offsetMin = effect.params().has("offset_min") ? effect.params().get("offset_min").getAsInt() : 200;
        int offsetMax = effect.params().has("offset_max") ? effect.params().get("offset_max").getAsInt() : 300;
        int displayTicks = effect.params().has("display_ticks") ? effect.params().get("display_ticks").getAsInt() : 100;
        if (offsetMax < offsetMin) {
            int swap = offsetMin;
            offsetMin = offsetMax;
            offsetMax = swap;
        }

        // Soft afflict gate — reading a place-memory from the target's mind.
        if (!gateAfflict(caster, target, Math.max(40, displayTicks / 2), true)) {
            return;
        }

        ServerLevel level = caster.serverLevel();
        BlockPos origin = target.blockPosition();
        List<TagKey<Structure>> pool = new ArrayList<>(LOCUS_STRUCTURE_TAGS);
        Collections.shuffle(pool, new Random(level.getRandom().nextLong()));

        BlockPos found = null;
        for (TagKey<Structure> tag : pool) {
            BlockPos hit = level.findNearestMapStructure(tag, origin, searchRadius, false);
            if (hit != null) {
                found = hit;
                break;
            }
        }
        if (found == null) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.mental.locus_echo_empty"),
                    true);
            spawnSense(level, target.position().add(0, 1, 0));
            return;
        }

        var random = level.getRandom();
        int dist = offsetMin + random.nextInt(Math.max(1, offsetMax - offsetMin + 1));
        double angle = random.nextDouble() * Math.PI * 2.0;
        int blurX = found.getX() + (int) Math.round(Math.cos(angle) * dist);
        int blurZ = found.getZ() + (int) Math.round(Math.sin(angle) * dist);
        int blurY = found.getY() + random.nextInt(41) - 20;

        PacketDistributor.sendToPlayer(
                caster, new ModNetworking.BlurredLocusPayload(blurX, blurY, blurZ, displayTicks));
        finishHit(level, target.position(), HitFx.SENSE);
        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.55f,
                1.55f);
    }

    public static void synapticOverload(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target == null) {
            finishHit(caster.serverLevel(), aim, HitFx.SYNAPSE);
            return;
        }
        int confuseTicks = scaledTicks(effect, power, "confusion_ticks", 100);
        if (!gateAfflict(caster, target, confuseTicks, true)) {
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, confuseTicks, 2));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, confuseTicks / 2, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, confuseTicks, 1));
        finishHit(caster.serverLevel(), target.position(), HitFx.SYNAPSE);
    }

    public static void psychicDrain(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target == null) {
            finishHit(caster.serverLevel(), aim, HitFx.DRAIN);
            return;
        }
        int fogTicks = scaledTicks(effect, power, "fatigue_ticks", 120);
        if (!gateAfflict(caster, target, fogTicks, true)) {
            return;
        }
        float psiGain = effect.params().has("psi_gain")
                ? effect.params().get("psi_gain").getAsFloat()
                : 4f;
        psiGain *= power / 50f;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, fogTicks, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, fogTicks / 2, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, fogTicks / 2, 0));
        PlayerPsiData data = PsiHelper.get(caster);
        data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + psiGain));
        PsiHelper.set(caster, data);
        finishHit(caster.serverLevel(), target.position(), HitFx.DRAIN);
    }

    public static void mentalFortress(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 400;
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.REGENERATION, duration, 0, false, false, true));
        spawnWard(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void thoughtBomb(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 5f;
        int ticks = scaledTicks(effect, power, "confusion_ticks", 140);
        Vec3 center = target != null ? target.position() : aim;
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.position().distanceToSqr(center) > radius * radius) {
                continue;
            }
            if (!gateAfflict(caster, entity, ticks, false)) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.CONFUSION, ticks, 2));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WEAKNESS, ticks / 2, 1));
            spawnBomb(level, entity.position().add(0, 1, 0));
        }
        spawnBomb(level, center.add(0, 1, 0));
        level.playSound(null, BlockPos.containing(center), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    public static void psychicStorm(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 7f;
        int pulses = effect.params().has("pulses") ? effect.params().get("pulses").getAsInt() : 3;
        int baseTicks = scaledTicks(effect, power, "confusion_ticks", 60);
        AABB box = caster.getBoundingBox().inflate(radius);
        for (int p = 0; p < pulses; p++) {
            int amp = Math.min(2, p);
            int ticks = baseTicks + p * 20;
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (entity == caster) {
                    continue;
                }
                if (entity.distanceToSqr(caster) > radius * radius) {
                    continue;
                }
                if (!gateAfflict(caster, entity, ticks, false)) {
                    continue;
                }
                BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.CONFUSION, ticks, amp));
                BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.WEAKNESS, ticks / 2, 0));
            }
        }
        spawnFog(level, caster.position().add(0, 1, 0));
        spawnSynapse(level, caster.position().add(0, 1.2, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.7f, 0.8f);
    }

    public static void psychicAmplify(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 400;
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration);
        PsiHelper.set(caster, data);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration / 2, 0, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration / 2, 0, false, false, true));
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.phi_sense_active"), true);
        spawnSense(caster.serverLevel(), caster.position().add(0, 1, 0));
    }

    public static void omegaMind(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        int senseTicks = effect.params().has("phi_sense_ticks") ? effect.params().get("phi_sense_ticks").getAsInt() : 300;
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + senseTicks);
        PsiHelper.set(caster, data);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 2, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
        spawnSense(caster.serverLevel(), caster.position().add(0, 1.2, 0));
        spawnWard(caster.serverLevel(), caster.position().add(0, 1, 0));
        caster.serverLevel().playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    public static void mindTerror(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.TERROR, 140);
    }

    public static void cliffUrge(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.CLIFF, 160);
    }

    public static void drownUrge(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.DROWN, 180);
    }

    public static void psychicFrenzy(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.FRENZY, 120);
    }

    public static void massHysteria(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 10f;
        int duration = scaledTicks(effect, power, "duration_ticks", 160);
        MentalCompulsionService.Type[] pool = {
            MentalCompulsionService.Type.TERROR,
            MentalCompulsionService.Type.CLIFF,
            MentalCompulsionService.Type.FRENZY,
            MentalCompulsionService.Type.DEPRESS,
            MentalCompulsionService.Type.DROWN
        };
        AABB box = caster.getBoundingBox().inflate(radius);
        int hit = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
            if (mob.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            MentalCompulsionService.Type type = pool[mob.getRandom().nextInt(pool.length)];
            LivingEntity fearSource = null;
            if (type == MentalCompulsionService.Type.TERROR) {
                fearSource = pickRandomFearSource(level, mob, radius * 1.5f, caster);
            }
            if (!MentalCompulsionService.apply(caster, mob, type, duration, fearSource)) {
                continue;
            }
            BreathDebuffs.apply(mob, new MobEffectInstance(MobEffects.CONFUSION, Math.min(duration, 120), 1));
            spawnFear(level, mob.position().add(0, 1, 0));
            hit++;
        }
        spawnFear(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.45f, 1.35f);
        if (hit == 0) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.mental.compel_invalid"),
                    true);
        }
    }

    /** I.1 Empathic scan — surface emotions (not true telepathy). */
    public static void empathicScan(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 200;
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 16f;
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration);
        PsiHelper.set(caster, data);

        ServerLevel level = caster.serverLevel();
        AABB box = caster.getBoundingBox().inflate(radius);
        int read = 0;
        boolean overloaded = false;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            long now = level.getGameTime();
            if (MentalityService.hasShield(entity, now)) {
                caster.displayClientMessage(
                        Component.translatable("message.effecoria.mental.empathy_shielded", entity.getDisplayName()),
                        true);
                continue;
            }
            Component emotion = surfaceEmotion(entity);
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.GLOWING, Math.min(80, duration / 2), 0, false, false, true));
            caster.displayClientMessage(
                    Component.translatable("message.effecoria.mental.empathy_read", entity.getDisplayName(), emotion),
                    true);
            read++;
            if (entity.getHealth() / Math.max(1f, entity.getMaxHealth()) < 0.25f
                    || entity.hasEffect(MobEffects.WITHER)) {
                overloaded = true;
            }
        }
        if (overloaded) {
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 50, 0));
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.empathy_overload"), true);
        } else if (read == 0) {
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.empathy_none"), true);
        }
        spawnSense(level, caster.position().add(0, caster.getEyeHeight() * 0.5, 0));
    }

    private static Component surfaceEmotion(LivingEntity entity) {
        float ratio = entity.getHealth() / Math.max(1f, entity.getMaxHealth());
        if (ratio < 0.3f || entity.isOnFire() || entity.hasEffect(MobEffects.WITHER)) {
            return Component.translatable("message.effecoria.mental.emotion.fear");
        }
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            return Component.translatable("message.effecoria.mental.emotion.anger");
        }
        if (entity.hasEffect(MobEffects.REGENERATION) || entity.hasEffect(MobEffects.DAMAGE_BOOST)) {
            return Component.translatable("message.effecoria.mental.emotion.joy");
        }
        if (entity.isInvisible() || entity.hasEffect(MobEffects.INVISIBILITY)) {
            return Component.translatable("message.effecoria.mental.emotion.lie");
        }
        return Component.translatable("message.effecoria.mental.emotion.calm");
    }

    /** I.2 Ψ-whisper — soft leave suggestion. */
    public static void psiWhisper(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = scaledTicks(effect, power, "duration_ticks", 80);
        // Reject if urging a player to give items — Stage-1 backlash.
        if (target instanceof ServerPlayer) {
            PlayerPsiData data = PsiHelper.get(caster);
            data.setEntropyB(data.entropyB() + 0.04f);
            PsiHelper.set(caster, data);
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 30, 0));
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.whisper_reject"), true);
            return;
        }
        if (!(target instanceof Mob)) {
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.compel_invalid"), true);
            return;
        }
        if (!MentalCompulsionService.apply(caster, target, MentalCompulsionService.Type.WHISPER, duration)) {
            MentalityService.notifyFail(caster, target);
            PlayerPsiData data = PsiHelper.get(caster);
            data.setEntropyB(data.entropyB() + 0.03f);
            PsiHelper.set(caster, data);
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, Math.min(40, duration), 0));
        caster.displayClientMessage(
                Component.translatable("message.effecoria.mental.whisper_ok", target.getDisplayName()), true);
        finishHit(caster.serverLevel(), target.position(), HitFx.FOG);
    }

    /** II.5 Sensory illusion — victim body stays; soul walks a private mirage. */
    public static void mindIllusion(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        int duration = scaledTicks(effect, power, "duration_ticks", 160);
        ServerLevel level = caster.serverLevel();
        if (target != null && gateAfflict(caster, target, duration, false)) {
            if (target instanceof ServerPlayer victim) {
                float pulse = effect.params().has("mirage_pulse")
                        ? effect.params().get("mirage_pulse").getAsFloat()
                        : 2.5f * (0.85f + power / 120f);
                MirageWorldService.start(victim, caster, duration, pulse);
            } else {
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.BLINDNESS, Math.min(60, duration / 2), 0));
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, duration / 2, 0));
            }
        }
        Vec3 aim = caster.getEyePosition().add(caster.getLookAngle().normalize().scale(4));
        spawnFog(level, aim);
        spawnSense(level, caster.position().add(0, 1, 0));
        caster.displayClientMessage(Component.translatable("message.effecoria.mental.illusion_on"), true);
        level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.7f, 1.1f);
    }

    /** III.7 Mind control — puppet. */
    public static void mindDominate(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        applyCompulsion(caster, effect, power, target, MentalCompulsionService.Type.DOMINATE, 120);
        int duration = scaledTicks(effect, power, "duration_ticks", 120);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, false, true));
    }

    /** III.8 False memory — forget aggro / brief amnesia. */
    public static void falseMemory(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        int duration = scaledTicks(effect, power, "duration_ticks", 160);
        if (!gateAfflict(caster, target, duration, true)) {
            return;
        }
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getNavigation().stop();
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, duration, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.BLINDNESS, Math.min(40, duration / 3), 0));
        if (caster.getRandom().nextFloat() < 0.12f) {
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, duration * 2, 2));
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.memory_scar"), true);
        }
        caster.displayClientMessage(
                Component.translatable("message.effecoria.mental.memory_ok", target.getDisplayName()), true);
        finishHit(caster.serverLevel(), target.position(), HitFx.FOG);
    }

    /** III.9 Dream lock — night stronger. */
    public static void dreamLock(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        boolean night = !caster.level().isDay();
        int duration = scaledTicks(effect, power, "duration_ticks", night ? 140 : 70);
        if (!gateAfflict(caster, target, duration, true)) {
            return;
        }
        int amp = night ? 5 : 2;
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amp));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, duration, night ? 1 : 0));
        if (target instanceof Mob mob) {
            MentalCompulsionService.apply(caster, mob, MentalCompulsionService.Type.DEPRESS, duration);
        }
        PlayerPsiData data = PsiHelper.get(caster);
        data.setEntropyB(data.entropyB() + (night ? 0.08f : 0.04f));
        PsiHelper.set(caster, data);
        finishHit(caster.serverLevel(), target.position(), HitFx.FEAR);
        caster.displayClientMessage(
                Component.translatable(night ? "message.effecoria.mental.dream_night" : "message.effecoria.mental.dream_day"),
                true);
    }

    /** IV.10 Hive mind — link nearby living allies/mobs. */
    public static void hiveMind(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 10f;
        int duration = scaledTicks(effect, power, "duration_ticks", 160);
        ServerLevel level = caster.serverLevel();
        AABB box = caster.getBoundingBox().inflate(radius);
        int linked = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            if (MentalityService.of(entity) == MentalityService.Kind.CONSTRUCT) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
            linked++;
        }
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, true));
        if (linked >= 4 && caster.getRandom().nextFloat() < 0.2f) {
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.CONFUSION, 80, 1));
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.hive_blur"), true);
        }
        caster.displayClientMessage(Component.translatable("message.effecoria.mental.hive_ok", linked), true);
        spawnSense(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** IV.11 Ψ-echo — aggro decoy copy. */
    public static void psiEcho(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int life = scaledTicks(effect, power, "duration_ticks", 100);
        ServerLevel level = caster.serverLevel();
        Vec3 at = caster.position().add(caster.getLookAngle().normalize().scale(2.5));
        ArmorStand echo = new ArmorStand(level, at.x, at.y, at.z);
        echo.setInvisible(true);
        echo.setNoGravity(true);
        echo.setInvulnerable(false);
        echo.setCustomName(Component.translatable("entity.effecoria.psi_echo", caster.getDisplayName()));
        echo.setCustomNameVisible(true);
        echo.setGlowingTag(true);
        echo.getPersistentData().putLong("effecoria:psi_echo_until", level.getGameTime() + life);
        echo.getPersistentData().putUUID("effecoria:psi_echo_owner", caster.getUUID());
        level.addFreshEntity(echo);
        AABB box = echo.getBoundingBox().inflate(16);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
            if (mob.getTarget() == caster || mob.distanceToSqr(caster) < 64) {
                mob.setTarget(echo);
            }
        }
        caster.displayClientMessage(Component.translatable("message.effecoria.mental.echo_spawn"), true);
        spawnSense(level, at.add(0, 1, 0));
    }

    /** IV.12 Total veil — area illusion. */
    public static void totalVeil(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float radius = effect.params().has("radius") ? effect.params().get("radius").getAsFloat() : 12f;
        int duration = scaledTicks(effect, power, "duration_ticks", 120);
        ServerLevel level = caster.serverLevel();
        AABB box = caster.getBoundingBox().inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == caster) {
                continue;
            }
            if (entity.distanceToSqr(caster) > radius * radius) {
                continue;
            }
            if (!gateAfflict(caster, entity, duration, false)) {
                continue;
            }
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
            BreathDebuffs.apply(entity, new MobEffectInstance(MobEffects.CONFUSION, duration, 1));
        }
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
        if (caster.getRandom().nextFloat() < 0.15f) {
            BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            caster.displayClientMessage(Component.translatable("message.effecoria.mental.veil_trap"), true);
        }
        PlayerPsiData data = PsiHelper.get(caster);
        data.setEntropyB(data.entropyB() + 0.12f);
        data.setCurrentPsi(Math.max(0f, data.currentPsi() * 0.5f));
        PsiHelper.set(caster, data);
        spawnFog(level, caster.position().add(0, 1, 0));
        spawnSense(level, caster.position().add(0, 1.2, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.5f, 0.7f);
        caster.displayClientMessage(Component.translatable("message.effecoria.mental.veil_on"), true);
    }

    private static LivingEntity pickRandomFearSource(
            ServerLevel level, Mob victim, float searchRadius, LivingEntity fallback) {
        List<LivingEntity> candidates = new ArrayList<>();
        AABB box = victim.getBoundingBox().inflate(searchRadius);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (other != victim) {
                candidates.add(other);
            }
        }
        if (candidates.isEmpty()) {
            return fallback;
        }
        return candidates.get(victim.getRandom().nextInt(candidates.size()));
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
        int duration = scaledTicks(effect, power, "duration_ticks", defaultTicks);
        if (!(target instanceof Mob) || target instanceof net.minecraft.world.entity.player.Player) {
            caster.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.effecoria.mental.compel_invalid"),
                    true);
            return;
        }
        if (!MentalCompulsionService.apply(caster, target, type, duration)) {
            MentalityService.notifyFail(caster, target);
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, Math.min(duration, 100), 0));
        finishHit(caster.serverLevel(), target.position(), HitFx.FEAR);
        caster.serverLevel().playSound(
                null, target.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.55f, 1.5f);
    }

    public static void telekineticSurge(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        float force = effect.params().has("force") ? effect.params().get("force").getAsFloat() : 3.5f;
        if (!gateAfflict(caster, target, 40, true)) {
            return;
        }
        Vec3 look = caster.getLookAngle().normalize();
        double strength = force * (power / 50f);
        target.setDeltaMovement(target.getDeltaMovement().add(look.scale(strength)));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, 40, 0));
        target.hurtMarked = true;
        finishHit(caster.serverLevel(), target.position(), HitFx.FORCE);
    }

    private static boolean gateAfflict(
            ServerPlayer caster, LivingEntity target, int durationTicks, boolean notifyOnFail) {
        if (MentalityService.tryAfflict(caster, target, durationTicks)) {
            return true;
        }
        if (notifyOnFail) {
            MentalityService.notifyFail(caster, target);
        }
        return false;
    }

    private static int scaledTicks(SpellEffectEntry effect, float power, String key, int fallback) {
        int base = effect.params().has(key) ? effect.params().get(key).getAsInt() : fallback;
        return Math.round(base * (0.85f + power / 120f));
    }

    private enum HitFx {
        FOG,
        SHARD,
        FORCE,
        SYNAPSE,
        WARD,
        FEAR,
        SENSE,
        DRAIN,
        BOMB
    }

    private static void finishHit(ServerLevel level, Vec3 pos, HitFx fx) {
        Vec3 at = pos.add(0, 1, 0);
        switch (fx) {
            case SHARD -> spawnShard(level, at);
            case FORCE -> spawnForce(level, at);
            case SYNAPSE -> spawnSynapse(level, at);
            case WARD -> spawnWard(level, at);
            case FEAR -> spawnFear(level, at);
            case SENSE -> spawnSense(level, at);
            case DRAIN -> spawnDrain(level, at);
            case BOMB -> spawnBomb(level, at);
            case FOG -> spawnFog(level, at);
        }
        level.playSound(
                null,
                net.minecraft.core.BlockPos.containing(pos),
                SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS,
                0.55f,
                1.4f);
    }

    /** Default / confusion fog. */
    public static void spawnFog(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y + 0.5, pos.z, 10, 0.3, 0.2, 0.3, 0.005);
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y, pos.z, 6, 0.2, 0.15, 0.2, 0.003);
    }

    public static void spawnShard(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_SHARD.get(), pos.x, pos.y, pos.z, 12, 0.25, 0.3, 0.25, 0.04);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y, pos.z, 4, 0.12, 0.15, 0.12, 0.02);
    }

    public static void spawnForce(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_FORCE.get(), pos.x, pos.y, pos.z, 14, 0.35, 0.25, 0.35, 0.05);
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y, pos.z, 4, 0.2, 0.15, 0.2, 0.003);
    }

    public static void spawnSynapse(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_SYNAPSE.get(), pos.x, pos.y, pos.z, 16, 0.3, 0.35, 0.3, 0.06);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y, pos.z, 5, 0.15, 0.2, 0.15, 0.03);
    }

    public static void spawnWard(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_WARD.get(), pos.x, pos.y, pos.z, 12, 0.4, 0.35, 0.4, 0.01);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y, pos.z, 4, 0.2, 0.2, 0.2, 0.02);
    }

    public static void spawnFear(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_FEAR.get(), pos.x, pos.y, pos.z, 12, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y, pos.z, 5, 0.2, 0.15, 0.2, 0.004);
    }

    public static void spawnSense(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_SENSE.get(), pos.x, pos.y, pos.z, 14, 0.35, 0.35, 0.35, 0.03);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y, pos.z, 8, 0.25, 0.25, 0.25, 0.02);
    }

    public static void spawnDrain(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_FORCE.get(), pos.x, pos.y, pos.z, 8, 0.2, 0.25, 0.2, 0.02);
        level.sendParticles(ModParticleTypes.MENTAL_FEAR.get(), pos.x, pos.y, pos.z, 6, 0.15, 0.2, 0.15, 0.015);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y, pos.z, 4, 0.1, 0.15, 0.1, 0.02);
    }

    public static void spawnBomb(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_SHARD.get(), pos.x, pos.y, pos.z, 10, 0.4, 0.35, 0.4, 0.05);
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y, pos.z, 10, 0.35, 0.3, 0.35, 0.006);
        level.sendParticles(ModParticleTypes.MENTAL_SYNAPSE.get(), pos.x, pos.y, pos.z, 6, 0.25, 0.25, 0.25, 0.04);
    }

    /** Legacy alias — fog + spark. */
    public static void spawnMindParticles(ServerLevel level, Vec3 pos) {
        spawnFog(level, pos);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), pos.x, pos.y - 0.2, pos.z, 4, 0.15, 0.2, 0.15, 0.02);
    }
}
