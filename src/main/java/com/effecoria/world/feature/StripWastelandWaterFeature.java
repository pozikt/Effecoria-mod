package com.effecoria.world.feature;

import com.effecoria.world.DeadWastelandHydrology;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Evaporate inland wasteland water only (air). Never touches biome-border cells —
 * that was eating ocean edges and creating waterfall trenches.
 */
public final class StripWastelandWaterFeature extends Feature<NoneFeatureConfiguration> {
    public StripWastelandWaterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        boolean any = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // 3×3 footprint so lakes get cleared even with sparse placement samples
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                int yMin = Math.max(level.getMinBuildHeight() + 1, surface - 32);
                int yMax = Math.min(level.getMaxBuildHeight() - 1, surface + 4);
                for (int y = yMin; y <= yMax; y++) {
                    cursor.set(x, y, z);
                    if (!DeadWastelandHydrology.isInteriorDryCell(level, cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!DeadWastelandHydrology.isForbiddenWater(state)) {
                        continue;
                    }
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    any = true;
                }
            }
        }
        return any;
    }
}
