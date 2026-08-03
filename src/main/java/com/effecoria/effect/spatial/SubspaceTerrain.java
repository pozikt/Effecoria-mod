package com.effecoria.effect.spatial;

import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared hyperspace floor helpers — translucent Φ veil instead of End stone. */
public final class SubspaceTerrain {
    private SubspaceTerrain() {}

    public static BlockState floorState() {
        return ModBlocks.PHI_VEIL.get().defaultBlockState();
    }

    public static boolean isHyperspaceFloor(BlockState state) {
        return state.is(ModBlocks.PHI_VEIL.get()) || state.is(Blocks.END_STONE);
    }

    /** Place / migrate the membrane under {@code feet}. Converts leftover End stone. */
    public static void ensureFloorCell(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        BlockState state = level.getBlockState(floor);
        if (state.isAir() || state.canBeReplaced() || state.is(Blocks.END_STONE)) {
            level.setBlock(floor, floorState(), 3);
        }
    }

    /** Convert End-stone leftovers around a landing so return trips don't show End rock. */
    public static void sanitizeAround(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(cursor).is(Blocks.END_STONE)) {
                        level.setBlock(cursor, floorState(), 3);
                    }
                }
            }
        }
    }
}
