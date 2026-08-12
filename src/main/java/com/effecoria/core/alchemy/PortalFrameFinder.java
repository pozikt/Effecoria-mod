package com.effecoria.core.alchemy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Detects a closed mithril (Φ-conductor) contour adjacent to a portal modulator and its interior.
 */
public final class PortalFrameFinder {
    public static final int MAX_FRAME = 256;
    public static final int MAX_INTERIOR = 128;

    public record FrameShape(
            List<BlockPos> frameCells, List<BlockPos> interiorCells, Direction.Axis planeNormal) {}

    private PortalFrameFinder() {}

    @javax.annotation.Nullable
    public static FrameShape find(Level level, BlockPos modulatorPos) {
        BlockPos seed = null;
        for (Direction dir : Direction.values()) {
            BlockPos adj = modulatorPos.relative(dir);
            if (isFrameMaterial(level.getBlockState(adj))) {
                seed = adj;
                break;
            }
        }
        if (seed == null) {
            return null;
        }

        List<BlockPos> frame = collectFrame(level, seed);
        if (frame.isEmpty() || frame.size() > MAX_FRAME) {
            return null;
        }

        Direction.Axis axis = dominantPlane(frame);
        List<BlockPos> interior = findInterior(level, frame, axis);
        if (interior.isEmpty() || interior.size() > MAX_INTERIOR) {
            return null;
        }
        return new FrameShape(List.copyOf(frame), List.copyOf(interior), axis);
    }

    private static boolean isFrameMaterial(BlockState state) {
        // Portal frames are mithril; bus may touch but does not count as structural frame.
        return state.is(ModBlocks.MITHRIL_BLOCK.get());
    }

    private static List<BlockPos> collectFrame(Level level, BlockPos seed) {
        List<BlockPos> out = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(seed);
        visited.add(seed);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            out.add(cur);
            if (out.size() > MAX_FRAME) {
                return List.of();
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                if (!isFrameMaterial(level.getBlockState(next))) {
                    continue;
                }
                visited.add(next);
                queue.add(next);
            }
        }
        return out;
    }

    private static Direction.Axis dominantPlane(List<BlockPos> frame) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : frame) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        int sx = maxX - minX;
        int sy = maxY - minY;
        int sz = maxZ - minZ;
        // Normal is the thinnest axis (portal lies in the other two).
        if (sx <= sy && sx <= sz) {
            return Direction.Axis.X;
        }
        if (sy <= sx && sy <= sz) {
            return Direction.Axis.Y;
        }
        return Direction.Axis.Z;
    }

    private static List<BlockPos> findInterior(Level level, List<BlockPos> frame, Direction.Axis normal) {
        Set<BlockPos> frameSet = new HashSet<>(frame);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : frame) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }

        // Collapse to a single slab along the normal (median of frame).
        int slab = medianCoord(frame, normal);

        Set<BlockPos> candidates = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (coord(p, normal) != slab) {
                        continue;
                    }
                    if (frameSet.contains(p)) {
                        continue;
                    }
                    if (!isReplaceableInterior(level.getBlockState(p))) {
                        continue;
                    }
                    candidates.add(p);
                }
            }
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        // Outside flood on the slab (4-connected in plane).
        Set<BlockPos> outside = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos c : candidates) {
            if (isOnAabbBorder(c, minX, maxX, minY, maxY, minZ, maxZ, normal)) {
                queue.add(c);
                outside.add(c);
            }
        }
        // Also seed just outside AABB on the slab.
        seedOutsideBorder(queue, outside, candidates, minX, maxX, minY, maxY, minZ, maxZ, slab, normal);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction dir : planarDirs(normal)) {
                BlockPos next = cur.relative(dir);
                if (coord(next, normal) != slab) {
                    continue;
                }
                if (frameSet.contains(next) || outside.contains(next)) {
                    continue;
                }
                if (!candidates.contains(next) && !isReplaceableInterior(level.getBlockState(next))) {
                    continue;
                }
                // Stay within expanded AABB
                if (next.getX() < minX - 1
                        || next.getX() > maxX + 1
                        || next.getY() < minY - 1
                        || next.getY() > maxY + 1
                        || next.getZ() < minZ - 1
                        || next.getZ() > maxZ + 1) {
                    continue;
                }
                outside.add(next);
                queue.add(next);
            }
        }

        List<BlockPos> interior = new ArrayList<>();
        for (BlockPos c : candidates) {
            if (!outside.contains(c)) {
                interior.add(c);
            }
        }
        return interior;
    }

    private static void seedOutsideBorder(
            Queue<BlockPos> queue,
            Set<BlockPos> outside,
            Set<BlockPos> candidates,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            int slab,
            Direction.Axis normal) {
        // Walk AABB perimeter on the slab; any non-frame cell starts outside flood.
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int y = minY - 1; y <= maxY + 1; y++) {
                for (int z = minZ - 1; z <= maxZ + 1; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (coord(p, normal) != slab) {
                        continue;
                    }
                    boolean onExpandedBorder = x == minX - 1
                            || x == maxX + 1
                            || y == minY - 1
                            || y == maxY + 1
                            || z == minZ - 1
                            || z == maxZ + 1;
                    if (!onExpandedBorder) {
                        continue;
                    }
                    if (!outside.contains(p)) {
                        outside.add(p);
                        queue.add(p);
                    }
                }
            }
        }
    }

    private static boolean isOnAabbBorder(
            BlockPos p, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Direction.Axis normal) {
        return switch (normal) {
            case X -> p.getY() == minY || p.getY() == maxY || p.getZ() == minZ || p.getZ() == maxZ;
            case Y -> p.getX() == minX || p.getX() == maxX || p.getZ() == minZ || p.getZ() == maxZ;
            case Z -> p.getX() == minX || p.getX() == maxX || p.getY() == minY || p.getY() == maxY;
        };
    }

    private static Direction[] planarDirs(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction[] {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            case Y -> new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            case Z -> new Direction[] {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
        };
    }

    private static int coord(BlockPos p, Direction.Axis axis) {
        return switch (axis) {
            case X -> p.getX();
            case Y -> p.getY();
            case Z -> p.getZ();
        };
    }

    private static int medianCoord(List<BlockPos> frame, Direction.Axis axis) {
        List<Integer> vals = new ArrayList<>(frame.size());
        for (BlockPos p : frame) {
            vals.add(coord(p, axis));
        }
        vals.sort(Integer::compareTo);
        return vals.get(vals.size() / 2);
    }

    private static boolean isReplaceableInterior(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(ModBlocks.PORTAL_GATE.get());
    }
}
