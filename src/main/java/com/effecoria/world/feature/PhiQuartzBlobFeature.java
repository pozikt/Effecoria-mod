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

/** Surface / near-surface Φ-quartz blobs on the Glass Plain. */
public final class PhiQuartzBlobFeature extends Feature<NoneFeatureConfiguration> {
    public PhiQuartzBlobFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1
                - random.nextInt(4);
        BlockPos center = new BlockPos(origin.getX(), y, origin.getZ());
        BlockState quartz = ModBlocks.PHI_QUARTZ.get().defaultBlockState();
        boolean any = false;
        int r = 1 + random.nextInt(2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r + 1) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(p);
                    if (state.is(ModBlocks.PHI_GLASS.get())
                            || state.is(ModBlocks.PHI_GLASS_DUNE.get())
                            || state.is(ModBlocks.ESSONITE_DUST_BLOCK.get())
                            || state.is(ModBlocks.PARCHED_SANDSTONE.get())
                            || state.is(net.minecraft.world.level.block.Blocks.STONE)
                            || state.is(net.minecraft.world.level.block.Blocks.SANDSTONE)) {
                        level.setBlock(p, quartz, 2);
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
