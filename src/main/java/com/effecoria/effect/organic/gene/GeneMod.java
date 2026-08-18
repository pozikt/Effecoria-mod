package com.effecoria.effect.organic.gene;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import net.minecraft.network.chat.Component;

/**
 * Organic gene grafts. Bit indices are stable — append new mods only at the end.
 *
 * <p>Tier 1 = basic body craft, Tier 2 = advanced, Tier 3 = master / near-metamorphosis.
 */
public enum GeneMod {
    // —— Tier 1 (existing bits 0–7 kept) ——
    HYPER_REGEN(0, Tier.BASIC, 0.55f, 18f, false, GeneAnatomySlot.TORSO),
    SPRINT_LIMBS(1, Tier.BASIC, 0.40f, 12f, false, GeneAnatomySlot.HIND),
    KEEN_EYES(2, Tier.BASIC, 0.40f, 10f, false, GeneAnatomySlot.HEAD),
    ECHO_SENSE(3, Tier.ADVANCED, 0.65f, 16f, false, GeneAnatomySlot.HEAD),
    ORKANUMN_WEAVE(4, Tier.BASIC, 0.50f, 14f, false, GeneAnatomySlot.TORSO),
    KERATIN_PLATES(5, Tier.BASIC, 0.45f, 12f, false, GeneAnatomySlot.TORSO),
    GILL_BUDS(6, Tier.BASIC, 0.50f, 12f, false, GeneAnatomySlot.TORSO),
    TOXIN_GLANDS(7, Tier.ADVANCED, 0.70f, 16f, false, GeneAnatomySlot.TORSO),
    // —— New ——
    BONE_WEAPONS(8, Tier.BASIC, 0.42f, 12f, false, GeneAnatomySlot.HEAD),
    MUSCLE_HYPERTROPHY(9, Tier.BASIC, 0.48f, 14f, false, GeneAnatomySlot.FORE),
    MEMBRANE_WINGS(10, Tier.ADVANCED, 0.72f, 22f, false, GeneAnatomySlot.DORSUM),
    EXTRA_LIMBS(11, Tier.ADVANCED, 0.75f, 20f, false, GeneAnatomySlot.FORE),
    LIMB_REGEN(12, Tier.ADVANCED, 0.78f, 24f, false, GeneAnatomySlot.HIND),
    BEAST_MORPH(13, Tier.MASTER, 0.88f, 28f, false, GeneAnatomySlot.DORSUM),
    PHI_HEART(14, Tier.MASTER, 0.90f, 32f, true, GeneAnatomySlot.TORSO),
    CELL_IMMORTAL(15, Tier.MASTER, 0.92f, 30f, true, GeneAnatomySlot.TORSO),
    SYMBIOTE_COLONY(16, Tier.MASTER, 0.85f, 26f, true, GeneAnatomySlot.TORSO);

    /** Mastery required to lock grafts into heritable DNA. */
    public static final float DNA_LOCK_MASTERY = 0.88f;
    public static final float DNA_LOCK_PSI_COST = 40f;

    public enum Tier {
        BASIC,
        ADVANCED,
        MASTER
    }

    private final int bitIndex;
    private final Tier tier;
    private final float minMastery;
    private final float applyPsiCost;
    private final boolean playerOnly;
    private final GeneAnatomySlot slot;

    GeneMod(
            int bitIndex,
            Tier tier,
            float minMastery,
            float applyPsiCost,
            boolean playerOnly,
            GeneAnatomySlot slot) {
        this.bitIndex = bitIndex;
        this.tier = tier;
        this.minMastery = minMastery;
        this.applyPsiCost = applyPsiCost;
        this.playerOnly = playerOnly;
        this.slot = slot;
    }

    public int bitIndex() {
        return bitIndex;
    }

    public int mask() {
        return 1 << bitIndex;
    }

    public Tier tier() {
        return tier;
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

    public boolean playerOnly() {
        return playerOnly;
    }

    public GeneAnatomySlot slot() {
        return slot;
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

    public Component tierLabel() {
        return Component.translatable("gene.effecoria.tier." + tier.name().toLowerCase(Locale.ROOT));
    }

    /** Graft slots scale with engineer breathing mastery. */
    public static int maxSlots(float mastery) {
        if (mastery >= 0.82f) {
            return 4;
        }
        if (mastery >= 0.60f) {
            return 3;
        }
        return 2;
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
        if (chosen.contains(HYPER_REGEN) && chosen.contains(LIMB_REGEN)) {
            return false;
        }
        if (chosen.contains(MUSCLE_HYPERTROPHY) && chosen.contains(EXTRA_LIMBS)) {
            return false;
        }
        if (chosen.contains(MEMBRANE_WINGS) && chosen.contains(GILL_BUDS)) {
            return false;
        }
        if (chosen.contains(PHI_HEART) && chosen.contains(SYMBIOTE_COLONY)) {
            return false;
        }
        if (chosen.contains(BEAST_MORPH) && chosen.contains(CELL_IMMORTAL)) {
            return false;
        }
        if (chosen.contains(BONE_WEAPONS) && chosen.contains(TOXIN_GLANDS)) {
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
