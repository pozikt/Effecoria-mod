package com.effecoria.world.feature;

import com.effecoria.world.DeadWastelandHydrology;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Shallow dry wash — only recolors surface sand to a slightly depressed ash ribbon.
 * Does not dig pits or scatter gravel (that caused the ash/sand patchwork).
 */
public final class DriedRiverbedFeature extends Feature<NoneFeatureConfiguration> {
    public DriedRiverbedFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int startX = origin.getX();
        int startZ = origin.getZ();
        Direction heading = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = 12 + random.nextInt(16);
        boolean placed = false;

        double x = startX + 0.5;
        double z = startZ + 0.5;
        float yaw = heading.toYRot();

        for (int step = 0; step < length; step++) {
            yaw += (random.nextFloat() - 0.5f) * 28f;
            double rad = Math.toRadians(yaw);
            x += Mth.sin((float) rad);
            z -= Mth.cos((float) rad);

            int bx = Mth.floor(x);
            int bz = Mth.floor(z);
            int by = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz);
            BlockPos surface = new BlockPos(bx, by - 1, bz);
            BlockState ground = level.getBlockState(surface);
            if (!isSurfaceSand(ground)) {
                continue;
            }
            // Evaporate inland wash water only — never nibble ocean/river borders
            BlockPos above = surface.above();
            if (DeadWastelandHydrology.isInteriorDryCell(level, above)
                    && DeadWastelandHydrology.isForbiddenWater(level.getBlockState(above))) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
            }
            // One-block-deep ash ribbon; keep neighbors as sand so the crust stays continuous
            level.setBlock(surface, DeadWastelandHydrology.channelFloor(), 2);
            placed = true;
        }
        return placed;
    }

    private static boolean isSurfaceSand(BlockState state) {
        return state.is(BlockTags.SAND)
                || state.is(com.effecoria.content.ModBlocks.PARCHED_SAND.get());
    }
}
