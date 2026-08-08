package com.effecoria.client;

import com.effecoria.armor.EssoniteArmorAbility;

/** Client-mirrored armor ability selection for HUD (server is authoritative on activate). */
public final class ClientArmorAbilityState {
    private static EssoniteArmorAbility selected = EssoniteArmorAbility.FLASH;

    private ClientArmorAbilityState() {}

    public static EssoniteArmorAbility selected() {
        return selected;
    }

    public static void set(EssoniteArmorAbility ability) {
        selected = ability == null ? EssoniteArmorAbility.FLASH : ability;
    }

    public static EssoniteArmorAbility cycleLocal() {
        selected = EssoniteArmorAbility.cycle(selected);
        return selected;
    }
}
