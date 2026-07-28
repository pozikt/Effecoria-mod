package com.effecoria.core.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Data-driven spell template. Loaded from {@code data/effecoria/spells/*.json} in phase 1.
 */
public record SpellDefinition(
        ResourceLocation id,
        MagicSchool requiredSchool,
        float frequencyHz,
        float baseCost,
        float powerMultiplier,
        float sideEntropyRatio,
        float minPhi) {

    public static final Codec<SpellDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(SpellDefinition::id),
            Codec.STRING.xmap(MagicSchool::fromSerializedName, MagicSchool::getSerializedName)
                    .fieldOf("school").forGetter(SpellDefinition::requiredSchool),
            Codec.FLOAT.fieldOf("frequency_hz").forGetter(SpellDefinition::frequencyHz),
            Codec.FLOAT.fieldOf("base_cost").forGetter(SpellDefinition::baseCost),
            Codec.FLOAT.optionalFieldOf("power_multiplier", 1f).forGetter(SpellDefinition::powerMultiplier),
            Codec.FLOAT.optionalFieldOf("side_entropy", 0.05f).forGetter(SpellDefinition::sideEntropyRatio),
            Codec.FLOAT.optionalFieldOf("min_phi", 0.1f).forGetter(SpellDefinition::minPhi)
    ).apply(instance, SpellDefinition::new));
}
