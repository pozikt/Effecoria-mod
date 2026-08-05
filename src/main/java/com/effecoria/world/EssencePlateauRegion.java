package com.effecoria.world;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

import static terrablender.api.ParameterUtils.*;

/**
 * Surface jagged-peak mountains only. Underground Φ caves are placed under those
 * surface columns via {@code under_plateau_surface} — not as orphan cave biomes.
 */
public final class EssencePlateauRegion extends Region {
    public EssencePlateauRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        VanillaParameterOverlayBuilder builder = new VanillaParameterOverlayBuilder();
        // Vanilla jagged-peaks niche: cold, far inland, lowest erosion, peak weirdness.
        new ParameterPointListBuilder()
                .temperature(Temperature.span(Temperature.COOL, Temperature.FROZEN))
                .humidity(Humidity.span(Humidity.DRY, Humidity.NEUTRAL))
                .continentalness(Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_0)
                .depth(Depth.SURFACE)
                .weirdness(Weirdness.PEAK_NORMAL)
                .build()
                .forEach(point -> builder.add(point, ModBiomes.ESSENCE_PLATEAU));
        new ParameterPointListBuilder()
                .temperature(Temperature.span(Temperature.COOL, Temperature.FROZEN))
                .humidity(Humidity.span(Humidity.DRY, Humidity.NEUTRAL))
                .continentalness(Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_0)
                .depth(Depth.SURFACE)
                .weirdness(Weirdness.PEAK_VARIANT)
                .build()
                .forEach(point -> builder.add(point, ModBiomes.ESSENCE_PLATEAU));
        builder.build().forEach(mapper);
    }
}
