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
 * Giant Φ-canopy — claims the full vanilla jungle climate family so patches stay
 * large enough for mega-trees (jungle + sparse + bamboo + mangrove).
 *
 * <p>{@code replaceBiome} keeps locate reliable on real climate points.
 */
public final class EmeraldCanopyRegion extends Region {
    public EmeraldCanopyRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.JUNGLE, ModBiomes.EMERALD_CANOPY);
            builder.replaceBiome(Biomes.SPARSE_JUNGLE, ModBiomes.EMERALD_CANOPY);
            builder.replaceBiome(Biomes.BAMBOO_JUNGLE, ModBiomes.EMERALD_CANOPY);
            builder.replaceBiome(Biomes.MANGROVE_SWAMP, ModBiomes.EMERALD_CANOPY);
        });
    }
}
