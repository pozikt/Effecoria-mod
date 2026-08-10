package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;

/** Φ-conductivity of a craft material (0 = insulator, 1 = excellent conductor). */
public record MaterialConductivityDefinition(
        ResourceLocation id,
        ResourceLocation itemId,
        float conductivity,
        String notes) {}
