package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Small surface ZNΦ mute disk — crust in {@code #effecoria:zero_flux}, optional lead core.
 */
public final class ZnPhiMutePatchFeature extends Feature<NoneFeatureConfiguration> {
    public ZnPhiMutePatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int cx = origin.getX();
        int cz = origin.getZ();
        int radius = 2 + random.nextInt(3);
        int r2 = radius * radius;
        boolean placed = false;
        BlockPos centerSurface = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2 + random.nextInt(2)) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockPos surface = new BlockPos(x, y - 1, z);
                BlockState ground = level.getBlockState(surface);
                if (!isReplaceableSurface(ground)) {
                    continue;
                }
                BlockPos above = surface.above();
                if (!level.getFluidState(above).isEmpty()) {
                    continue;
                }
                if (level.getBlockState(above).is(Blocks.SNOW)) {
                    level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(surface, ModBlocks.ZNPHI_CRUST.get().defaultBlockState(), 2);
                placed = true;
                if (dx == 0 && dz == 0) {
                    centerSurface = surface.immutable();
                }
            }
        }

        if (placed && centerSurface != null && random.nextFloat() < 0.25f) {
            level.setBlock(centerSurface, ModBlocks.LEAD_BLOCK.get().defaultBlockState(), 2);
        }
        return placed;
    }

    private static boolean isReplaceableSurface(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.PACKED_MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SNOW_BLOCK);
    }
}
