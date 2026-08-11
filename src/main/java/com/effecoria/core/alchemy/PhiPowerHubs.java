package com.effecoria.core.alchemy;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Long-range Φ hubs (Forge) registered while supplying — avoids expanding the full PhiPower voxel scan to 32.
 */
public final class PhiPowerHubs {
    private static final Map<Level, Set<BlockPos>> HUBS = new ConcurrentHashMap<>();

    private PhiPowerHubs() {}

    public static void setActive(Level level, BlockPos pos, boolean active) {
        if (level.isClientSide()) {
            return;
        }
        Set<BlockPos> set = HUBS.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet());
        if (active) {
            set.add(pos.immutable());
        } else {
            set.remove(pos);
            if (set.isEmpty()) {
                HUBS.remove(level);
            }
        }
    }

    public static void clearLevel(Level level) {
        HUBS.remove(level);
    }

    @javax.annotation.Nullable
    public static PhiPowerProvider findBestHub(Level level, BlockPos consumerPos) {
        Set<BlockPos> set = HUBS.get(level);
        if (set == null || set.isEmpty()) {
            return null;
        }
        PhiPowerProvider best = null;
        float bestFactor = 0f;
        Iterator<BlockPos> it = set.iterator();
        while (it.hasNext()) {
            BlockPos hubPos = it.next();
            BlockEntity be = level.getBlockEntity(hubPos);
            if (!(be instanceof PhiPowerProvider provider) || !provider.supplying()) {
                it.remove();
                continue;
            }
            int cheb = Math.max(
                    Math.max(
                            Math.abs(hubPos.getX() - consumerPos.getX()),
                            Math.abs(hubPos.getY() - consumerPos.getY())),
                    Math.abs(hubPos.getZ() - consumerPos.getZ()));
            if (cheb > provider.radius()) {
                continue;
            }
            if (!PhiPower.hasLineOfSight(level, consumerPos, hubPos)) {
                continue;
            }
            float factor = provider.powerFactor();
            if (factor > bestFactor) {
                bestFactor = factor;
                best = provider;
            }
        }
        return best;
    }
}
