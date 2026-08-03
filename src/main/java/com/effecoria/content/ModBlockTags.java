package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Datapack block tags for Φ / anti-magic rules. */
public final class ModBlockTags {
    private ModBlockTags() {}

    /** Dense anti-magic mass (lead stand-ins for Stage I) — ZNΦ within 2 blocks. */
    public static final TagKey<Block> ZERO_FLUX = TagKey.create(Registries.BLOCK, EffecoriaMod.id("zero_flux"));

    /** Cold iron — softer ZNΦ shell within 1 block. */
    public static final TagKey<Block> COLD_IRON = TagKey.create(Registries.BLOCK, EffecoriaMod.id("cold_iron"));
}
