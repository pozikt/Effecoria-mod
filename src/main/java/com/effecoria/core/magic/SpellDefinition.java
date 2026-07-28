package com.effecoria.core.magic;

import java.util.List;

public record SpellDefinition(
        ResourceLocation id,
        MagicSchool requiredSchool,
        float frequencyHz,
        float baseCost,
        float powerMultiplier,
        float sideEntropyRatio,
        float minPhi,
        List<SpellEffectEntry> effects) {}
