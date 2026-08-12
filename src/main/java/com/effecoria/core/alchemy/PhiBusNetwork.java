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
import com.effecoria.block.SparkReactorBlockEntity;
import com.effecoria.content.ModBlockTags;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BFS along Φ-conductors ({@code phi_bus}, mithril, …) to find a supplying injector and hop distance.
 * Mithril is a Φ-superconductor — hops across it do not attenuate. Only {@code phi_bus} cable adds hop loss.
 * Touching a Heart/Forge/Geo hull part counts as touching the controller core.
 */
public final class PhiBusNetwork {
    public static final int MAX_HOPS = 64;
    /** Attenuation per phi_bus hop (mithril does not count). */
    public static final float HOP_LOSS = 0.03f;
    public static final float MIN_ATTENUATION = 0.35f;

    public record Source(float powerFactor, int hops, @javax.annotation.Nullable PhiPowerProvider injector) {}

    private PhiBusNetwork() {}

    public static boolean isConductor(BlockState state) {
        return state.is(ModBlockTags.PHI_CONDUCTORS);
    }

    /** Superconductor cells — carry Φ without hop attenuation. */
    public static boolean isSuperconductor(BlockState state) {
        return state.is(ModBlocks.MITHRIL_BLOCK.get());
    }

    @javax.annotation.Nullable
    public static Source findSource(Level level, BlockPos start) {
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
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            int hops = dist.get(cur);
            if (hops > MAX_HOPS) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos adj = cur.relative(dir);
                PhiPowerProvider injector = resolveInjector(level, adj);
                if (injector != null && injector.supplying()) {
                    best = better(best, injector, hops);
                }
            }
            if (hops >= MAX_HOPS) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (!isConductor(nextState)) {
                    continue;
                }
                // Only resistive cable hops count; mithril is lossless.
                int nextHops = isSuperconductor(nextState) ? hops : hops + 1;
                visited.add(next);
                dist.put(next, nextHops);
                queue.add(next);
            }
        }
        return best;
    }

    /**
     * Spark / Heart core / Forge core / Geo core, or a multiblock hull part that points at its controller.
     */
    @javax.annotation.Nullable
    public static PhiPowerProvider resolveInjector(Level level, BlockPos pos) {
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
            @javax.annotation.Nullable Source cur, PhiPowerProvider injector, int hops) {
        float attenuated = Math.max(MIN_ATTENUATION, injector.powerFactor() * (1f - HOP_LOSS * hops));
        if (cur == null || attenuated > cur.powerFactor()) {
            return new Source(attenuated, hops, injector);
        }
        return cur;
    }
}
