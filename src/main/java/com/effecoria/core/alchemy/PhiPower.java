package com.effecoria.core.alchemy;

import com.effecoria.content.ModBlocks;
import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.circuit.PhiChannels;
import com.effecoria.core.formula.FormulaEngine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
     * Returns false if no provider or drain fails. Tries the next-best source if the first
     * cannot cover the load (e.g. a nearly-empty accumulator).
     */
    public static boolean consumeTick(Level level, BlockPos consumerPos, int loadTicks) {
        if (loadTicks <= 0) {
            return hasPower(level, consumerPos);
        }
        PhiChannel device = PhiChannels.ofDevice(level, consumerPos);
        for (PhiPowerProvider candidate : findCandidates(level, consumerPos)) {
            if (!candidate.supplying()) {
                continue;
            }
            PhiChannel source = channelOf(level, consumerPos, candidate);
            float resonance = FormulaEngine.phiFlowResonance(source.hz(), device.hz());
            if (resonance <= 0.05f) {
                PhiChannels.leakOmega(level, consumerPos, loadTicks);
                continue;
            }
            int drain = loadTicks;
            if (resonance < 0.99f) {
                drain = Math.max(loadTicks, Mth.ceil(loadTicks / Math.max(0.2f, resonance)));
                PhiChannels.leakOmega(level, consumerPos, FormulaEngine.phiOmegaLeak(drain, resonance));
            }
            if (candidate.drainFuel(drain)) {
                return true;
            }
        }
        return false;
    }

    private static java.util.List<PhiPowerProvider> findCandidates(Level level, BlockPos consumerPos) {
        java.util.ArrayList<PhiPowerProvider> out = new java.util.ArrayList<>();
        java.util.HashSet<PhiPowerProvider> seen = new java.util.HashSet<>();

        // Hardwired terminals: any adjacent Φ-conductor (bus, mithril, coupler, contactor,
        // accumulator) relays the island injector. Coupler/contactor are NOT PhiPowerProviders,
        // so without this a machine touching only a coupler gets nothing from a wired reactor.
        for (Direction dir : Direction.values()) {
            addWiredInjector(level, consumerPos.relative(dir), seen, out);
        }
        for (BlockPos far : com.effecoria.core.circuit.PhiFilamentLinks.neighbors(level, consumerPos)) {
            addWiredInjector(level, far, seen, out);
        }

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
                    if (chebyshev > 1 && !hasLineOfSight(level, consumerPos, cursor.immutable())) {
                        continue;
                    }
                    if (seen.add(provider)) {
                        out.add(provider);
                    }
                }
            }
        }
        PhiPowerProvider hub = PhiPowerHubs.findBestHub(level, consumerPos);
        if (hub != null && hub.supplying() && seen.add(hub)) {
            out.add(hub);
        }
        // Prefer live injectors over bus outlets / UPS buffers so a charged accumulator
        // next to a machine does not win the sort and empty/recharge (LIT flicker).
        out.sort((a, b) -> {
            int cmp = Integer.compare(providerTier(b), providerTier(a));
            return cmp != 0 ? cmp : Float.compare(b.powerFactor(), a.powerFactor());
        });
        return out;
    }

    private static int providerTier(PhiPowerProvider provider) {
        if (provider instanceof com.effecoria.block.PhiAccumulatorBlockEntity) {
            return 0;
        }
        if (provider instanceof com.effecoria.block.PhiBusBlockEntity) {
            return 1;
        }
        return 2;
    }

    private static void addWiredInjector(
            Level level,
            BlockPos terminal,
            java.util.HashSet<PhiPowerProvider> seen,
            java.util.ArrayList<PhiPowerProvider> out) {
        if (!PhiBusNetwork.isConductor(level.getBlockState(terminal))) {
            return;
        }
        PhiBusNetwork.Source wired = PhiBusNetwork.findSource(level, terminal);
        if (wired != null && wired.injector() != null && wired.injector().supplying() && seen.add(wired.injector())) {
            out.add(wired.injector());
        }
    }

    private static PhiChannel channelOf(Level level, BlockPos consumerPos, PhiPowerProvider best) {
        if (best instanceof com.effecoria.core.circuit.PhiTuned tuned) {
            PhiChannel ch = tuned.phiChannel();
            if (ch != PhiChannel.BROADBAND) {
                return ch;
            }
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = consumerPos.relative(dir);
            if (PhiBusNetwork.isConductor(level.getBlockState(adj))) {
                return PhiBusNetwork.channelAt(level, adj);
            }
        }
        for (BlockPos far : com.effecoria.core.circuit.PhiFilamentLinks.neighbors(level, consumerPos)) {
            if (PhiBusNetwork.isConductor(level.getBlockState(far))) {
                return PhiBusNetwork.channelAt(level, far);
            }
        }
        return PhiChannel.BROADBAND;
    }

    @javax.annotation.Nullable
    public static PhiPowerProvider findBest(Level level, BlockPos consumerPos) {
        java.util.List<PhiPowerProvider> candidates = findCandidates(level, consumerPos);
        return candidates.isEmpty() ? null : candidates.get(0);
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
            if (state.is(ModBlocks.HEART_REACTOR_PART.get())
                    || state.is(ModBlocks.FORGE_REACTOR_PART.get())
                    || state.is(ModBlocks.GEO_WELL_PART.get())) {
                continue;
            }
            // Conductors are Φ-transparent for wireless LOS through cabling / mithril frames.
            if (state.is(ModBlocks.PHI_BUS.get())
                    || state.is(ModBlocks.MITHRIL_BLOCK.get())
                    || state.is(ModBlocks.PHI_CONTACTOR.get())
                    || state.is(ModBlocks.PHI_COUPLER.get())
                    || state.is(ModBlocks.PHI_ACCUMULATOR.get())) {
                continue;
            }
            if (state.canOcclude() && state.isCollisionShapeFullBlock(level, check)) {
                return false;
            }
        }
        return true;
    }
}
