package com.effecoria.core.formula;

import com.effecoria.config.BalanceConfig;

/** Spell mastery from breathing technique and stored essence. */
public final class Mastery {
    private Mastery() {}

    /**
     * @return multiplier in ~[1.0, 1.3] — higher power, slightly lower cost via {@link #costMultiplier(float)}.
     */
    public static float factor(float breathingMastery, int essence) {
        float cap = BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue();
        float normalized = cap > 0f ? Math.min(1f, breathingMastery / cap) : 0f;
        float fromBreathing = normalized * BalanceConfig.MASTERY_BREATHING_MAX.get().floatValue();
        float fromEssence = Math.min(
                essence * BalanceConfig.MASTERY_ESSENCE_PER_POINT.get().floatValue(),
                BalanceConfig.MASTERY_ESSENCE_CAP.get().floatValue());
        return 1f + fromBreathing + fromEssence;
    }

    /** Subtle cost reduction — not linear with power. */
    public static float costMultiplier(float mastery) {
        float bonus = Math.max(0f, mastery - 1f);
        float reduction = bonus * BalanceConfig.MASTERY_COST_REDUCTION_RATIO.get().floatValue();
        return Math.max(0.5f, 1f - reduction);
    }
}
