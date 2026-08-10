package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

/** Focus cut from data/effecoria/artifact/focus_cuts. */
public record FocusCutDefinition(
        ResourceLocation id,
        List<TagKey<Item>> materialTags,
        float power,
        int focusTier,
        float schoolBias,
        int cookTicks) {}
