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

/** Rare glass-stone tower with an essonite “mage statue” and loot chest. */
public final class VitrifiedMageTowerFeature extends Feature<NoneFeatureConfiguration> {
    public VitrifiedMageTowerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState stone = ModBlocks.VITRIFIED_STONE.get().defaultBlockState();
        BlockState dirt = ModBlocks.VITRIFIED_DIRT.get().defaultBlockState();
        int h = 10 + random.nextInt(6);
        int r = 2;

        for (int y = 0; y < h; y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean wall = Math.abs(dx) == r || Math.abs(dz) == r;
                    boolean door = y > 0 && y < 3 && dx == 0 && dz == -r;
                    BlockPos p = origin.offset(dx, y, dz);
                    if (wall && !door) {
                        level.setBlock(p, stone, 2);
                    } else if (!wall && y == 0) {
                        level.setBlock(p, dirt, 2);
                    } else if (!wall && y > 0 && y < h - 1) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // Roof
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                level.setBlock(origin.offset(dx, h, dz), stone, 2);
            }
        }
        // Statue
        BlockPos statue = origin.offset(0, 1, 0);
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

        BlockPos chestPos = origin.offset(1, 1, 0);
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
}
