package com.effecoria.world;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * In this TerraBlender region slice, vanilla {@link Biomes#DARK_FOREST} becomes Ω-Scar.
 *
 * <p>{@code replaceBiome} keeps vanilla dark-forest climate points so locate stays reliable.
 */
public final class OmegaScarRegion extends Region {
    public OmegaScarRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder ->
                builder.replaceBiome(Biomes.DARK_FOREST, ModBiomes.OMEGA_SCAR));
    }
}
