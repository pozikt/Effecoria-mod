package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

/** Item seal (enchantment analogue) from data/effecoria/item_seals. */
public record ItemSealDefinition(
        ResourceLocation id,
        int maxLevel,
        List<TagKey<Item>> applicableTags,
        String effect,
        Map<String, Float> params,
        boolean starter) {}
