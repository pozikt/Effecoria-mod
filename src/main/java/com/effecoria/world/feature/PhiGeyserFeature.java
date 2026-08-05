package com.effecoria.world.feature;

import com.effecoria.block.PhiGeyserBlock;
import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places a Φ-geyser crack with void-obsidian glazed rim on open plateau surface.
 */
public final class PhiGeyserFeature extends Feature<NoneFeatureConfiguration> {
    public PhiGeyserFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockPos surface = origin;
        if (!level.getBlockState(surface).canBeReplaced() && !level.isEmptyBlock(surface)) {
            surface = origin.above();
        }
        BlockPos ground = surface.below();
        BlockState groundState = level.getBlockState(ground);
        if (!isPlateauGround(groundState)) {
            return false;
        }
        if (!level.getBlockState(surface).canBeReplaced() && !level.isEmptyBlock(surface)) {
            return false;
        }

        Direction facing = random.nextBoolean() ? Direction.NORTH : Direction.EAST;
        BlockState geyser = ModBlocks.PHI_GEYSER
                .get()
                .defaultBlockState()
                .setValue(PhiGeyserBlock.FACING, facing);

        level.setBlock(surface, geyser, 2);

        BlockState rim = ModBlocks.VOID_OBSIDIAN.get().defaultBlockState();
        // Elongated rim along the crack axis
        if (facing.getAxis() == Direction.Axis.Z) {
            placeRim(level, surface.offset(-1, -1, 0), rim);
            placeRim(level, surface.offset(1, -1, 0), rim);
            placeRim(level, surface.offset(-1, -1, -1), rim);
            placeRim(level, surface.offset(1, -1, -1), rim);
            placeRim(level, surface.offset(-1, -1, 1), rim);
            placeRim(level, surface.offset(1, -1, 1), rim);
            placeRim(level, surface.offset(0, -1, -2), rim);
            placeRim(level, surface.offset(0, -1, 2), rim);
        } else {
            placeRim(level, surface.offset(0, -1, -1), rim);
            placeRim(level, surface.offset(0, -1, 1), rim);
            placeRim(level, surface.offset(-1, -1, -1), rim);
            placeRim(level, surface.offset(-1, -1, 1), rim);
            placeRim(level, surface.offset(1, -1, -1), rim);
            placeRim(level, surface.offset(1, -1, 1), rim);
            placeRim(level, surface.offset(-2, -1, 0), rim);
            placeRim(level, surface.offset(2, -1, 0), rim);
        }
        // Ensure footing under geyser is Φ-stone
        level.setBlock(ground, ModBlocks.PHI_STONE.get().defaultBlockState(), 2);
        return true;
    }

    private static boolean isPlateauGround(BlockState state) {
        return state.is(ModBlocks.PHI_STONE.get())
                || state.is(ModBlocks.PHI_DIRT.get())
                || state.is(ModBlocks.PHI_GRASS.get())
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT);
    }

    private static void placeRim(WorldGenLevel level, BlockPos pos, BlockState rim) {
        BlockState current = level.getBlockState(pos);
        if (current.isAir() || current.canBeReplaced()) {
            return;
        }
        if (isPlateauGround(current) || current.is(ModBlocks.PHI_STONE.get())) {
            level.setBlock(pos, rim, 2);
        }
    }
}
