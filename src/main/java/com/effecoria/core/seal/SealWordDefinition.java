package com.effecoria.core.seal;

import net.minecraft.resources.ResourceLocation;

/**
 * Datapack seal lexicon entry.
 *
 * @param effect logical effect key for PROPERTY/TRIGGER/SENSE ({@code hardness}, {@code hurt}, …)
 * @param numberValue numeric magnitude for NUMBER words
 * @param soundEvent optional sound id for MODIFIER / sound defaults
 * @param starter granted on SEALS school initiation
 */
public record SealWordDefinition(
        ResourceLocation id,
        SealWordKind kind,
        String effect,
        float numberValue,
        ResourceLocation soundEvent,
        float psiCost,
        float minMastery,
        boolean starter) {}
