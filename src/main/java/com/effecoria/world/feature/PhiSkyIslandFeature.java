package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Small floating Φ-rock islands in open air at extreme heights (sky layer).
 */
public final class PhiSkyIslandFeature extends Feature<NoneFeatureConfiguration> {
    public PhiSkyIslandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!level.isEmptyBlock(origin)) {
            return false;
        }

        int radiusXZ = 3 + random.nextInt(5);
        int radiusY = 2 + random.nextInt(3);
        BlockState stone = ModBlocks.PHI_STONE.get().defaultBlockState();
        BlockState dirt = ModBlocks.PHI_DIRT.get().defaultBlockState();
        BlockState grass = ModBlocks.PHI_GRASS.get().defaultBlockState();
        BlockState crystal = ModBlocks.ESSONITE_CRYSTAL
                .get()
                .defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP);

        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
            for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                for (int dy = -radiusY; dy <= radiusY; dy++) {
                    double nx = dx / (double) radiusXZ;
                    double ny = dy / (double) Math.max(1, radiusY);
                    double nz = dz / (double) radiusXZ;
                    if (nx * nx + ny * ny * 1.35 + nz * nz > 1.0 + random.nextDouble() * 0.12) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isEmptyBlock(cursor) && !level.getBlockState(cursor).is(Blocks.SNOW)) {
                        continue;
                    }
                    boolean topShell = dy >= radiusY - 1;
                    boolean upper = dy >= 0;
                    BlockState state = stone;
                    if (topShell && upper) {
                        state = grass;
                    } else if (upper && dy >= -1) {
                        state = dirt;
                    }
                    level.setBlock(cursor, state, 2);
                    placed = true;

                    if (topShell
                            && random.nextFloat() < 0.04f
                            && level.isEmptyBlock(cursor.above())) {
                        level.setBlock(cursor.above(), crystal, 2);
                    }
                }
            }
        }
        return placed;
    }
}
