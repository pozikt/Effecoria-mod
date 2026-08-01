package com.effecoria.core.seal;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

/** Outcome of placing a seal, including conflict + layer snapshot for builder feedback. */
public record SealPlaceOutcome(
        SealPlaceResult result,
        @Nullable ResourceLocation previousOffensive,
        List<ResourceLocation> layersAfter) {

    public static SealPlaceOutcome of(
            SealPlaceResult result,
            @Nullable ResourceLocation previousOffensive,
            List<SealInstance> layers) {
        return new SealPlaceOutcome(
                result,
                previousOffensive,
                layers.stream().map(SealInstance::typeId).toList());
    }
}
