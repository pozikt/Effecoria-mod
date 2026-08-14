package com.effecoria.core.circuit;

import net.minecraft.util.StringRepresentable;

/**
 * Frequency channels of a Φ-network (Lonver Essential Flow Laws).
 * {@link #BROADBAND} is untuned reactor noise — usable, but leaky on tuned loads.
 */
public enum PhiChannel implements StringRepresentable {
    BROADBAND("broadband", 0f),
    LIFE("life", 5f),
    PSI("psi", 7.8f),
    INDUSTRY("industry", 22.3f),
    DEFENSE("defense", 55f);

    private final String name;
    private final float hz;

    PhiChannel(String name, float hz) {
        this.name = name;
        this.hz = hz;
    }

    public float hz() {
        return hz;
    }

    public PhiChannel next() {
        PhiChannel[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static PhiChannel byName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return BROADBAND;
        }
        for (PhiChannel channel : values()) {
            if (channel.name.equals(raw)) {
                return channel;
            }
        }
        return BROADBAND;
    }
}
