package com.effecoria.content;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Natural spawn predicates so Effecoria fauna can appear on Φ / glass terrain.
 * Do not call {@link Mob#checkMobSpawnRules} for glowing biomes — surface blocks emit
 * light, which makes vanilla monster darkness checks always fail.
 */
public final class EffecoriaMobSpawns {
    private EffecoriaMobSpawns() {}

    public static boolean phiLarva(
            EntityType<? extends Mob> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (!level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU)) {
            return false;
        }
        if (!MobSpawnType.ignoresLightRequirements(reason) && level.getRawBrightness(pos, 0) < 7) {
            return false;
        }
        return isPlateauGround(level.getBlockState(pos.below())) && spaceClear(level, pos);
    }

    public static boolean eidos(
            EntityType<? extends Mob> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (!level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU)) {
            return false;
        }
        if (level.getBlockState(pos).is(Blocks.WATER)) {
            return false;
        }
        return solidEnough(level, pos.below()) && spaceClear(level, pos);
    }

    /** Neutral cave/surface crab — day or night on the plateau. */
    public static boolean crystalCrab(
            EntityType<? extends Mob> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (!level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU)) {
            return false;
        }
        return solidEnough(level, pos.below()) && spaceClear(level, pos);
    }

    /** Apex flyer — rare, any light, plateau peaks preferred. */
    public static boolean essenceWyvern(
            EntityType<? extends Mob> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (!level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU)) {
            return false;
        }
        if (pos.getY() < level.getSeaLevel() + 20 && random.nextFloat() < 0.75f) {
            return false;
        }
        return spaceClear(level, pos);
    }

    /** Glass wasteland sentinel — day or night on vitrified ground. */
    public static boolean vitrifiedGolem(
            EntityType<? extends Mob> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        if (!level.getBiome(pos).is(ModBiomeTags.VITRIFIED_WASTES)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(ModBlocks.VITRIFIED_SAND.get())
                || below.is(ModBlocks.VITRIFIED_DIRT.get())
                || below.is(ModBlocks.VITRIFIED_STONE.get())
                || below.is(ModBlocks.ESSONITE_CRUST.get())
                || below.is(BlockTags.VALID_SPAWN)
                || solidEnough(level, pos.below());
        return ground && spaceClear(level, pos);
    }

    private static boolean isPlateauGround(BlockState below) {
        return below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(ModBlocks.PHI_GRASS.get())
                || below.is(ModBlocks.PHI_DIRT.get())
                || below.is(ModBlocks.PHI_STONE.get())
                || below.is(ModBlocks.ESSONITE_CRUST.get());
    }

    private static boolean solidEnough(ServerLevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos)
                || state.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || state.is(BlockTags.VALID_SPAWN);
    }

    private static boolean spaceClear(ServerLevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
}
