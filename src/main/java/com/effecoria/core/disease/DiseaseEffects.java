package com.effecoria.core.disease;

import com.effecoria.config.BalanceConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Gameplay modifiers derived from active Φ-diseases. */
public final class DiseaseEffects {
    private DiseaseEffects() {}

    public static float orkanumMultiplier(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        float mult = 1f;
        if (profile.orkanumnScar()) {
            mult *= BalanceConfig.DISEASE_SCAR_ORKANUM_MULT.get().floatValue();
        }
        DiseaseInstance atrophy = profile.get(PhiDisease.ORKANUMN_ATROPHY);
        if (atrophy != null) {
            float[] stages = {
                BalanceConfig.DISEASE_ATROPHY_STAGE1.get().floatValue(),
                BalanceConfig.DISEASE_ATROPHY_STAGE2.get().floatValue(),
                BalanceConfig.DISEASE_ATROPHY_STAGE3.get().floatValue()
            };
            mult *= stages[Mth.clamp(atrophy.stage(), 1, 3) - 1];
        }
        DiseaseInstance barren = profile.get(PhiDisease.MAGE_BARRENNESS);
        if (barren != null) {
            mult *= 0.05f;
        }
        DiseaseInstance fever = profile.get(PhiDisease.CRYSTAL_FEVER);
        if (fever != null) {
            // Oscillating bursts — mild average penalty with stage.
            mult *= fever.stage() >= 2 ? 0.85f : 0.95f;
        }
        DiseaseInstance cancer = profile.get(PhiDisease.ESSENTOCYTOSIS);
        if (cancer != null) {
            // Surge / crash: alternate every ~10s of active ticks.
            boolean surge = ((cancer.ticksActive() / 200) % 2) == 0;
            mult *= surge ? (1.15f + 0.1f * cancer.stage()) : (0.55f - 0.08f * cancer.stage());
        }
        DiseaseInstance dust = profile.get(PhiDisease.DUST_LUNG);
        if (dust != null) {
            mult *= 1f - 0.08f * dust.stage();
        }
        DiseaseInstance curse = profile.get(PhiDisease.CURSE_ROT);
        if (curse != null) {
            mult *= 1f - 0.12f * curse.stage();
        }
        return Mth.clamp(mult, 0.02f, 2.5f);
    }

    public static float castFailChance(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        float chance = 0f;
        DiseaseInstance burn = profile.get(PhiDisease.ESSENCE_BURN);
        if (burn != null) {
            chance += 0.08f * burn.stage();
        }
        DiseaseInstance dissonance = profile.get(PhiDisease.SOUL_DISSONANCE);
        if (dissonance != null) {
            chance += 0.12f * dissonance.stage();
        }
        DiseaseInstance echo = profile.get(PhiDisease.GHOST_ECHO);
        if (echo != null && echo.echoHostile()) {
            chance += 0.1f;
        }
        DiseaseInstance omega = profile.get(PhiDisease.OMEGA_SICKNESS);
        if (omega != null && omega.stage() >= 3) {
            chance += 0.15f;
        }
        return Mth.clamp(chance, 0f, 0.85f);
    }

    public static float castCostMultiplier(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        float mult = 1f;
        DiseaseInstance burn = profile.get(PhiDisease.ESSENCE_BURN);
        if (burn != null) {
            mult += 0.15f * burn.stage();
        }
        DiseaseInstance atrophy = profile.get(PhiDisease.ORKANUMN_ATROPHY);
        if (atrophy != null) {
            mult += 0.2f * atrophy.stage();
        }
        DiseaseInstance dissonance = profile.get(PhiDisease.SOUL_DISSONANCE);
        if (dissonance != null) {
            mult += 0.1f * dissonance.stage();
        }
        return Mth.clamp(mult, 1f, 3f);
    }

    public static float entropyGainMultiplier(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        float mult = 1f;
        DiseaseInstance omega = profile.get(PhiDisease.OMEGA_SICKNESS);
        if (omega != null) {
            mult += 0.25f * omega.stage();
        }
        DiseaseInstance rot = profile.get(PhiDisease.OMEGA_ROT);
        if (rot != null) {
            mult += 0.15f * rot.stage();
        }
        return mult;
    }

    public static float spellPowerMultiplier(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        DiseaseInstance cancer = profile.get(PhiDisease.ESSENTOCYTOSIS);
        if (cancer != null) {
            boolean surge = ((cancer.ticksActive() / 200) % 2) == 0;
            return surge ? 1.2f + 0.15f * cancer.stage() : 0.6f;
        }
        DiseaseInstance fever = profile.get(PhiDisease.CRYSTAL_FEVER);
        if (fever != null) {
            boolean spike = ((fever.ticksActive() / 100) % 2) == 0;
            return spike ? 1.15f : 0.8f;
        }
        return 1f;
    }

    public static boolean blocksHealing(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        if (profile.has(PhiDisease.OMEGA_ROT)) {
            return true;
        }
        return stageOf(profile, PhiDisease.OMEGA_SICKNESS) >= 3;
    }

    public static boolean blocksInitiation(Player player) {
        return DiseaseService.get(player).has(PhiDisease.MAGE_BARRENNESS);
    }

    public static boolean suppressesMagic(Player player) {
        DiseaseProfile profile = DiseaseService.get(player);
        DiseaseInstance atrophy = profile.get(PhiDisease.ORKANUMN_ATROPHY);
        return atrophy != null && atrophy.stage() >= 3;
    }

    public static float airBiologyPenalty(Player player) {
        DiseaseInstance dust = DiseaseService.get(player).get(PhiDisease.DUST_LUNG);
        if (dust == null) {
            return 1f;
        }
        return 1f - 0.12f * dust.stage();
    }

    private static int stageOf(DiseaseProfile profile, PhiDisease disease) {
        DiseaseInstance inst = profile.get(disease);
        return inst == null ? 0 : inst.stage();
    }

    /** Apply periodic status from active diseases (vanilla effects as presentation). */
    public static void applyPresentation(ServerPlayer player, DiseaseProfile profile) {
        for (var entry : profile.diseases().entrySet()) {
            PhiDisease disease = entry.getKey();
            int stage = entry.getValue().stage();
            switch (disease) {
                case ESSENCE_BURN -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WEAKNESS, 40, Math.min(2, stage - 1), false, false, true));
                    if (stage >= 2) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                    }
                }
                case OMEGA_SICKNESS -> {
                    if (stage >= 1) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.DARKNESS, 40, 0, false, false, true));
                    }
                    if (stage >= 2) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.CONFUSION, 60, 0, false, false, true));
                    }
                }
                case SOUL_DISSONANCE -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.CONFUSION, 40, Math.min(1, stage / 2), false, false, true));
                case DUST_LUNG -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.HUNGER, 40, 0, false, false, true));
                    if (stage >= 2) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                    }
                }
                case OMEGA_ROT -> {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WITHER, 40, Math.min(1, stage - 1), false, false, true));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            ModMobEffectsProxy.omegaWound(), 40, 0, false, false, true));
                }
                case CRYSTAL_FEVER -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 40, 0, false, false, true));
                case ESSENTOCYTOSIS -> {
                    if (((entry.getValue().ticksActive() / 200) % 2) != 0) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 1, false, false, true));
                    }
                }
                case CURSE_ROT -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.UNLUCK, 40, stage - 1, false, false, true));
                default -> {
                }
            }
        }
    }

    /** Avoid circular import of ModMobEffects in effects package — thin proxy. */
    private static final class ModMobEffectsProxy {
        static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> omegaWound() {
            return com.effecoria.content.ModMobEffects.omegaWound();
        }
    }
}
