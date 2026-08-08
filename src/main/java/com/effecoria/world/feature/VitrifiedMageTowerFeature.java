package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Rare ruined glass-stone tower with an essonite “mage statue” and loot chest. */
public final class VitrifiedMageTowerFeature extends Feature<NoneFeatureConfiguration> {
    public VitrifiedMageTowerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        BlockPos ground = findGround(level, context.origin());
        if (ground == null || !isFlatVitrifiedPad(level, ground, 2)) {
            return false;
        }

        BlockPos base = ground.above();
        BlockState stone = ModBlocks.VITRIFIED_STONE.get().defaultBlockState();
        BlockState dirt = ModBlocks.VITRIFIED_DIRT.get().defaultBlockState();
        // Short ruin — not a tall blank 5×5 pillar
        int h = 6 + random.nextInt(4);
        int r = 2;

        for (int y = 0; y < h; y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean wall = Math.abs(dx) == r || Math.abs(dz) == r;
                    boolean door = y > 0 && y < 3 && dx == 0 && dz == -r;
                    boolean window =
                            y == 3 && ((Math.abs(dx) == r && dz == 0) || (Math.abs(dz) == r && dx == 0));
                    boolean crumbled = y >= h - 1 && wall && random.nextFloat() < 0.45f;
                    BlockPos p = base.offset(dx, y, dz);
                    if (door || window || crumbled) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    } else if (wall) {
                        level.setBlock(p, stone, 2);
                    } else if (y == 0) {
                        level.setBlock(p, dirt, 2);
                    } else {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (random.nextFloat() < 0.55f) {
                    level.setBlock(base.offset(dx, h, dz), stone, 2);
                }
            }
        }

        BlockPos statue = base.offset(0, 1, 0);
        level.setBlock(statue, ModBlocks.ESSONITE_BLOCK.get().defaultBlockState(), 2);
        level.setBlock(statue.above(), ModBlocks.ESSONITE_BLOCK.get().defaultBlockState(), 2);
        level.setBlock(
                statue.above(2),
                ModBlocks.ESSONITE_CRYSTAL.get()
                        .defaultBlockState()
                        .setValue(
                                net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                                Direction.UP),
                2);

        BlockPos chestPos = base.offset(1, 1, 0);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.PHI_CELL.get()));
            chest.setItem(1, new ItemStack(ModItems.ESSENITE_DUST.get(), 4 + random.nextInt(6)));
            chest.setItem(2, new ItemStack(ModItems.ESSONITE_SHARD.get(), 1 + random.nextInt(3)));
            if (random.nextBoolean()) {
                chest.setItem(3, new ItemStack(ModItems.PURE_ESSONITE.get()));
            }
        }
        return true;
    }

    /** Snap down from heightmap onto real vitrified ground (never onto trees). */
    private static BlockPos findGround(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int i = 0; i < 16; i++) {
            BlockState here = level.getBlockState(cursor);
            BlockState below = level.getBlockState(cursor.below());
            if (isTree(here) || isTree(below)) {
                cursor.move(Direction.DOWN);
                continue;
            }
            if ((here.canBeReplaced() || here.isAir()) && isVitrifiedGround(below)) {
                return cursor.below().immutable();
            }
            if (isVitrifiedGround(here)
                    && (level.getBlockState(cursor.above()).canBeReplaced()
                            || level.getBlockState(cursor.above()).isAir())) {
                return cursor.immutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static boolean isFlatVitrifiedPad(WorldGenLevel level, BlockPos ground, int radius) {
        int ok = 0;
        int total = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                total++;
                BlockPos p = ground.offset(dx, 0, dz);
                if (isVitrifiedGround(level.getBlockState(p))
                        || isVitrifiedGround(level.getBlockState(p.above()))
                        || isVitrifiedGround(level.getBlockState(p.below()))) {
                    ok++;
                }
            }
        }
        return ok * 2 >= total;
    }

    private static boolean isVitrifiedGround(BlockState state) {
        return state.is(ModBlocks.VITRIFIED_SAND.get())
                || state.is(ModBlocks.VITRIFIED_DIRT.get())
                || state.is(ModBlocks.VITRIFIED_STONE.get())
                || state.is(ModBlocks.ESSONITE_CRUST.get());
    }

    private static boolean isTree(BlockState state) {
        return state.is(ModBlocks.VITRIFIED_LOG.get()) || state.is(ModBlocks.VITRIFIED_BRANCHES.get());
    }
}
