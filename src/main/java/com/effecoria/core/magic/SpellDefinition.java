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
        float minMastery,
        float minPower,
        int unlockEssenceCost,
        RadialCategory radialCategory,
        List<SpellEffectEntry> effects) {}
