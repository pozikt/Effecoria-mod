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
 * In this TerraBlender region slice, vanilla {@link Biomes#DRIPSTONE_CAVES} becomes Deep Φ pocket.
 *
 * <p>{@code replaceBiome} keeps dripstone-cave climate points so {@code /locate biome} stays reliable.
 * Other regions still generate normal dripstone caves.
 */
public final class DeepPhiPocketRegion extends Region {
    public DeepPhiPocketRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder ->
                builder.replaceBiome(Biomes.DRIPSTONE_CAVES, ModBiomes.DEEP_PHI_POCKET));
    }
}
