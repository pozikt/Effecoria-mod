package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;

/** Assemble recipe: two part kinds → result item. */
public record AssembleRecipeDefinition(
        ResourceLocation id,
        String template,
        String inputAKind,
        String inputBKind,
        ResourceLocation resultItem,
        int cookTicks) {}
