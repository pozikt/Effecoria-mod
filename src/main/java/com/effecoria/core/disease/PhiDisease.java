package com.effecoria.core.disease;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

/** Catalog of Φ-field pathologies (Orkanum / Ψ / DNA / infection). */
public enum PhiDisease {
    ESSENCE_BURN(DiseaseClass.ORKANUM, 3),
    ORKANUMN_ATROPHY(DiseaseClass.ORKANUM, 3),
    ESSENTOCYTOSIS(DiseaseClass.ORKANUM, 3),
    OMEGA_SICKNESS(DiseaseClass.PSI, 3),
    SOUL_DISSONANCE(DiseaseClass.PSI, 4),
    GHOST_ECHO(DiseaseClass.PSI, 2),
    MAGE_BARRENNESS(DiseaseClass.DNA, 1),
    CURSE_ROT(DiseaseClass.DNA, 3),
    DUST_LUNG(DiseaseClass.INFECTION, 3),
    OMEGA_ROT(DiseaseClass.INFECTION, 3),
    CRYSTAL_FEVER(DiseaseClass.INFECTION, 2);

    public enum DiseaseClass {
        ORKANUM,
        PSI,
        DNA,
        INFECTION
    }

    private final DiseaseClass diseaseClass;
    private final int maxStage;

    PhiDisease(DiseaseClass diseaseClass, int maxStage) {
        this.diseaseClass = diseaseClass;
        this.maxStage = maxStage;
    }

    public DiseaseClass diseaseClass() {
        return diseaseClass;
    }

    public int maxStage() {
        return maxStage;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "disease.effecoria." + id();
    }

    public static Optional<PhiDisease> byId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        for (PhiDisease disease : values()) {
            if (disease.id().equals(key)) {
                return Optional.of(disease);
            }
        }
        return Optional.empty();
    }
}
