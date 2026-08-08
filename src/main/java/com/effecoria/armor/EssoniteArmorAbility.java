package com.effecoria.armor;

import java.util.Locale;
import java.util.Optional;

public enum EssoniteArmorAbility {
    FLASH,
    CRYSTAL_SKIN,
    WINGS,
    OMEGA_BLOCK;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<EssoniteArmorAbility> fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static EssoniteArmorAbility cycle(EssoniteArmorAbility current) {
        EssoniteArmorAbility[] all = values();
        return all[(current.ordinal() + 1) % all.length];
    }
}
