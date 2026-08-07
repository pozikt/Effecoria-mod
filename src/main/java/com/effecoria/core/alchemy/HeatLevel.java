package com.effecoria.core.alchemy;

/** Φ-heat intensity published by burners and read by consumers (alembic, later furnace). */
public enum HeatLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    public boolean isPresent() {
        return this != NONE;
    }

    public HeatLevel max(HeatLevel other) {
        return other.ordinal() > this.ordinal() ? other : this;
    }

    public static HeatLevel byId(int id) {
        HeatLevel[] values = values();
        if (id < 0 || id >= values.length) {
            return NONE;
        }
        return values[id];
    }
}
