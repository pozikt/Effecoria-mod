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
    /** ~40–100 Hz — necromancy (external Ψ relay; later) */
    NECROMANCY("necromancy", 55f),
    /** ~100+ Hz — spatial (later) */
    SPATIAL("spatial", 120f),
    /** complex frequency — seals / corruption (later) */
    SEALS("seals", 0f),
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
        return this == MENTAL || this == ELEMENTAL || this == ORGANIC;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static MagicSchool fromSerializedName(String name) {
        for (MagicSchool school : values()) {
            if (school.name.equals(name)) {
                return school;
            }
        }
        return NONE;
    }
}
