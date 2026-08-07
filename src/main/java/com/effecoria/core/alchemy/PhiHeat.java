package com.effecoria.core.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared Φ-heat bus: consumers query neighbors; sources publish via {@link PhiHeatSource}.
 * Burners radiate to horizontal sides and the block above (up to four consumers).
 */
public final class PhiHeat {
    private static final Direction[] CONSUMER_NEIGHBORS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    private PhiHeat() {}

    /** Strongest heat among adjacent heat sources relative to the consumer. */
    public static HeatLevel getNeighborHeat(Level level, BlockPos consumerPos) {
        HeatLevel best = HeatLevel.NONE;
        for (Direction dir : CONSUMER_NEIGHBORS) {
            BlockEntity be = level.getBlockEntity(consumerPos.relative(dir));
            if (be instanceof PhiHeatSource source) {
                best = best.max(source.heatLevel());
            }
        }
        return best;
    }

    /**
     * Drain one heat tick from the strongest adjacent source that can supply heat.
     * Prefer sources with higher {@link HeatLevel}.
     */
    public static boolean consumeNeighborHeat(ServerLevel level, BlockPos consumerPos) {
        PhiHeatSource best = null;
        HeatLevel bestLevel = HeatLevel.NONE;
        for (Direction dir : CONSUMER_NEIGHBORS) {
            BlockEntity be = level.getBlockEntity(consumerPos.relative(dir));
            if (be instanceof PhiHeatSource source) {
                HeatLevel levelHeat = source.heatLevel();
                if (levelHeat.isPresent() && levelHeat.ordinal() >= bestLevel.ordinal()) {
                    best = source;
                    bestLevel = levelHeat;
                }
            }
        }
        return best != null && best.consumeHeatTick();
    }

    public static boolean hasNeighborHeat(Level level, BlockPos consumerPos) {
        return getNeighborHeat(level, consumerPos).isPresent();
    }
}
