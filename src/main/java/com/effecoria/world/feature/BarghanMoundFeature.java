package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Soft Φ-barghan dust mound on the Glass Plain. */
public final class BarghanMoundFeature extends Feature<NoneFeatureConfiguration> {
    public BarghanMoundFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int cx = origin.getX();
        int cz = origin.getZ();
        int cy = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        BlockPos center = new BlockPos(cx, cy - 1, cz);
        BlockState ground = level.getBlockState(center);
        if (!ground.is(ModBlocks.PHI_GLASS.get())
                && !ground.is(ModBlocks.PHI_GLASS_DUNE.get())
                && !ground.is(ModBlocks.ESSONITE_DUST_BLOCK.get())) {
            return false;
        }

        BlockState dust = ModBlocks.ESSONITE_DUST_BLOCK.get().defaultBlockState();
        int radius = 2 + random.nextInt(3);
        int height = 2 + random.nextInt(3);
        boolean placed = false;
        for (int dy = 0; dy < height; dy++) {
            int r = Math.max(1, radius - dy);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + random.nextInt(2)) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy + 1, dz);
                    if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, dust, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }
}
