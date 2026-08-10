package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * Shaft length profile from data/effecoria/artifact/shaft_forms.
 * {@code lengthMeters} is physical length (player height ≈ 1.8).
 */
public record ShaftFormDefinition(
        ResourceLocation id,
        List<TagKey<Item>> materialTags,
        float lengthMeters,
        float reach,
        float castCostMul,
        int durability,
        int cookTicks) {}
