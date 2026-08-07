package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Datapack tags for modular Φ-alchemy machines. */
public final class ModItemTags {
    private ModItemTags() {}

    public static final TagKey<Item> PESTLES = TagKey.create(Registries.ITEM, EffecoriaMod.id("pestles"));
    public static final TagKey<Item> MORTAR_INPUTS = TagKey.create(Registries.ITEM, EffecoriaMod.id("mortar_inputs"));
    public static final TagKey<Item> BURNER_CATALYSTS =
            TagKey.create(Registries.ITEM, EffecoriaMod.id("burner_catalysts"));
    public static final TagKey<Item> ALEMBIC_WATER = TagKey.create(Registries.ITEM, EffecoriaMod.id("alembic_water"));
    public static final TagKey<Item> ALEMBIC_REAGENT_POWER =
            TagKey.create(Registries.ITEM, EffecoriaMod.id("alembic_reagent_power"));
    public static final TagKey<Item> ALEMBIC_REAGENT_OPTIONAL =
            TagKey.create(Registries.ITEM, EffecoriaMod.id("alembic_reagent_optional"));
}
