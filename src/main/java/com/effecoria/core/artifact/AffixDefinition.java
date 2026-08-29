package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Rolled jewelry affix from data/effecoria/artifact/affixes. */
public record AffixDefinition(
        ResourceLocation id,
        String polarity,
        String effect,
        int weight,
        List<String> templates,
        Map<String, Float> params,
        int maxTier) {

    public float param(String key, float fallback) {
        return params.getOrDefault(key, fallback);
    }
}
