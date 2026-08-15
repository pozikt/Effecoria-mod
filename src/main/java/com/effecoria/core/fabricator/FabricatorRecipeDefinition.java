package com.effecoria.core.fabricator;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Datapack fabricator recipe — rebuild matter; no elemental transmutation. */
public record FabricatorRecipeDefinition(
        ResourceLocation id,
        ResourceLocation resultItem,
        int resultCount,
        List<FabricatorIngredient> ingredients,
        int cookTicks,
        int powerPerTick,
        int minClass,
        int omegaPercent) {}
