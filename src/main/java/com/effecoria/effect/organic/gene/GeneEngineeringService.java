package com.effecoria.effect.organic.gene;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.BreathDebuffs;
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

    /**
     * Rewrite host grafts. Returns false if validation fails (slots, mastery, psi, range).
     */
    public static boolean applyFromEngineer(
            ServerPlayer engineer, LivingEntity host, List<String> modIds, double maxRange) {
        if (host == null || !host.isAlive()) {
            return false;
        }
        if (engineer.distanceToSqr(host) > maxRange * maxRange) {
            return false;
        }
        PlayerPsiData data = PsiHelper.get(engineer);
        if (data.school() != com.effecoria.core.magic.MagicSchool.ORGANIC) {
            return false;
        }
        float mastery = BreathingService.referenceRatio(data.breathingMastery());
        EnumSet<GeneMod> chosen = EnumSet.noneOf(GeneMod.class);
        for (String id : modIds) {
            GeneMod.byId(id).ifPresent(chosen::add);
        }
        if (chosen.size() > GeneMod.MAX_SLOTS) {
            return false;
        }
        if (!GeneMod.compatible(chosen)) {
            return false;
        }
        for (GeneMod mod : chosen) {
            if (mastery + 1.0e-4f < mod.minMastery()) {
                return false;
            }
        }
        float cost = applyPsiCost(chosen);
        if (!com.effecoria.core.phi.CreativeGodMode.isActive(engineer) && data.currentPsi() < cost) {
            return false;
        }
        if (!com.effecoria.core.phi.CreativeGodMode.isActive(engineer)) {
            data.setCurrentPsi(data.currentPsi() - cost);
            PsiHelper.set(engineer, data);
            engineer.syncData(ModAttachments.PSI.get());
        }

        GeneProfile profile = get(host);
        clearAttributeMods(host);
        profile.setMods(chosen, engineer.getUUID(), host.level().getGameTime());
        set(host, profile);
        applyAttributeMods(host, profile);
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
    }

    public static void tickLiving(LivingEntity host) {
        if (host.level().isClientSide() || !host.isAlive()) {
            return;
        }
        GeneProfile profile = get(host);
        if (profile.isEmpty()) {
            return;
        }
        // Re-assert attributes occasionally (other mods may wipe).
        if (host.tickCount % 100 == 0) {
            applyAttributeMods(host, profile);
        }
        if (profile.has(GeneMod.HYPER_REGEN) && host.tickCount % 20 == 0) {
            tickHyperRegen(host, profile);
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
    }

    public static void onHostHurt(LivingEntity host, @Nullable LivingEntity attacker) {
        if (attacker == null || !attacker.isAlive() || host.level().isClientSide()) {
            return;
        }
        GeneProfile profile = get(host);
        if (!profile.has(GeneMod.TOXIN_GLANDS)) {
            return;
        }
        BreathDebuffs.apply(attacker, new MobEffectInstance(MobEffects.POISON, 60, 0));
    }

    private static void tickHyperRegen(LivingEntity host, GeneProfile profile) {
        float max = host.getMaxHealth();
        if (max <= 0f || host.getHealth() >= max - 0.05f) {
            return;
        }
        float before = host.getHealth();
        // ~1 HP / sec — costly biology.
        host.heal(1.0f);
        float healed = host.getHealth() - before;
        if (healed <= 0.01f) {
            return;
        }
        profile.addRegenAccrued(healed / max);
        // Every 25% max-HP restored: players lose half current hunger; mobs get slowed.
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
        } else if (brightness >= 12) {
            // Day glare — remade eyes strain in hard light.
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false, true));
        }
    }

    private static void tickEchoSense(LivingEntity host) {
        if (!(host.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = host.getBoundingBox().inflate(10.0);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (other == host) {
                continue;
            }
            BreathDebuffs.apply(other, new MobEffectInstance(MobEffects.GLOWING, 45, 0, true, false, true));
        }
        // Sensory bandwidth cost: mild mining fatigue / attack slow for a beat.
        BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, true, false, true));
    }

    private static void tickGillBuds(LivingEntity host) {
        if (host.isInWaterOrRain()) {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false, true));
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true));
        } else {
            BreathDebuffs.apply(host, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false, true));
        }
    }

    private static void applyAttributeMods(LivingEntity host, GeneProfile profile) {
        if (profile.has(GeneMod.SPRINT_LIMBS)) {
            addOrReplace(
                    host, Attributes.MOVEMENT_SPEED, SPRINT_SPEED, 0.22, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addOrReplace(host, Attributes.JUMP_STRENGTH, SPRINT_JUMP, 0.12, AttributeModifier.Operation.ADD_VALUE);
        }
        if (profile.has(GeneMod.KERATIN_PLATES)) {
            addOrReplace(host, Attributes.ARMOR, KERATIN_ARMOR, 4.0, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(
                    host, Attributes.MOVEMENT_SPEED, KERATIN_SPEED, -0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
            if (host.getHealth() > host.getMaxHealth()) {
                host.setHealth(host.getMaxHealth());
            }
        }
        if (profile.has(GeneMod.TOXIN_GLANDS)) {
            addOrReplace(host, Attributes.ATTACK_DAMAGE, TOXIN_ATK, -1.0, AttributeModifier.Operation.ADD_VALUE);
            addOrReplace(host, Attributes.MAX_HEALTH, TOXIN_HP, -2.0, AttributeModifier.Operation.ADD_VALUE);
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

    public static boolean engineerCanUse(ServerPlayer engineer, GeneMod mod) {
        PlayerPsiData data = PsiHelper.get(engineer);
        float mastery = BreathingService.referenceRatio(data.breathingMastery());
        return mastery + 1.0e-4f >= mod.minMastery();
    }

    public static List<String> unlockedIds(ServerPlayer engineer) {
        PlayerPsiData data = PsiHelper.get(engineer);
        float mastery = BreathingService.referenceRatio(data.breathingMastery());
        return GeneMod.unlockedFor(mastery).stream().map(GeneMod::id).toList();
    }
}
