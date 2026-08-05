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
 * In this TerraBlender region slice, vanilla {@link Biomes#DESERT} becomes Dead Wasteland.
 *
 * <p>Do <b>not</b> invent sparse {@code VanillaParameterOverlayBuilder} niches for deserts —
 * unmatched climate attractors make {@code /locate biome} teleport under bedrock while the
 * surface stays a normal biome. {@code replaceBiome(DESERT, …)} keeps exact vanilla desert
 * parameter points (flat arid inland), so locate lands on real sand and badlands hoodoos stay
 * untouched.
 */
public final class DeadWastelandRegion extends Region {
    public DeadWastelandRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder ->
                builder.replaceBiome(Biomes.DESERT, ModBiomes.DEAD_WASTELAND));
    }
}
