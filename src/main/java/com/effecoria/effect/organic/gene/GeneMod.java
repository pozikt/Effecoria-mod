package com.effecoria.effect.organic.gene;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import net.minecraft.network.chat.Component;

/**
 * Stage-I gene grafts an Organic mage can inscribe onto a living host.
 * Keep bits stable — append only.
 */
public enum GeneMod {
    HYPER_REGEN(0, 0.55f, 18f),
    SPRINT_LIMBS(1, 0.40f, 12f),
    KEEN_EYES(2, 0.40f, 10f),
    ECHO_SENSE(3, 0.65f, 16f),
    ORKANUMN_WEAVE(4, 0.50f, 14f),
    KERATIN_PLATES(5, 0.45f, 12f),
    GILL_BUDS(6, 0.55f, 12f),
    TOXIN_GLANDS(7, 0.70f, 16f);

    public static final int MAX_SLOTS = 2;

    private final int bitIndex;
    private final float minMastery;
    private final float applyPsiCost;

    GeneMod(int bitIndex, float minMastery, float applyPsiCost) {
        this.bitIndex = bitIndex;
        this.minMastery = minMastery;
        this.applyPsiCost = applyPsiCost;
    }

    public int bitIndex() {
        return bitIndex;
    }

    public int mask() {
        return 1 << bitIndex;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public float minMastery() {
        return minMastery;
    }

    public float applyPsiCost() {
        return applyPsiCost;
    }

    public Component title() {
        return Component.translatable("gene.effecoria." + id());
    }

    public Component benefit() {
        return Component.translatable("gene.effecoria." + id() + ".benefit");
    }

    public Component cost() {
        return Component.translatable("gene.effecoria." + id() + ".cost");
    }

    public static Optional<GeneMod> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        for (GeneMod mod : values()) {
            if (mod.id().equals(path) || mod.name().equalsIgnoreCase(path)) {
                return Optional.of(mod);
            }
        }
        return Optional.empty();
    }

    public static List<GeneMod> unlockedFor(float mastery) {
        List<GeneMod> out = new ArrayList<>();
        for (GeneMod mod : values()) {
            if (mastery + 1.0e-4f >= mod.minMastery) {
                out.add(mod);
            }
        }
        return out;
    }

    public static boolean compatible(Set<GeneMod> chosen) {
        if (chosen.contains(KEEN_EYES) && chosen.contains(ECHO_SENSE)) {
            return false;
        }
        if (chosen.contains(SPRINT_LIMBS) && chosen.contains(KERATIN_PLATES)) {
            return false;
        }
        if (chosen.contains(HYPER_REGEN) && chosen.contains(TOXIN_GLANDS)) {
            return false;
        }
        return true;
    }

    public static EnumSet<GeneMod> fromMask(int mask) {
        EnumSet<GeneMod> set = EnumSet.noneOf(GeneMod.class);
        for (GeneMod mod : values()) {
            if ((mask & mod.mask()) != 0) {
                set.add(mod);
            }
        }
        return set;
    }

    public static int toMask(Iterable<GeneMod> mods) {
        int mask = 0;
        for (GeneMod mod : mods) {
            mask |= mod.mask();
        }
        return mask;
    }
}
