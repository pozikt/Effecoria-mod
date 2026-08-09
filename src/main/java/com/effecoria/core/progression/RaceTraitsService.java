package com.effecoria.core.progression;

import com.effecoria.EffecoriaMod;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModItems;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Race passives — attributes on assign, tick/cast hooks elsewhere.
 */
public final class RaceTraitsService {
    private static final ResourceLocation ORC_HP = EffecoriaMod.id("race_orc_hp");
    private static final ResourceLocation DWARF_TOUGH = EffecoriaMod.id("race_dwarf_tough");
    private static final ResourceLocation DWARF_MINING = EffecoriaMod.id("race_dwarf_mining");
    private static final ResourceLocation HARPY_FALL = EffecoriaMod.id("race_harpy_fall");
    private static final ResourceLocation HARPY_SAFE_FALL = EffecoriaMod.id("race_harpy_safe_fall");

    private RaceTraitsService() {}

    public static void applyOnAssign(ServerPlayer player, PlayerPsiData data, PlayerRace race) {
        clearAttributes(player);
        switch (race) {
            case ORC -> addOrReplace(
                    player, Attributes.MAX_HEALTH, ORC_HP, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case DWARF -> {
                addOrReplace(
                        player, Attributes.ARMOR_TOUGHNESS, DWARF_TOUGH, 1.0, AttributeModifier.Operation.ADD_VALUE);
                addOrReplace(
                        player,
                        Attributes.BLOCK_BREAK_SPEED,
                        DWARF_MINING,
                        0.12,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            }
            case HARPY -> {
                addOrReplace(
                        player,
                        Attributes.FALL_DAMAGE_MULTIPLIER,
                        HARPY_FALL,
                        -0.5,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                addOrReplace(
                        player, Attributes.SAFE_FALL_DISTANCE, HARPY_SAFE_FALL, 4.0, AttributeModifier.Operation.ADD_VALUE);
            }
            case LONVER -> {
                float bonus = BalanceConfig.RACE_LONVER_MAX_PSI_BONUS.get().floatValue();
                if (bonus > 0f && data.raceMaxPsiBonus() <= 0f) {
                    data.setMaxPsi(data.maxPsi() + bonus);
                    data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + bonus * 0.5f));
                    data.setRaceMaxPsiBonus(bonus);
                }
            }
            default -> {
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void clear(ServerPlayer player, PlayerRace previous) {
        clearAttributes(player);
        if (previous == PlayerRace.LONVER) {
            PlayerPsiData data = PsiHelper.get(player);
            float bonus = data.raceMaxPsiBonus();
            if (bonus > 0f) {
                data.setMaxPsi(Math.max(10f, data.maxPsi() - bonus));
                data.setCurrentPsi(Math.min(data.currentPsi(), data.maxPsi()));
                data.setRaceMaxPsiBonus(0f);
                PsiHelper.set(player, data);
            }
        }
    }

    public static void reapplyAttributes(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        data.race().ifPresent(race -> applyOnAssign(player, data, race));
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
    }

    private static void clearAttributes(ServerPlayer player) {
        remove(player, Attributes.MAX_HEALTH, ORC_HP);
        remove(player, Attributes.ARMOR_TOUGHNESS, DWARF_TOUGH);
        remove(player, Attributes.BLOCK_BREAK_SPEED, DWARF_MINING);
        remove(player, Attributes.FALL_DAMAGE_MULTIPLIER, HARPY_FALL);
        remove(player, Attributes.SAFE_FALL_DISTANCE, HARPY_SAFE_FALL);
    }

    public static float spellCostMultiplier(Player player, MagicSchool school) {
        return PsiHelper.get(player).race().map(race -> switch (race) {
            case ELF -> 0.92f;
            case DWARF -> school == MagicSchool.SEALS ? 0.90f : 1f;
            default -> 1f;
        }).orElse(1f);
    }

    public static float spellPowerMultiplier(Player player, MagicSchool school) {
        return PsiHelper.get(player).race().map(race -> {
            if (race != PlayerRace.DRYAD) {
                return 1f;
            }
            float mult = school == MagicSchool.ORGANIC ? 1.12f : 0.95f;
            if (inForest(player)) {
                mult *= school == MagicSchool.ORGANIC ? 1.05f : 1f;
            }
            return mult;
        }).orElse(1f);
    }

    public static float entropyGainMultiplier(Player player) {
        return PsiHelper.get(player).race().map(race -> race == PlayerRace.ORC ? 0.90f : 1f).orElse(1f);
    }

    public static float breathingMasteryGainMultiplier(Player player) {
        return PsiHelper.get(player).race().map(race -> race == PlayerRace.HUMAN ? 1.05f : 1f).orElse(1f);
    }

    public static float regenMultiplier(Player player) {
        PlayerPsiData data = PsiHelper.get(player);
        return data.race().map(race -> {
            float mult = 1f;
            if (race == PlayerRace.VAMPIRE) {
                mult *= 0.5f;
            }
            if (race == PlayerRace.LONVER) {
                PhiSample phi = PhiFieldService.sample(player.level(), player.position(), player);
                if (!phi.isInfinite() && phi.effectiveValue() < 0.45f) {
                    mult *= 1.15f;
                }
            }
            return mult;
        }).orElse(1f);
    }

    public static float exhaustionDecayMultiplier(Player player) {
        return PsiHelper.get(player).race().map(race -> race == PlayerRace.LONVER ? 0.85f : 1f).orElse(1f);
    }

    public static float exhaustionGainMultiplier(Player player) {
        return PsiHelper.get(player).race().map(race -> race == PlayerRace.ORC ? 0.90f : 1f).orElse(1f);
    }

    public static long phiSenseDurationBonusTicks(Player player) {
        return PsiHelper.get(player).race().map(race -> race == PlayerRace.ELF ? 100L : 0L).orElse(0L);
    }

    /** Server tick passives (every player tick from RaceEvents). */
    public static void tick(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        PlayerRace race = data.race().orElse(null);
        if (race == null) {
            return;
        }
        Level level = player.level();
        switch (race) {
            case VARANAGI -> {
                if (player.onGround()
                        && !player.isSprinting()
                        && player.getDeltaMovement().horizontalDistanceSqr() < 0.0001
                        && player.tickCount % 40 == 0
                        && player.getHealth() < player.getMaxHealth()) {
                    player.heal(0.5f);
                }
            }
            case DRYAD -> {
                if (inForest(player) && player.tickCount % 80 == 0) {
                    player.getFoodData().addExhaustion(-0.15f);
                }
            }
            case VAMPIRE -> tickVampireSun(player);
            default -> {
            }
        }
    }

    private static void tickVampireSun(ServerPlayer player) {
        if (player.tickCount % 40 != 0) {
            return;
        }
        Level level = player.level();
        if (!level.isDay() || level.isRainingAt(player.blockPosition())) {
            return;
        }
        if (!level.canSeeSky(player.blockPosition().above())) {
            return;
        }
        ItemStack helmet = player.getInventory().getArmor(3);
        if (!helmet.isEmpty() && helmet.getItem() != Items.AIR) {
            return;
        }
        player.hurt(player.damageSources().onFire(), 1.0f);
        player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 40));
    }

    public static boolean tryDrinkBlood(ServerPlayer player, ItemStack stack) {
        PlayerPsiData data = PsiHelper.get(player);
        if (data.race().orElse(null) != PlayerRace.VAMPIRE) {
            return false;
        }
        float restore;
        float bioBump;
        if (stack.is(ModItems.MAGE_BLOOD_VIAL.get()) || stack.is(ModItems.WYVERN_BLOOD_VIAL.get())) {
            restore = 28f;
            bioBump = 0.04f;
        } else if (stack.is(ModItems.BLOOD_VIAL.get())) {
            restore = 14f;
            bioBump = 0.02f;
        } else if (stack.is(ModItems.OMEGA_BLOOD_VIAL.get())) {
            restore = 8f;
            bioBump = -0.02f;
            data.setEntropyB(data.entropyB() + 0.08f);
        } else {
            return false;
        }
        data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + restore));
        data.setBiologyQ(Math.max(0.05f, Math.min(1.2f, data.biologyQ() + bioBump)));
        stack.shrink(1);
        if (!player.getAbilities().instabuild) {
            ItemStack empty = new ItemStack(ModItems.BLOOD_VIAL_EMPTY.get());
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.race.vampire_drink", (int) restore),
                true);
        return true;
    }

    public static void onSpellCastStarted(ServerPlayer player) {
        if (PsiHelper.get(player).race().orElse(null) == PlayerRace.VARANAGI) {
            player.getFoodData().addExhaustion(-0.08f);
        }
    }

    private static boolean inForest(Player player) {
        return player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_FOREST)
                || player.level().getBiome(player.blockPosition()).is(BiomeTags.IS_JUNGLE);
    }

    private static void addOrReplace(
            ServerPlayer player,
            Holder<Attribute> attribute,
            ResourceLocation id,
            double amount,
            AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
