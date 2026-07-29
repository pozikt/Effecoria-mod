package com.effecoria.core.progression;

import com.effecoria.config.BalanceConfig;

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
