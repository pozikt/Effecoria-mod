package com.effecoria.core.alchemy;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.block.ForgeReactorPartBlockEntity;
import com.effecoria.block.GeoWellBlockEntity;
import com.effecoria.block.GeoWellPartBlockEntity;
import com.effecoria.block.HeartReactorBlockEntity;
import com.effecoria.block.HeartReactorPartBlockEntity;
import com.effecoria.block.PhiAccumulatorBlockEntity;
import com.effecoria.block.PhiContactorBlock;
import com.effecoria.block.PhiCouplerBlockEntity;
import com.effecoria.block.SparkReactorBlockEntity;
import com.effecoria.content.ModBlockTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.circuit.PhiFilamentLinks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BFS along Φ-conductors ({@code phi_bus}, mithril, circuitry, filaments) to find a supplying injector.
 * Open contactors break the graph. Mithril and filaments do not attenuate; {@code phi_bus} hops do.
 */
public final class PhiBusNetwork {
    public static final int MAX_HOPS = 64;
    /** Attenuation per phi_bus hop (mithril / filament do not count). */
    public static final float HOP_LOSS = 0.03f;
    public static final float MIN_ATTENUATION = 0.35f;

    public record Source(
            float powerFactor,
            int hops,
            @javax.annotation.Nullable PhiPowerProvider injector,
            PhiChannel channel) {
        public Source(float powerFactor, int hops, @javax.annotation.Nullable PhiPowerProvider injector) {
            this(powerFactor, hops, injector, PhiChannel.BROADBAND);
        }
    }

    private PhiBusNetwork() {}

    public static boolean isConductor(BlockState state) {
        if (state.is(ModBlocks.PHI_CONTACTOR.get()) && !state.getValue(PhiContactorBlock.CLOSED)) {
            return false;
        }
        return state.is(ModBlockTags.PHI_CONDUCTORS);
    }

    /** Superconductor cells — carry Φ without hop attenuation. */
    public static boolean isSuperconductor(BlockState state) {
        return state.is(ModBlocks.MITHRIL_BLOCK.get());
    }

    @javax.annotation.Nullable
    public static Source findSource(Level level, BlockPos start) {
        return findSource(level, start, true);
    }

    @javax.annotation.Nullable
    public static Source findSource(Level level, BlockPos start, boolean includeBuffers) {
        if (!isConductor(level.getBlockState(start))) {
            return null;
        }
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> dist = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        dist.put(start, 0);
        visited.add(start);

        Source best = null;
        PhiChannel island = PhiChannel.BROADBAND;
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            int hops = dist.get(cur);
            if (hops > MAX_HOPS) {
                continue;
            }
            // Read channel from coupler blockstate only — never call PhiTuned.phiChannel()
            // here: accumulators (and any Tuned that BFS's) would recurse into findSource.
            if (island == PhiChannel.BROADBAND) {
                BlockState curState = level.getBlockState(cur);
                if (curState.is(ModBlocks.PHI_COUPLER.get())) {
                    PhiChannel stamped = curState.getValue(com.effecoria.block.PhiCouplerBlock.CHANNEL);
                    if (stamped != PhiChannel.BROADBAND) {
                        island = stamped;
                    }
                }
            }
            PhiPowerProvider self = resolveInjector(level, cur, includeBuffers);
            if (self != null && self.supplying()) {
                best = better(best, self, hops, island);
            }
            for (Direction dir : Direction.values()) {
                BlockPos adj = cur.relative(dir);
                PhiPowerProvider injector = resolveInjector(level, adj, includeBuffers);
                if (injector != null && injector.supplying()) {
                    best = better(best, injector, hops, island);
                }
            }
            if (hops >= MAX_HOPS) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                tryEnqueue(level, queue, dist, visited, cur.relative(dir), hops);
            }
            for (BlockPos far : PhiFilamentLinks.neighbors(level, cur)) {
                tryEnqueueFilament(queue, dist, visited, far, hops);
            }
        }
        if (best == null) {
            return null;
        }
        return new Source(best.powerFactor(), best.hops(), best.injector(), island);
    }

    private static void tryEnqueue(
            Level level,
            Queue<BlockPos> queue,
            Map<BlockPos, Integer> dist,
            Set<BlockPos> visited,
            BlockPos next,
            int hops) {
        if (visited.contains(next)) {
            return;
        }
        BlockState nextState = level.getBlockState(next);
        if (!isConductor(nextState)) {
            return;
        }
        int nextHops = isSuperconductor(nextState) ? hops : hops + 1;
        visited.add(next);
        dist.put(next, nextHops);
        queue.add(next);
    }

    private static void tryEnqueueFilament(
            Queue<BlockPos> queue,
            Map<BlockPos, Integer> dist,
            Set<BlockPos> visited,
            BlockPos next,
            int hops) {
        if (visited.contains(next)) {
            return;
        }
        visited.add(next);
        dist.put(next, hops);
        queue.add(next);
    }

    /**
     * Spark / Heart / Forge / Geo cores, hull parts, or a charged Φ-accumulator.
     */
    @javax.annotation.Nullable
    public static PhiPowerProvider resolveInjector(Level level, BlockPos pos) {
        return resolveInjector(level, pos, true);
    }

    @javax.annotation.Nullable
    public static PhiPowerProvider resolveInjector(Level level, BlockPos pos, boolean includeBuffers) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SparkReactorBlockEntity spark) {
            return spark;
        }
        if (be instanceof HeartReactorBlockEntity heart) {
            return heart;
        }
        if (be instanceof ForgeReactorBlockEntity forge) {
            return forge;
        }
        if (be instanceof GeoWellBlockEntity geo) {
            return geo;
        }
        if (includeBuffers && be instanceof PhiAccumulatorBlockEntity acc) {
            return acc;
        }
        if (be instanceof HeartReactorPartBlockEntity part && part.getControllerPos() != null) {
            BlockEntity core = level.getBlockEntity(part.getControllerPos());
            if (core instanceof HeartReactorBlockEntity heart) {
                return heart;
            }
        }
        if (be instanceof ForgeReactorPartBlockEntity part && part.getControllerPos() != null) {
            BlockEntity core = level.getBlockEntity(part.getControllerPos());
            if (core instanceof ForgeReactorBlockEntity forge) {
                return forge;
            }
        }
        if (be instanceof GeoWellPartBlockEntity part && part.getControllerPos() != null) {
            BlockEntity core = level.getBlockEntity(part.getControllerPos());
            if (core instanceof GeoWellBlockEntity geo) {
                return geo;
            }
        }
        return null;
    }

    private static Source better(
            @javax.annotation.Nullable Source cur,
            PhiPowerProvider injector,
            int hops,
            PhiChannel channel) {
        float attenuated = Math.max(MIN_ATTENUATION, injector.powerFactor() * (1f - HOP_LOSS * hops));
        if (cur == null || attenuated > cur.powerFactor()) {
            return new Source(attenuated, hops, injector, channel);
        }
        return cur;
    }

    /** Island channel at a conductor (or BROADBAND). */
    public static PhiChannel channelAt(Level level, BlockPos start) {
        Source source = findSource(level, start);
        return source == null ? PhiChannel.BROADBAND : source.channel();
    }

    public static void noteCoupler(PhiCouplerBlockEntity ignored) {
        // hook reserved for island dirtying
    }
}
