package com.effecoria.core.artifact;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Datapack material profile for artifact craft (conductivity + affix bias). */
public record MaterialDefinition(
        ResourceLocation id,
        ResourceLocation itemId,
        float conductivity,
        float positiveBias,
        float negativeBias,
        List<ImplicitAffix> implicitAffixes,
        String notes) {

    public record ImplicitAffix(ResourceLocation affixId, int tier, float chance) {}

    public static MaterialDefinition fallback(ResourceLocation itemId) {
        return new MaterialDefinition(
                itemId,
                itemId,
                MaterialConductivity.DEFAULT,
                0f,
                0f,
                List.of(),
                "");
    }
}
