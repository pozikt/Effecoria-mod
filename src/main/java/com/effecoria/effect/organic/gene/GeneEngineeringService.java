package com.effecoria.effect.organic.gene;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.AABB;

/** Apply / tick / clear gene grafts on living hosts. */
public final class GeneEngineeringService {
    private static final ResourceLocation SPRINT_SPEED = EffecoriaMod.id("gene_sprint_speed");
    private static final ResourceLocation SPRINT_JUMP = EffecoriaMod.id("gene_sprint_jump");
    private static final ResourceLocation KERATIN_ARMOR = EffecoriaMod.id("gene_keratin_armor");
    private static final ResourceLocation KERATIN_SPEED = EffecoriaMod.id("gene_keratin_speed");
    private static final ResourceLocation ORKANUMN_TOUGH = EffecoriaMod.id("gene_orkanumn_tough");
    private static final ResourceLocation ORKANUMN_HP = EffecoriaMod.id("gene_orkanumn_hp");
    private static final ResourceLocation ORKANUMN_SPEED = EffecoriaMod.id("gene_orkanumn_speed");
    private static final ResourceLocation TOXIN_ATK = EffecoriaMod.id("gene_toxin_atk");
    private static final ResourceLocation TOXIN_HP = EffecoriaMod.id("gene_toxin_hp");
    private static final ResourceLocation BONE_ATK = EffecoriaMod.id("gene_bone_atk");
    private static final ResourceLocation MUSCLE_ATK = EffecoriaMod.id("gene_muscle_atk");
    private static final ResourceLocation MUSCLE_JUMP = EffecoriaMod.id("gene_muscle_jump");
    private static final ResourceLocation WING_GRAVITY = EffecoriaMod.id("gene_wing_gravity");
    private static final ResourceLocation WING_FALL_DMG = EffecoriaMod.id("gene_wing_fall_dmg");
    private static final ResourceLocation WING_SAFE_FALL = EffecoriaMod.id("gene_wing_safe_fall");
    private static final ResourceLocation LIMB_REACH = EffecoriaMod.id("gene_limb_reach");
    private static final ResourceLocation LIMB_BLOCK_REACH = EffecoriaMod.id("gene_limb_block_reach");
    private static final ResourceLocation LIMB_ATK_SPD = EffecoriaMod.id("gene_limb_atk_spd");
    private static final ResourceLocation BEAST_SPEED = EffecoriaMod.id("gene_beast_speed");
    private static final ResourceLocation BEAST_ATK = EffecoriaMod.id("gene_beast_atk");
    private static final ResourceLocation SWIM = EffecoriaMod.id("gene_gill_swim");

    private static final float PHI_HEART_PSI = 40f;
    private static final float PHI_HEART_PHI = 0.25f;

    private GeneEngineeringService() {}

    public static GeneProfile get(LivingEntity entity) {
        return entity.getData(ModAttachments.GENE_PROFILE.get());
    }

    public static void set(LivingEntity entity, GeneProfile profile) {
        entity.setData(ModAttachments.GENE_PROFILE.get(), profile);
    }

    public static float applyPsiCost(Iterable<GeneMod> mods) {
        float sum = 8f;
        for (GeneMod mod : mods) {
            sum += mod.applyPsiCost();
        }
        return sum;
    }

    public static boolean applyFromEngineer(
            ServerPlayer engineer, LivingEntity host, List<String> modIds, double maxRange) {
        if (host == null || !host.isAlive()) {
            return false;
        }
        if (engineer.distanceToSqr(host) > maxRange * maxRange) {
            return false;
        }
        PlayerPsiData data = PsiHelper.get(engineer);
        if (data.school() != MagicSchool.ORGANIC) {
            return false;
        }
        float mastery = BreathingService.referenceRatio(data.breathingMastery());
        EnumSet<GeneMod> chosen = EnumSet.noneOf(GeneMod.class);
        for (String id : modIds) {
            GeneMod.byId(id).ifPresent(chosen::add);
        }
        int slots = GeneMod.maxSlots(mastery);
        if (chosen.size() > slots) {
            return false;
        }
        if (!GeneMod.compatible(chosen)) {
            return false;
        }
        for (GeneMod mod : chosen) {
            if (mastery + 1.0e-4f < mod.minMastery()) {
                return false;
            }
            if (mod.playerOnly() && !(host instanceof Player)) {
                return false;
            }
        }
        float cost = applyPsiCost(chosen);
        if (!CreativeGodMode.isActive(engineer) && data.currentPsi() < cost) {
            return false;
        }
        if (!CreativeGodMode.isActive(engineer)) {
            data.setCurrentPsi(data.currentPsi() - cost);
            PsiHelper.set(engineer, data);
            engineer.syncData(ModAttachments.PSI.get());
        }

        GeneProfile profile = get(host);
        clearAttributeMods(host);
        clearChannelBonuses(host, profile);
        profile.setMods(chosen, engineer.getUUID(), host.level().getGameTime());
        set(host, profile);
        applyAttributeMods(host, profile);
        applyChannelBonuses(host, profile);
        host.level().playSound(
                null,
                host.blockPosition(),
                net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.7f,
                1.35f);
        return true;
    }

    public static void clearFromEngineer(ServerPlayer engineer, LivingEntity host, double maxRange) {
        if (host == null || engineer.distanceToSqr(host) > maxRange * maxRange) {
            return;
        }
        GeneProfile profile = get(host);
        clearAttributeMods(host);
        clearChannelBonuses(host, profile);
        profile.clear();
        set(host, profile);
    }

    public static void reapplyOnLoad(LivingEntity host) {
        GeneProfile profile = get(host);
        if (profile.isEmpty()) {
            return;
        }
        clearAttributeMods(host);
        applyAttributeMods(host, profile);
        // Channel bonuses already in PlayerPsiData NBT if previously applied — only re-sync attributes.
    }

    public static void tickLiving(LivingEntity host) {
        if (host.level().isClientSide() || !host.isAlive()) {
            return;
        }
        GeneProfile profile = get(host);
        if (profile.isEmpty()) {
            return;
        }
        if (host.tickCount % 100 == 0) {
            applyAttributeMods(host, profile);
        }
        if (profile.has(GeneMod.HYPER_REGEN) && host.tickCount % 20 == 0) {
            tickMetabolicHeal(host, profile, 1.0f);
        }
        if (profile.has(GeneMod.LIMB_REGEN) && host.tickCount % 20 == 0 && host.getHealth() < host.getMaxHealth() * 0.7f) {
            tickMetabolicHeal(host, profile, 2.0f);
        }
        if (profile.has(GeneMod.KEEN_EYES) && host.tickCount % 40 == 0) {
            tickKeenEyes(host);
        }
        if (profile.has(GeneMod.ECHO_SENSE) && host.tickCount % 40 == 0) {
            tickEchoSense(host);
        }
        if (profile.has(GeneMod.GILL_BUDS) && host.tickCount % 20 == 0) {
            tickGillBuds(host);
        }
        if (profile.has(GeneMod.KERATIN_PLATES) && host instanceof Player player && host.tickCount % 60 == 0) {
            player.causeFoodExhaustion(0.35f);
        }
        if (profile.has(GeneMod.SPRINT_LIMBS) && host instanceof Player player && player.isSprinting()
                && host.tickCount % 20 == 0) {
            player.causeFoodExhaustion(0.25f);
        }
        if (profile.has(GeneMod.MUSCLE_HYPERTROPHY) && host instanceof Player player && host.tickCount % 40 == 0) {
            player.causeFoodExhaustion(0.45f);
            if (player.getFoodData().getFoodLevel() <= 4) {
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
            }
        }
        if (profile.has(GeneMod.MEMBRANE_WINGS) && host.tickCount % 20 == 0) {
            tickWings(host);
        }
        if (profile.has(GeneMod.EXTRA_LIMBS) && host.tickCount % 160 == 0) {
            // Brain lag — brief chaotic control.
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, true, false, true));
            if (host.getRandom().nextBoolean()) {
                BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false, true));
            }
        }
        if (profile.has(GeneMod.BEAST_MORPH) && host.tickCount % 20 == 0) {
            tickBeastMorph(host);
        }
        if (profile.has(GeneMod.PHI_HEART) && host instanceof ServerPlayer player && host.tickCount % 40 == 0) {
            // Overcharge risk — entropy rises faster while the second Orkanum runs.
            PlayerPsiData psi = PsiHelper.get(player);
            psi.setEntropyB(psi.entropyB() + 0.015f);
            PsiHelper.set(player, psi);
        }
        if (profile.has(GeneMod.CELL_IMMORTAL) && host.tickCount % 80 == 0) {
            tickCellImmortal(host, profile);
        }
        if (profile.has(GeneMod.SYMBIOTE_COLONY) && host.tickCount % 40 == 0) {
            tickSymbiote(host);
        }
    }

    /** Host was hit — reactive toxin / bone self-bleed risk. */
    public static void onHostHurt(LivingEntity host, @Nullable LivingEntity attacker) {
        if (host.level().isClientSide()) {
            return;
        }
        GeneProfile profile = get(host);
        if (profile.has(GeneMod.TOXIN_GLANDS) && attacker != null && attacker.isAlive()) {
            BreathDebuffs.apply(attacker, new MobEffectInstance(MobEffects.POISON, 70, 0));
        }
        if (profile.has(GeneMod.BONE_WEAPONS) && host.getRandom().nextFloat() < 0.18f) {
            // Improper folding — self bleed.
            host.hurt(host.damageSources().magic(), 1.0f);
        }
    }

    /** Host is the attacker — bone edge / venom delivery. */
    public static void onHostAttack(LivingEntity host, LivingEntity target, LivingIncomingDamageBridge bridge) {
        if (host.level().isClientSide() || !target.isAlive()) {
            return;
        }
        GeneProfile profile = get(host);
        if (profile.has(GeneMod.BONE_WEAPONS)) {
            bridge.addBonusDamage(2.0f);
            if (host.getRandom().nextFloat() < 0.35f) {
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
        }
        if (profile.has(GeneMod.TOXIN_GLANDS)) {
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.POISON, 80, 0));
            // Self-poison risk on botched synthesis.
            if (host.getRandom().nextFloat() < 0.08f) {
                BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.POISON, 40, 0));
            }
        }
    }

    /** Tiny bridge so events can request bonus damage without circular imports. */
    @FunctionalInterface
    public interface LivingIncomingDamageBridge {
        void addBonusDamage(float amount);
    }

    private static void tickMetabolicHeal(LivingEntity host, GeneProfile profile, float healAmount) {
        float max = host.getMaxHealth();
        if (max <= 0f || host.getHealth() >= max - 0.05f) {
            return;
        }
        float before = host.getHealth();
        host.heal(healAmount);
        float healed = host.getHealth() - before;
        if (healed <= 0.01f) {
            return;
        }
        profile.addRegenAccrued(healed / max);
        while (profile.regenAccruedFraction() >= 0.25f) {
            profile.consumeRegenThreshold();
            if (host instanceof Player player) {
                FoodData food = player.getFoodData();
                int next = Math.max(0, food.getFoodLevel() / 2);
                food.setFoodLevel(next);
                food.setSaturation(food.getSaturationLevel() * 0.5f);
            } else {
                BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
        }
        set(host, profile);
    }

    private static void tickKeenEyes(LivingEntity host) {
        float brightness = host.level().getMaxLocalRawBrightness(host.blockPosition());
        if (brightness <= 7) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        } else if (brightness >= 13) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false, true));
            if (host.getRandom().nextFloat() < 0.25f) {
                BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, true, false, true));
            }
        }
    }

    private static void tickEchoSense(LivingEntity host) {
        if (!(host.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = host.getBoundingBox().inflate(12.0);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (other == host) {
                continue;
            }
            BreathDebuffs.apply(other, new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false, true));
        }
        BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, true, false, true));
        if (level.getMaxLocalRawBrightness(host.blockPosition()) <= 2 && host.getRandom().nextFloat() < 0.15f) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.CONFUSION, 40, 0, true, false, true));
        }
    }

    private static void tickGillBuds(LivingEntity host) {
        if (host.isInWaterOrRain()) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, true, false, true));
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 50, 0, true, false, true));
        } else {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, true));
            // Folding risk — brief air hunger.
            if (host.getRandom().nextFloat() < 0.05f) {
                host.setAirSupply(Math.max(-20, host.getAirSupply() - 40));
            }
        }
    }

    private static void tickWings(LivingEntity host) {
        BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, true));
        if (!host.onGround()) {
            if (host instanceof Player player) {
                player.causeFoodExhaustion(0.6f);
                PlayerPsiData psi = player instanceof ServerPlayer sp ? PsiHelper.get(sp) : null;
                if (psi != null && !CreativeGodMode.isActive(player)) {
                    psi.setCurrentPsi(Math.max(0f, psi.currentPsi() - 0.35f));
                    PsiHelper.set((ServerPlayer) player, psi);
                }
            } else {
                BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.HUNGER, 40, 1, true, false, true));
            }
        }
    }

    private static void tickBeastMorph(LivingEntity host) {
        BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false, true));
        if (host instanceof Player player) {
            player.causeFoodExhaustion(0.5f);
        }
        if (host.getRandom().nextFloat() < 0.08f) {
            // Animal mind slips through.
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.BLINDNESS, 35, 0, true, false, true));
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.CONFUSION, 45, 0, true, false, true));
        }
    }

    private static void tickCellImmortal(LivingEntity host, GeneProfile profile) {
        if (host.getHealth() < host.getMaxHealth()) {
            host.heal(0.5f);
            profile.addMutationCycle();
            set(host, profile);
        }
        int cycles = profile.mutationCycles();
        if (cycles >= 5 && host.tickCount % 200 == 0) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WITHER, 60, 0, true, false, true));
        }
        if (cycles >= 7 && host.tickCount % 200 == 0) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.POISON, 80, 0, true, false, true));
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, true, false, true));
        }
    }

    private static void tickSymbiote(LivingEntity host) {
        if (host instanceof ServerPlayer player) {
            PlayerPsiData psi = PsiHelper.get(player);
            psi.setCurrentPsi(Math.min(psi.maxPsi(), psi.currentPsi() + 1.2f));
            PsiHelper.set(player, psi);
            FoodData food = player.getFoodData();
            if (food.getFoodLevel() >= 8 && player.getRandom().nextFloat() < 0.35f) {
                // Symbionts feed from Φ — spare a little hunger.
                food.setFoodLevel(Math.min(20, food.getFoodLevel() + 1));
            }
            if (food.getFoodLevel() <= 3) {
                // Colony turns on the host.
                player.hurt(player.damageSources().magic(), 1.5f);
            }
        } else {
            host.heal(0.25f);
            if (host.getRandom().nextFloat() < 0.1f) {
                host.hurt(host.damageSources().magic(), 1.0f);
            }
        }
    }

    private static void applyChannelBonuses(LivingEntity host, GeneProfile profile) {
        if (!(host instanceof ServerPlayer player) || !profile.has(GeneMod.PHI_HEART)) {
            profile.setChannelBonuses(0f, 0f);
            set(host, profile);
            return;
        }
        PlayerPsiData psi = PsiHelper.get(player);
        psi.setMaxPsi(psi.maxPsi() + PHI_HEART_PSI);
        psi.setPhiMultiplier(psi.phiMultiplier() + PHI_HEART_PHI);
        PsiHelper.set(player, psi);
        player.syncData(ModAttachments.PSI.get());
        profile.setChannelBonuses(PHI_HEART_PSI, PHI_HEART_PHI);
        set(host, profile);
    }

    private static void clearChannelBonuses(LivingEntity host, GeneProfile profile) {
        if (!(host instanceof ServerPlayer player)) {
            profile.setChannelBonuses(0f, 0f);
            return;
        }
        if (profile.psiBonusApplied() <= 0f && profile.phiBonusApplied() <= 0f) {
            return;
        }
        PlayerPsiData psi = PsiHelper.get(player);
        psi.setMaxPsi(Math.max(10f, psi.maxPsi() - profile.psiBonusApplied()));
        psi.setPhiMultiplier(Math.max(0f, psi.phiMultiplier() - profile.phiBonusApplied()));
        PsiHelper.set(player, psi);
        player.syncData(ModAttachments.PSI.get());
        profile.setChannelBonuses(0f, 0f);
    }

    private static void applyAttributeMods(LivingEntity host, GeneProfile profile) {
        if (profile.has(GeneMod.SPRINT_LIMBS)) {
            addOrReplace(
                    host, Attributes.MOVEMENT_SPEED, SPRINT_SPEED, 0.22, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addOrReplace(host, Attributes.JUMP_STRENGTH, SPRINT_JUMP, 0.12, AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.KERATIN_PLATES)) {
            addOrReplace(host, Attributes.ARMOR, KERATIN_ARMOR, 5.0, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(
                    host, Attributes.MOVEMENT_SPEED, KERATIN_SPEED, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if (profile.has(GeneMod.ORKANUMN_WEAVE)) {
            addOrReplace(host, Attributes.MAX_HEALTH, ORKANUMN_HP, 4.0, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(host, Attributes.ARMOR_TOUGHNESS, ORKANUMN_TOUGH, 2.0, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(
                    host,
                    Attributes.MOVEMENT_SPEED,
                    ORKANUMN_SPEED,
                    -0.06,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            clampHealth(host);
        }
        if (profile.has(GeneMod.TOXIN_GLANDS)) {
            addOrReplace(host, Attributes.ATTACK_DAMAGE, TOXIN_ATK, -0.5, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(host, Attributes.MAX_HEALTH, TOXIN_HP, -2.0, AttributeModifier.Operation.ADD_VALUE);
            clampHealth(host);
        }
        if (profile.has(GeneMod.BONE_WEAPONS)) {
            addOrReplace(host, Attributes.ATTACK_DAMAGE, BONE_ATK, 2.0, AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.MUSCLE_HYPERTROPHY)) {
            addOrReplace(host, Attributes.ATTACK_DAMAGE, MUSCLE_ATK, 3.0, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(host, Attributes.JUMP_STRENGTH, MUSCLE_JUMP, 0.18, AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.MEMBRANE_WINGS)) {
            addOrReplace(host, Attributes.GRAVITY, WING_GRAVITY, -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addOrReplace(
                    host,
                    Attributes.FALL_DAMAGE_MULTIPLIER,
                    WING_FALL_DMG,
                    -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addOrReplace(host, Attributes.SAFE_FALL_DISTANCE, WING_SAFE_FALL, 12.0, AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.EXTRA_LIMBS)) {
            addOrReplace(host, Attributes.ATTACK_SPEED, LIMB_ATK_SPD, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addOrReplace(host, Attributes.ENTITY_INTERACTION_RANGE, LIMB_REACH, 0.75, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(
                    host, Attributes.BLOCK_INTERACTION_RANGE, LIMB_BLOCK_REACH, 0.5, AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.GILL_BUDS)) {
            addOrReplace(
                    host,
                    Attributes.WATER_MOVEMENT_EFFICIENCY,
                    SWIM,
                    0.5,
                    AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.BEAST_MORPH)) {
            addOrReplace(
                    host, Attributes.MOVEMENT_SPEED, BEAST_SPEED, 0.18, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addOrReplace(host, Attributes.ATTACK_DAMAGE, BEAST_ATK, 2.5, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    private static void clearAttributeMods(LivingEntity host) {
        remove(host, Attributes.MOVEMENT_SPEED, SPRINT_SPEED);
        remove(host, Attributes.JUMP_STRENGTH, SPRINT_JUMP);
        remove(host, Attributes.ARMOR, KERATIN_ARMOR);
        remove(host, Attributes.MOVEMENT_SPEED, KERATIN_SPEED);
        remove(host, Attributes.ARMOR_TOUGHNESS, ORKANUMN_TOUGH);
        remove(host, Attributes.MAX_HEALTH, ORKANUMN_HP);
        remove(host, Attributes.MOVEMENT_SPEED, ORKANUMN_SPEED);
        remove(host, Attributes.ATTACK_DAMAGE, TOXIN_ATK);
        remove(host, Attributes.MAX_HEALTH, TOXIN_HP);
        remove(host, Attributes.ATTACK_DAMAGE, BONE_ATK);
        remove(host, Attributes.ATTACK_DAMAGE, MUSCLE_ATK);
        remove(host, Attributes.JUMP_STRENGTH, MUSCLE_JUMP);
        remove(host, Attributes.GRAVITY, WING_GRAVITY);
        remove(host, Attributes.FALL_DAMAGE_MULTIPLIER, WING_FALL_DMG);
        remove(host, Attributes.SAFE_FALL_DISTANCE, WING_SAFE_FALL);
        remove(host, Attributes.ATTACK_SPEED, LIMB_ATK_SPD);
        remove(host, Attributes.ENTITY_INTERACTION_RANGE, LIMB_REACH);
        remove(host, Attributes.BLOCK_INTERACTION_RANGE, LIMB_BLOCK_REACH);
        remove(host, Attributes.WATER_MOVEMENT_EFFICIENCY, SWIM);
        remove(host, Attributes.MOVEMENT_SPEED, BEAST_SPEED);
        remove(host, Attributes.ATTACK_DAMAGE, BEAST_ATK);
    }

    private static void clampHealth(LivingEntity host) {
        if (host.getHealth() > host.getMaxHealth()) {
            host.setHealth(host.getMaxHealth());
        }
    }

    private static void addOrReplace(
            LivingEntity host,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation op) {
        AttributeInstance instance = host.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static void remove(
            LivingEntity host,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id) {
        AttributeInstance instance = host.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    public static List<String> unlockedIds(ServerPlayer engineer) {
        PlayerPsiData data = PsiHelper.get(engineer);
        float mastery = BreathingService.referenceRatio(data.breathingMastery());
        return GeneMod.unlockedFor(mastery).stream().map(GeneMod::id).toList();
    }

    public static int slotsFor(ServerPlayer engineer) {
        PlayerPsiData data = PsiHelper.get(engineer);
        return GeneMod.maxSlots(BreathingService.referenceRatio(data.breathingMastery()));
    }
}
