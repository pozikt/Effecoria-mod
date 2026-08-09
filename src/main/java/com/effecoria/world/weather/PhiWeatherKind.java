package com.effecoria.world.weather;

/**
 * Φ/Ω atmospheric phenomena. Priority for HUD/dominant kind is declaration order
 * (later enums in {@link #priority()} win when several apply).
 */
public enum PhiWeatherKind {
    CLEAR,
    ESSENCE_DEW,
    ESSENCE_MIST,
    ESSENCE_RAIN,
    OMEGA_FOG,
    ESSENCE_STORM,
    ESSENCE_LIGHTNING,
    ESSENCE_TORNADO,
    OMEGA_RAIN,
    BLOOD_RAIN;

    public String id() {
        return name().toLowerCase();
    }

    public static PhiWeatherKind fromId(String id) {
        if (id == null || id.isEmpty()) {
            return CLEAR;
        }
        try {
            return PhiWeatherKind.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return CLEAR;
        }
    }

    /** Higher = more severe / overrides milder ambient weather. */
    public int priority() {
        return ordinal();
    }

    public boolean isOmega() {
        return this == OMEGA_FOG || this == OMEGA_RAIN || this == BLOOD_RAIN;
    }

    public boolean isStormFamily() {
        return this == ESSENCE_STORM
                || this == ESSENCE_LIGHTNING
                || this == ESSENCE_TORNADO
                || this == BLOOD_RAIN;
    }
}
