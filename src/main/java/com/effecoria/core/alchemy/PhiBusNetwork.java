package com.effecoria.core.alchemy;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.block.HeartReactorBlockEntity;
import com.effecoria.block.SparkReactorBlockEntity;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * BFS along {@code phi_bus} to find a supplying Heart/Spark and hop distance.
 */
public final class PhiBusNetwork {
    public static final int MAX_HOPS = 64;

    public record Source(float powerFactor, int hops) {}

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
            // Adjacent power injectors (Heart / Spark)
            for (Direction dir : Direction.values()) {
                BlockPos adj = cur.relative(dir);
                BlockEntity be = level.getBlockEntity(adj);
                if (be instanceof HeartReactorBlockEntity heart && heart.supplying()) {
                    best = better(best, heart.powerFactor(), hops);
                } else if (be instanceof ForgeReactorBlockEntity forge && forge.supplying()) {
                    best = better(best, forge.powerFactor(), hops);
                } else if (be instanceof SparkReactorBlockEntity spark && spark.supplying()) {
                    best = better(best, spark.powerFactor(), hops);
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

    private static Source better(@javax.annotation.Nullable Source cur, float factor, int hops) {
        float attenuated = Math.max(0.25f, factor * (1f - 0.05f * hops));
        if (cur == null || attenuated > cur.powerFactor()) {
            return new Source(attenuated, hops);
        }
        return cur;
    }
}
