package com.effecoria.core.progression;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Playable Effecoria races — Orkanum baselines from encyclopedia biology.
 * Soft school affinity is localization-only in MVP (no hard locks).
 */
public enum PlayerRace implements StringRepresentable {
    HUMAN(0.60f, 0xFF6A6A70),
    ORC(0.85f, 0xFF4A6A30),
    ELF(0.75f, 0xFF3A5A88),
    DWARF(0.70f, 0xFF6A5020),
    VARANAGI(0.90f, 0xFF5A3A28),
    DRYAD(0.95f, 0xFF2A6630),
    LONVER(1.05f, 0xFF4A4A68),
    HARPY(0.70f, 0xFF5A6080),
    VAMPIRE(0.35f, 0xFF5A2028);

    private final float defaultBaseline;
    private final int headerColor;

    PlayerRace(float defaultBaseline, int headerColor) {
        this.defaultBaseline = defaultBaseline;
        this.headerColor = headerColor;
    }

    /** Lore baseline when config override is absent. */
    public float defaultBaseline() {
        return defaultBaseline;
    }

    public int headerColor() {
        return headerColor;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("race.effecoria." + getSerializedName());
    }

    public Component overview() {
        return Component.translatable("race.effecoria." + getSerializedName() + ".overview");
    }

    public Component traits() {
        return Component.translatable("race.effecoria." + getSerializedName() + ".traits");
    }

    /** Origins-style impact dots 1–3. */
    public int difficulty() {
        return switch (this) {
            case HUMAN -> 1;
            case ORC, ELF, DWARF, HARPY -> 2;
            case VARANAGI, DRYAD, LONVER, VAMPIRE -> 3;
        };
    }

    public static Optional<PlayerRace> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        for (PlayerRace race : values()) {
            if (race.getSerializedName().equalsIgnoreCase(path) || race.name().equalsIgnoreCase(path)) {
                return Optional.of(race);
            }
        }
        return Optional.empty();
    }

    public static PlayerRace fromSerializedName(String name) {
        return byId(name).orElse(HUMAN);
    }
}
