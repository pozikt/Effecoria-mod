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
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * BFS along {@code phi_bus} to find a supplying Heart/Spark/Forge and hop distance.
 * Touching a Heart/Forge hull part counts as touching the controller core.
 */
public final class PhiBusNetwork {
    public static final int MAX_HOPS = 64;

    public record Source(float powerFactor, int hops, @javax.annotation.Nullable PhiPowerProvider injector) {}

    private PhiBusNetwork() {}

    @javax.annotation.Nullable
    public static Source findSource(Level level, BlockPos startBus) {
        if (!level.getBlockState(startBus).is(ModBlocks.PHI_BUS.get())) {
            return null;
        }
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, Integer> dist = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startBus);
        dist.put(startBus, 0);
        visited.add(startBus);

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
                if (!level.getBlockState(next).is(ModBlocks.PHI_BUS.get())) {
                    continue;
                }
                visited.add(next);
                dist.put(next, hops + 1);
                queue.add(next);
            }
        }
        return best;
    }

    /**
     * Spark / Heart core / Forge core, or a multiblock hull part that points at its controller.
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
        float attenuated = Math.max(0.25f, injector.powerFactor() * (1f - 0.05f * hops));
        if (cur == null || attenuated > cur.powerFactor()) {
            return new Source(attenuated, hops, injector);
        }
        return cur;
    }
}
