package com.effecoria.core.tower;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Body template applied after a tower revive (lasts until the next death). */
public enum TowerBodyType implements StringRepresentable {
    BASIC("basic", 0, 0),
    ENHANCED("enhanced", 5 * 20, 10),
    COMBAT("combat", 15 * 20, 10),
    ARCANE("arcane", 15 * 20, 10);

    private final String id;
    private final int delayTicks;
    private final int omegaPercent;

    TowerBodyType(String id, int delayTicks, int omegaPercent) {
        this.id = id;
        this.delayTicks = delayTicks;
        this.omegaPercent = omegaPercent;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public int delayTicks() {
        return delayTicks;
    }

    /** Base Ω% added on revive for this body (before scatter multiply). */
    public int omegaPercent() {
        return omegaPercent == 0 ? 5 : omegaPercent;
    }

    public TowerBodyType next() {
        TowerBodyType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static TowerBodyType fromId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return BASIC;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        for (TowerBodyType type : values()) {
            if (type.id.equals(key)) {
                return type;
            }
        }
        return BASIC;
    }
}
