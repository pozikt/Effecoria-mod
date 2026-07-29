package com.effecoria.core.phi;

import com.effecoria.config.BalanceConfig;

import net.minecraft.world.entity.player.Player;

/** Creative-mode testing bypass: frictionless casting with sane effect scaling. */
public final class CreativeGodMode {
    private CreativeGodMode() {}

    public static boolean isActive(Player player) {
        return player != null
                && player.isCreative()
                && BalanceConfig.CREATIVE_GOD_MODE.get();
    }

    /** Caps runaway spell power from stacked Ψ × Φ in creative testing. */
    public static float clampSpellPower(Player player, float rawPower) {
        if (!isActive(player)) {
            return rawPower;
        }
        float cap = BalanceConfig.CREATIVE_SPELL_POWER_CAP.get().floatValue();
        if (cap <= 0f) {
            return rawPower;
        }
        return Math.min(rawPower, cap);
    }
}
