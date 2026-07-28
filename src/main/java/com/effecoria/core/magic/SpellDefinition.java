package com.effecoria.core.magic;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

public record SpellDefinition(
        ResourceLocation id,
        MagicSchool requiredSchool,
        float frequencyHz,
        float baseCost,
        float powerMultiplier,
        float sideEntropyRatio,
        float minPhi,
        RadialCategory radialCategory,
        List<SpellEffectEntry> effects) {}
