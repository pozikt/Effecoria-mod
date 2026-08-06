package com.effecoria.world.feature;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
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

/** Rarest structure — glass-flash village husk with statues and a central crack. */
public final class VitrifiedFrozenVillageFeature extends Feature<NoneFeatureConfiguration> {
    public VitrifiedFrozenVillageFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState log = ModBlocks.VITRIFIED_LOG.get().defaultBlockState();
        BlockState stone = ModBlocks.VITRIFIED_STONE.get().defaultBlockState();
        BlockState dirt = ModBlocks.VITRIFIED_DIRT.get().defaultBlockState();

        // Plaza
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), dirt, 2);
                if (Math.abs(dx) > 2 || Math.abs(dz) > 2) {
                    if (random.nextFloat() < 0.08f) {
                        level.setBlock(origin.offset(dx, 0, dz), ModBlocks.ESSONITE_BLOCK.get().defaultBlockState(), 2);
                    }
                }
            }
        }
        level.setBlock(origin, ModBlocks.VITRIFIED_GEYSER_CRACK.get().defaultBlockState(), 2);

        // 3 husk houses
        placeHouse(level, origin.offset(-5, 0, -4), log, stone, random);
        placeHouse(level, origin.offset(2, 0, -5), log, stone, random);
        placeHouse(level, origin.offset(-2, 0, 3), log, stone, random);

        // Center chest
        BlockPos chestPos = origin.offset(1, 0, 1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.PHI_CELL.get()));
            chest.setItem(1, new ItemStack(ModItems.PURE_ESSONITE.get(), 1 + random.nextInt(2)));
            chest.setItem(2, new ItemStack(ModItems.ESSONITE_CRYSTAL.get(), 2));
            chest.setItem(3, new ItemStack(ModItems.RESONANCE_FOCUS.get()));
        }
        return true;
    }

    private static void placeHouse(
            WorldGenLevel level, BlockPos o, BlockState log, BlockState stone, RandomSource random) {
        int w = 4;
        int d = 4;
        int h = 3;
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                level.setBlock(o.offset(dx, 0, dz), log, 2);
                boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == d - 1;
                for (int dy = 1; dy < h; dy++) {
                    if (edge && !(dy == 1 && dx == w / 2 && dz == 0)) {
                        level.setBlock(o.offset(dx, dy, dz), stone, 2);
                    }
                }
                level.setBlock(o.offset(dx, h, dz), log, 2);
            }
        }
        // Glass statue villager
        BlockPos statue = o.offset(w / 2, 1, d / 2);
        level.setBlock(statue, ModBlocks.ESSONITE_BLOCK.get().defaultBlockState(), 2);
        if (random.nextBoolean()) {
            level.setBlock(statue.above(), ModBlocks.ESSONITE_BLOCK.get().defaultBlockState(), 2);
        }
    }
}
