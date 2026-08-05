package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small crystal pocket under the Glass Plain crust. */
public final class GlassPlainCrystalCaveFeature extends Feature<NoneFeatureConfiguration> {
    public GlassPlainCrystalCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        int depth = 8 + random.nextInt(18);
        BlockPos center = new BlockPos(origin.getX(), Math.max(level.getMinBuildHeight() + 8, surface - depth), origin.getZ());

        int rx = 2 + random.nextInt(3);
        int ry = 2 + random.nextInt(2);
        int rz = 2 + random.nextInt(3);
        BlockState quartz = ModBlocks.PHI_QUARTZ.get().defaultBlockState();
        BlockState crystal = ModBlocks.ESSONITE_CRYSTAL.get().defaultBlockState();
        BlockState wall = ModBlocks.ESSONITE_DRIPSTONE_BLOCK.get().defaultBlockState();
        boolean carved = false;

        for (int dx = -rx - 1; dx <= rx + 1; dx++) {
            for (int dy = -ry - 1; dy <= ry + 1; dy++) {
                for (int dz = -rz - 1; dz <= rz + 1; dz++) {
                    double nx = dx / (double) Math.max(1, rx);
                    double ny = dy / (double) Math.max(1, ry);
                    double nz = dz / (double) Math.max(1, rz);
                    double d = nx * nx + ny * ny + nz * nz;
                    BlockPos p = center.offset(dx, dy, dz);
                    if (d <= 1.0) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        carved = true;
                    } else if (d <= 1.45 && !level.getBlockState(p).isAir()) {
                        level.setBlock(p, random.nextFloat() < 0.25f ? quartz : wall, 2);
                    }
                }
            }
        }

        if (!carved) {
            return false;
        }

        // Floor crystals
        for (int i = 0; i < 4 + random.nextInt(5); i++) {
            BlockPos p = center.offset(
                    random.nextInt(rx * 2 + 1) - rx,
                    -ry,
                    random.nextInt(rz * 2 + 1) - rz);
            if (level.getBlockState(p).isAir()
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                level.setBlock(
                        p,
                        crystal.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.UP),
                        2);
            }
        }

        // Ceiling drips
        for (int i = 0; i < 3 + random.nextInt(4); i++) {
            BlockPos p = center.offset(
                    random.nextInt(rx * 2 + 1) - rx,
                    ry,
                    random.nextInt(rz * 2 + 1) - rz);
            if (level.getBlockState(p).isAir()
                    && level.getBlockState(p.above()).isFaceSturdy(level, p.above(), Direction.DOWN)) {
                BlockState pointed = ModBlocks.ESSONITE_POINTED.get()
                        .defaultBlockState()
                        .setValue(com.effecoria.block.EssonitePointedBlock.THICKNESS, DripstoneThickness.TIP)
                        .setValue(com.effecoria.block.EssonitePointedBlock.TIP_DIRECTION, Direction.DOWN);
                level.setBlock(p, pointed, 2);
            }
        }
        return true;
    }
}
