package com.effecoria.core.magic;

import net.minecraft.util.StringRepresentable;

/**
 * Fundamental Ψ frequency class. One school per operator after initiation (spectral purity theorem).
 */
public enum MagicSchool implements StringRepresentable {
    /** ~7.8 Hz — telekinesis, perception, cognitive effects */
    MENTAL("mental", 7.8f),
    /** ~22.3 Hz — elemental / kinetic effects */
    ELEMENTAL("elemental", 22.3f),
    /** ~1–10 Hz — organic / Orkanum tissue magic */
    ORGANIC("organic", 5f),
    /** ~40–100 Hz — necromancy via external Ψ relay */
    NECROMANCY("necromancy", 55f),
    /** ~100+ Hz — spatial folds and short-range teleportation */
    SPATIAL("spatial", 120f),
    /** complex / near-zero Hz — combat corruption (b-component heavy) */
    CORRUPTION("corruption", 0.5f),
    /** seal inscriptions on blocks / later items */
    SEALS("seals", 0.45f),
    /** uninitiated */
    NONE("none", 0f);

    private final String name;
    private final float nominalFrequencyHz;

    MagicSchool(String name, float nominalFrequencyHz) {
        this.name = name;
        this.nominalFrequencyHz = nominalFrequencyHz;
    }

    public float nominalFrequencyHz() {
        return nominalFrequencyHz;
    }

    /** Schools available for player initiation right now. */
    public boolean isPlayable() {
        return this == MENTAL || this == ELEMENTAL || this == ORGANIC
                || this == NECROMANCY || this == SPATIAL || this == CORRUPTION || this == SEALS;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static MagicSchool fromSerializedName(String name) {
        if (name == null || name.isEmpty()) {
            return NONE;
        }
        String key = name.toLowerCase();
        if (key.equals("porcha")) {
            return CORRUPTION;
        }
        for (MagicSchool school : values()) {
            if (school.name.equals(key)) {
                return school;
            }
        }
        return NONE;
    }
}
