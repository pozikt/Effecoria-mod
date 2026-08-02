package com.effecoria.effect.spatial;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Spatial sonar: BFS through connected void (caves / tunnels), recording cavity walls.
 * Sense strength drops every {@code attenuateEvery} blocks of path distance.
 */
public final class SpatialSenseService {
    public static final byte KIND_WALL = 0;
    public static final byte KIND_VOID = 1;
    public static final byte KIND_TRAP = 2;

    public record Hit(int dx, int dy, int dz, byte strength, byte kind) {}

    public record ScanResult(BlockPos origin, List<Hit> hits, int cavities, int traps, int maxReach) {}

    private SpatialSenseService() {}

    public static ScanResult scan(ServerLevel level, BlockPos origin, int maxRange, int attenuateEvery) {
        int range = Mth.clamp(maxRange, 8, 64);
        int step = Math.max(1, attenuateEvery);
        Map<Long, Hit> best = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<long[]> queue = new ArrayDeque<>();

        BlockPos start = findStartAir(level, origin);
        long startKey = start.asLong();
        visited.add(startKey);
        queue.add(new long[] {startKey, 0L});

        int cavities = 0;
        int traps = 0;
        int maxReach = 0;
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

        while (!queue.isEmpty()) {
            long[] node = queue.poll();
            BlockPos pos = BlockPos.of(node[0]);
            int dist = (int) node[1];
            maxReach = Math.max(maxReach, dist);

            float intensity = intensityAt(dist, step);
            if (intensity <= 0.02f) {
                continue;
            }
            byte strength = (byte) Mth.clamp(Math.round(intensity * 100f), 1, 100);

            BlockState state = level.getBlockState(pos);
            if (isTrap(state)) {
                traps++;
                putBest(best, pos.subtract(start), strength, KIND_TRAP);
            }

            int solidFaces = 0;
            for (Direction dir : Direction.values()) {
                neighbor.setWithOffset(pos, dir);
                BlockState beside = level.getBlockState(neighbor);
                if (isSolidSense(beside, level, neighbor)) {
                    solidFaces++;
                    // Wall of the cavity — the solid shell the sonar "sees".
                    putBest(best, neighbor.immutable().subtract(start), strength, KIND_WALL);
                } else if (isPassable(beside, level, neighbor) && isCaveContext(level, neighbor, start)) {
                    long nKey = neighbor.asLong();
                    if (dist + 1 <= range && visited.add(nKey)) {
                        queue.add(new long[] {nKey, dist + 1L});
                    }
                }
            }

            // Enclosed air counts as cavity volume; sparse void samples for sonar motes.
            if (solidFaces >= 3) {
                cavities++;
                if ((pos.getX() + pos.getY() * 3 + pos.getZ() * 7) % sampleStride(dist) == 0) {
                    putBest(best, pos.subtract(start), strength, KIND_VOID);
                }
            }
        }

        List<Hit> hits = prioritize(best, 800);
        return new ScanResult(start, hits, cavities, traps, maxReach);
    }

    public static void sendTo(ServerPlayer player, ScanResult result, int durationTicks) {
        PacketDistributor.sendToPlayer(
                player,
                new ModNetworking.SpatialSensePayload(
                        result.origin().getX(),
                        result.origin().getY(),
                        result.origin().getZ(),
                        durationTicks,
                        result.hits()));
    }

    /** Strength after each full 5-block band of path distance. */
    public static float intensityAt(int pathDist, int attenuateEvery) {
        int bands = Math.max(0, pathDist) / Math.max(1, attenuateEvery);
        // Gentle fade so a larger cave network still reads faintly at the edge.
        return Math.max(0.06f, 1f - bands * 0.12f);
    }

    private static int sampleStride(int dist) {
        // Farther void samples rarer — sonar thins out.
        if (dist < 5) {
            return 5;
        }
        if (dist < 10) {
            return 7;
        }
        if (dist < 15) {
            return 11;
        }
        return 17;
    }

    private static void putBest(Map<Long, Hit> best, BlockPos rel, byte strength, byte kind) {
        int dx = Mth.clamp(rel.getX(), -127, 127);
        int dy = Mth.clamp(rel.getY(), -127, 127);
        int dz = Mth.clamp(rel.getZ(), -127, 127);
        long key = BlockPos.asLong(dx, dy, dz);
        Hit existing = best.get(key);
        if (existing == null
                || strength > existing.strength()
                || (strength == existing.strength() && kindPriority(kind) > kindPriority(existing.kind()))) {
            best.put(key, new Hit(dx, dy, dz, strength, kind));
        }
    }

    private static int kindPriority(byte kind) {
        return switch (kind) {
            case KIND_TRAP -> 3;
            case KIND_WALL -> 2;
            default -> 1;
        };
    }

    private static List<Hit> prioritize(Map<Long, Hit> best, int cap) {
        ArrayList<Hit> list = new ArrayList<>(best.values());
        list.sort((a, b) -> {
            int cmp = Byte.compare(b.strength(), a.strength());
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(kindPriority(b.kind()), kindPriority(a.kind()));
        });
        if (list.size() > cap) {
            return new ArrayList<>(list.subList(0, cap));
        }
        return list;
    }

    private static BlockPos findStartAir(ServerLevel level, BlockPos origin) {
        if (isPassable(level.getBlockState(origin), level, origin)) {
            return origin.immutable();
        }
        BlockPos above = origin.above();
        if (isPassable(level.getBlockState(above), level, above)) {
            return above;
        }
        return origin.immutable();
    }

    private static boolean isPassable(BlockState state, ServerLevel level, BlockPos pos) {
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isSolidSense(BlockState state, ServerLevel level, BlockPos pos) {
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Keep the ping inside cave networks: do not flood open sky.
     * Surface air is only followed when it still hugs terrain.
     */
    private static boolean isCaveContext(ServerLevel level, BlockPos pos, BlockPos origin) {
        if (!level.canSeeSky(pos)) {
            return true;
        }
        int solids = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            cursor.setWithOffset(pos, dir);
            if (isSolidSense(level.getBlockState(cursor), level, cursor)) {
                solids++;
            }
        }
        // Sky pockets near walls / overhangs still count; open fields do not.
        return solids >= 2 || pos.closerThan(origin, 3.5);
    }

    private static boolean isTrap(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.TRIPWIRE
                || block == Blocks.TRIPWIRE_HOOK
                || block == Blocks.STONE_PRESSURE_PLATE
                || block == Blocks.OAK_PRESSURE_PLATE
                || block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE
                || block == Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE
                || block == Blocks.SCULK_SENSOR
                || block == Blocks.CALIBRATED_SCULK_SENSOR
                || block == Blocks.SCULK_SHRIEKER
                || block == Blocks.TNT;
    }

    public static Vec3 pingCenter(ServerPlayer caster) {
        return caster.position().add(0, caster.getEyeHeight() * 0.55, 0);
    }
}
