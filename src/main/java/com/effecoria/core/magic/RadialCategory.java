package com.effecoria.core.magic;

import java.util.Locale;

/**
 * Radial menu ring category for a spell (data-driven via {@code radialCategory} in JSON).
 */
public enum RadialCategory {
    MOVEMENT("movement"),
    COMBAT("combat"),
    UTILITY("utility"),
    SEALS("seals");

    private final String serializedName;

    RadialCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public String langKey() {
        return "gui.effecoria.radial." + serializedName;
    }

    /** Ring order outside favorites: Movement → Combat → Utility → Seals. */
    public static RadialCategory[] outerRingOrder() {
        return new RadialCategory[] {MOVEMENT, COMBAT, UTILITY, SEALS};
    }

    public static RadialCategory fromSerializedName(String name) {
        String key = name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
        for (RadialCategory category : values()) {
            if (category.serializedName.equals(key)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown radialCategory: " + name);
    }
}
