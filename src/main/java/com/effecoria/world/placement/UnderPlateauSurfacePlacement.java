package com.effecoria.world.placement;

import com.effecoria.content.ModBiomeTags;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/**
 * Keeps underground Φ features under real Essence Plateau mountains (surface biome),
 * instead of orphan underground biome pockets.
 */
public final class UnderPlateauSurfacePlacement extends PlacementModifier {
    public static final UnderPlateauSurfacePlacement INSTANCE = new UnderPlateauSurfacePlacement();
    public static final MapCodec<UnderPlateauSurfacePlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    /** Minimum depth below WG surface for cave-only features (Φ-water lakes, etc.). */
    private static final int MIN_DEPTH_BELOW_SURFACE = 12;

    private UnderPlateauSurfacePlacement() {}

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
        if (pos.getY() > surfaceY - MIN_DEPTH_BELOW_SURFACE) {
            return Stream.empty();
        }
        if (pos.getY() > surfaceY + 24) {
            return Stream.empty();
        }
        Holder<Biome> surfaceBiome =
                level.getBiome(new BlockPos(pos.getX(), Math.max(surfaceY, level.getMinBuildHeight()), pos.getZ()));
        if (!surfaceBiome.is(ModBiomeTags.ESSENCE_PLATEAU)) {
            return Stream.empty();
        }
        return Stream.of(pos);
    }

    @Override
    public PlacementModifierType<?> type() {
        return com.effecoria.content.ModPlacementModifiers.UNDER_PLATEAU_SURFACE.get();
    }
}
