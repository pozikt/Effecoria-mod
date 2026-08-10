package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

/** Shaft form from data/effecoria/artifact/shaft_forms. */
public record ShaftFormDefinition(
        ResourceLocation id,
        List<TagKey<Item>> materialTags,
        float reach,
        float castCostMul,
        int durability,
        int cookTicks) {}
