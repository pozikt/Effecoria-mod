package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lines cave walls with essonite / Φ-stone under Essence Plateau mountains,
 * forming a connected crystalline cavern network down the column.
 */
public final class PhiCaveShellFeature extends Feature<NoneFeatureConfiguration> {
    public PhiCaveShellFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockState phiStone = ModBlocks.PHI_STONE.get().defaultBlockState();
        BlockState ore = ModBlocks.ESSENITE_ORE.get().defaultBlockState();
        BlockState deepOre = ModBlocks.DEEPSLATE_ESSENITE_ORE.get().defaultBlockState();
        BlockState core = ModBlocks.ESSONITE_BLOCK.get().defaultBlockState();

        boolean placed = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radius = 5 + random.nextInt(4);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!isReplaceableStone(state)) {
                        continue;
                    }
                    if (!touchesOpen(level, cursor)) {
                        // Deep mass under the mountain — still convert, but keep Φ-stone core.
                        if (random.nextFloat() < 0.35f) {
                            level.setBlock(cursor, pickMass(state, phiStone, ore, deepOre, core, random, cursor.getY()), 2);
                            placed = true;
                        }
                        continue;
                    }
                    // Cave shell: prefer glowing essonite veins on open faces.
                    BlockState shell;
                    if (cursor.getY() <= 0) {
                        shell = random.nextFloat() < 0.7f ? core : deepOre;
                    } else {
                        shell = random.nextFloat() < 0.65f ? ore : phiStone;
                    }
                    level.setBlock(cursor, shell, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private static boolean isReplaceableStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(ModBlocks.PHI_STONE.get());
    }

    private static boolean touchesOpen(WorldGenLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (neighbor.isAir() || neighbor.is(Blocks.WATER) || neighbor.is(Blocks.LAVA) || neighbor.is(Blocks.CAVE_AIR)) {
                return true;
            }
        }
        return false;
    }

    private static BlockState pickMass(
            BlockState current,
            BlockState phiStone,
            BlockState ore,
            BlockState deepOre,
            BlockState core,
            RandomSource random,
            int y) {
        if (y <= 0) {
            return random.nextFloat() < 0.82f ? core : (random.nextFloat() < 0.5f ? deepOre : phiStone);
        }
        if (current.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || current.is(Blocks.DEEPSLATE)) {
            return random.nextFloat() < 0.55f ? deepOre : phiStone;
        }
        return random.nextFloat() < 0.25f ? ore : phiStone;
    }
}
