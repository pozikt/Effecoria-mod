package com.effecoria.core.phi;

import com.effecoria.config.BalanceConfig;

import net.minecraft.world.entity.player.Player;

/** Creative-mode testing bypass: infinite ambient Φ and frictionless casting. */
public final class CreativeGodMode {
    private CreativeGodMode() {}

    public static boolean isActive(Player player) {
        return player != null
                && player.isCreative()
                && BalanceConfig.CREATIVE_GOD_MODE.get();
    }
}
