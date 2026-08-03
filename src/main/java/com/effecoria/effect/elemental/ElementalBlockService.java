package com.effecoria.effect.elemental;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Restores blocks after temporary elemental constructs (ice walls) expire. */
public final class ElementalBlockService {
    private static final Map<BlockPos, RestoreEntry> RESTORES = new ConcurrentHashMap<>();
    private static final List<PendingPlacement> PENDING = new CopyOnWriteArrayList<>();

    private ElementalBlockService() {}

    public record RestoreEntry(ServerLevel level, BlockState original, long restoreAt) {}

    private record PendingPlacement(ServerLevel level, BlockPos pos, BlockState state, int durationTicks, long placeAt) {}

    public static boolean placeTemporary(ServerLevel level, BlockPos pos, BlockState replacement, int durationTicks) {
        BlockState current = level.getBlockState(pos);
        if (!current.canBeReplaced() && !current.isAir()) {
            return false;
        }
        BlockState original = current.isAir() ? null : current;
        level.setBlock(pos, replacement, 3);
        long restoreAt = level.getGameTime() + durationTicks;
        RESTORES.put(pos.immutable(), new RestoreEntry(level, original, restoreAt));
        return true;
    }

    /** Immediately restore a temporary block (e.g. when a moving water shell relocates). */
    public static void restoreNow(ServerLevel level, BlockPos pos) {
        RestoreEntry restore = RESTORES.remove(pos.immutable());
        if (restore == null || restore.level() != level) {
            return;
        }
        if (restore.original() == null || restore.original().isAir()) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, restore.original(), 3);
        }
    }

    public static void restoreNow(ServerLevel level, Iterable<BlockPos> positions) {
        for (BlockPos pos : positions) {
            restoreNow(level, pos);
        }
    }

    /** Queue a block to appear later — used for rising ice walls. */
    public static void scheduleTemporary(
            ServerLevel level, BlockPos pos, BlockState replacement, int durationTicks, long placeAtGameTime) {
        PENDING.add(new PendingPlacement(level, pos.immutable(), replacement, durationTicks, placeAtGameTime));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        PENDING.removeIf(pending -> {
            if (pending.level() != level || now < pending.placeAt()) {
                return false;
            }
            if (placeTemporary(level, pending.pos(), pending.state(), pending.durationTicks())) {
                level.sendParticles(
                        ModParticleTypes.ICE_CRYSTAL.get(),
                        pending.pos().getX() + 0.5,
                        pending.pos().getY() + 0.5,
                        pending.pos().getZ() + 0.5,
                        2,
                        0.12,
                        0.12,
                        0.12,
                        0.015);
            }
            return true;
        });

        Iterator<Map.Entry<BlockPos, RestoreEntry>> it = RESTORES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, RestoreEntry> entry = it.next();
            RestoreEntry restore = entry.getValue();
            if (restore.level() != level) {
                continue;
            }
            if (now < restore.restoreAt()) {
                continue;
            }
            BlockPos pos = entry.getKey();
            if (restore.original() == null || restore.original().isAir()) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, restore.original(), 3);
            }
            it.remove();
        }
    }

    /** Sort bottom-up, then center-out for a natural wall rise. */
    public static List<BlockPos> risingOrder(List<BlockPos> blocks, BlockPos center) {
        List<BlockPos> ordered = new ArrayList<>(blocks);
        ordered.sort(Comparator
                .comparingInt((BlockPos pos) -> pos.getY())
                .thenComparingDouble(pos -> pos.distSqr(center)));
        return ordered;
    }
}
