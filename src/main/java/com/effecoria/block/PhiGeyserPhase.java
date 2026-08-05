package com.effecoria.block;

import net.minecraft.util.StringRepresentable;

/** Lifecycle of a Φ-geyser crack. */
public enum PhiGeyserPhase implements StringRepresentable {
    DORMANT("dormant"),
    PRECURSOR("precursor"),
    ERUPTING("erupting"),
    COOLDOWN("cooldown");

    private final String name;

    PhiGeyserPhase(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
