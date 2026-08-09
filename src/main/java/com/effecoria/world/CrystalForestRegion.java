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
 * In this TerraBlender region slice, vanilla {@link Biomes#FOREST} becomes Crystal Forest.
 *
 * <p>Uses {@code replaceBiome} (same reliability rule as Dead Wasteland) so {@code /locate biome}
 * lands on real forest climate points instead of sparse overlay attractors under bedrock.
 */
public final class CrystalForestRegion extends Region {
    public CrystalForestRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder ->
                builder.replaceBiome(Biomes.FOREST, ModBiomes.CRYSTAL_FOREST));
    }
}
