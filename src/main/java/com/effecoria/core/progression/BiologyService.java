package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Vanilla body-state modifiers for Orkanum / biologyQ. */
public final class BiologyService {
    private BiologyService() {}

    /**
     * Multiplier applied on top of stored biologyQ and breathing bonus.
     * Hunger and air supply affect Ψ conversion efficiency.
     */
    public static float bodyFactor(Player player) {
        return hungerFactor(player) * airFactor(player);
    }

    /** Effective Orkanum used by regen / soft cast scaling (includes breathing bonus + body). */
    public static float effectiveOrkanum(Player player, PlayerPsiData data) {
        return data.effectiveBiologyQ() * bodyFactor(player);
    }

    /**
     * Soft spell-power multiplier from live Orkanum. Weight 0 keeps casts regen-only.
     * Hungry / drowning slightly softens hits; well-fed + trained breath slightly strengthens.
     */
    public static float spellPowerFactor(float liveBiologyQ) {
        float weight = BalanceConfig.BIOLOGY_SPELL_POWER_WEIGHT.get().floatValue();
        if (weight <= 0f) {
            return 1f;
        }
        float baseline = Math.max(0.05f, BalanceConfig.BIOLOGY_DEFAULT_BASELINE.get().floatValue());
        float ratio = liveBiologyQ / baseline;
        float factor = 1f + weight * (ratio - 1f);
        float min = BalanceConfig.BIOLOGY_SPELL_POWER_MIN.get().floatValue();
        float max = BalanceConfig.BIOLOGY_SPELL_POWER_MAX.get().floatValue();
        return Mth.clamp(factor, min, max);
    }

    /**
     * Stage II race hook — sets the stored Orkanum baseline without wiping other progression.
     * Call on race assignment / initiation when races ship.
     */
    public static void applyRaceBaseline(PlayerPsiData data, float baseline) {
        data.setBiologyQ(Math.max(0.05f, baseline));
    }

    /** Default human baseline from config. */
    public static float defaultBaseline() {
        return BalanceConfig.BIOLOGY_DEFAULT_BASELINE.get().floatValue();
    }

    private static float hungerFactor(Player player) {
        var food = player.getFoodData();
        int level = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        float min = BalanceConfig.BIOLOGY_HUNGER_MIN.get().floatValue();
        if (level <= 6) {
            return min;
        }
        if (level >= 18 && saturation > 0f) {
            return 1f + BalanceConfig.BIOLOGY_SATURATION_BONUS.get().floatValue();
        }
        float t = (level - 6f) / 12f;
        return min + (1f - min) * t;
    }

    private static float airFactor(Player player) {
        int max = player.getMaxAirSupply();
        if (max <= 0) {
            return 1f;
        }
        float ratio = player.getAirSupply() / (float) max;
        float min = BalanceConfig.BIOLOGY_AIR_MIN.get().floatValue();
        if (ratio >= 0.85f) {
            return 1f;
        }
        if (ratio <= 0.25f) {
            return min;
        }
        float t = (ratio - 0.25f) / 0.6f;
        return min + (1f - min) * t;
    }
}
