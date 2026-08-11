package com.effecoria.core.alchemy;

import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wireless Φ-power bus: consumers query nearby {@link PhiPowerProvider}s with line-of-sight.
 */
public final class PhiPower {
    /** Max Chebyshev scan — must cover Heart radius (8). Providers still clamp to their own radius(). */
    private static final int SCAN_RADIUS = 8;

    private PhiPower() {}

    public static boolean hasPower(Level level, BlockPos consumerPos) {
        return powerFactor(level, consumerPos) > 0f;
    }

    /** Strongest power factor among LOS providers within scan range; 0 if none. */
    public static float powerFactor(Level level, BlockPos consumerPos) {
        PhiPowerProvider best = findBest(level, consumerPos);
        return best != null ? best.powerFactor() : 0f;
    }

    /** Drain 1 fuel tick from the best provider (or false if none / insufficient). */
    public static boolean consumeTick(Level level, BlockPos consumerPos) {
        return consumeTick(level, consumerPos, 1);
    }

    /**
     * Drain {@code loadTicks} from the strongest LOS provider (or Forge hub / bus injector).
     * Returns false if no provider or drain fails.
     */
    public static boolean consumeTick(Level level, BlockPos consumerPos, int loadTicks) {
        if (loadTicks <= 0) {
            return hasPower(level, consumerPos);
        }
        PhiPowerProvider best = findBest(level, consumerPos);
        return best != null && best.drainFuel(loadTicks);
    }

    @javax.annotation.Nullable
    public static PhiPowerProvider findBest(Level level, BlockPos consumerPos) {
        PhiPowerProvider best = null;
        float bestFactor = 0f;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cx = consumerPos.getX();
        int cy = consumerPos.getY();
        int cz = consumerPos.getZ();
        for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(cx + dx, cy + dy, cz + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (!(be instanceof PhiPowerProvider provider) || !provider.supplying()) {
                        continue;
                    }
                    int chebyshev = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (chebyshev > provider.radius()) {
                        continue;
                    }
                    // Adjacent (incl. bus outlets) is always hardwired — no LOS required.
                    if (chebyshev > 1 && !hasLineOfSight(level, consumerPos, cursor.immutable())) {
                        continue;
                    }
                    float factor = provider.powerFactor();
                    if (factor > bestFactor) {
                        bestFactor = factor;
                        best = provider;
                    }
                }
            }
        }
        PhiPowerProvider hub = PhiPowerHubs.findBestHub(level, consumerPos);
        if (hub != null && hub.powerFactor() > bestFactor) {
            best = hub;
        }
        return best;
    }

    /**
     * Voxel walk from consumer toward provider; opaque full cubes between endpoints block power.
     * Assembled Heart/Forge hull parts are Φ-transparent so cores can radiate through their shell.
     * Endpoints themselves are not checked.
     */
    public static boolean hasLineOfSight(BlockGetter level, BlockPos from, BlockPos to) {
        if (from.equals(to)) {
            return true;
        }
        double x0 = from.getX() + 0.5;
        double y0 = from.getY() + 0.5;
        double z0 = from.getZ() + 0.5;
        double x1 = to.getX() + 0.5;
        double y1 = to.getY() + 0.5;
        double z1 = to.getZ() + 0.5;
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        int steps = Math.max(1, Mth.ceil(Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) * 2.0));
        BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos();
        for (int i = 1; i < steps; i++) {
            double t = (double) i / (double) steps;
            int bx = Mth.floor(x0 + dx * t);
            int by = Mth.floor(y0 + dy * t);
            int bz = Mth.floor(z0 + dz * t);
            if ((bx == from.getX() && by == from.getY() && bz == from.getZ())
                    || (bx == to.getX() && by == to.getY() && bz == to.getZ())) {
                continue;
            }
            check.set(bx, by, bz);
            BlockState state = level.getBlockState(check);
            if (state.is(ModBlocks.HEART_REACTOR_PART.get()) || state.is(ModBlocks.FORGE_REACTOR_PART.get())) {
                continue;
            }
            if (state.canOcclude() && state.isCollisionShapeFullBlock(level, check)) {
                return false;
            }
        }
        return true;
    }
}
