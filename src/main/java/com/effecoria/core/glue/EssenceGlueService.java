package com.effecoria.core.glue;

import com.effecoria.network.ModNetworking;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Server-side Φ-glue — WorldEdit-axe style: click pos1, click pos2, glue the cuboid volume.
 */
public final class EssenceGlueService {
    public static final int VIEW_RADIUS = 96;
    /** Max solid blocks glued in one volume select. */
    public static final int MAX_SOLID = 2000;
    /** Max cuboid edge length (inclusive span). */
    public static final int MAX_EDGE = 48;

    private EssenceGlueService() {}

    /**
     * First click sets pos1; second click glues every non-air block in the cuboid
     * between pos1 and the clicked block (inclusive).
     */
    public static void selectCorner(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            player.displayClientMessage(Component.translatable("message.effecoria.essence_glue.air"), true);
            return;
        }

        BlockPos pending = EssenceGlueSelection.pending(player);
        if (pending == null) {
            EssenceGlueSelection.setPending(player, pos);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.45f, 1.4f);
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.essence_glue.pos1",
                            pos.getX(),
                            pos.getY(),
                            pos.getZ()),
                    true);
            syncViewAround(level, pos);
            return;
        }

        glueVolume(level, player, pending, pos);
        EssenceGlueSelection.clearPending(player);
        syncViewAround(level, pos);
    }

    private static void glueVolume(ServerLevel level, ServerPlayer player, BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());

        int sx = maxX - minX + 1;
        int sy = maxY - minY + 1;
        int sz = maxZ - minZ + 1;
        if (sx > MAX_EDGE || sy > MAX_EDGE || sz > MAX_EDGE) {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.essence_glue.too_large_edge", MAX_EDGE),
                    true);
            return;
        }

        Set<BlockPos> cells = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        cells.add(cursor.immutable());
                        if (cells.size() > MAX_SOLID) {
                            player.displayClientMessage(
                                    Component.translatable(
                                            "message.effecoria.essence_glue.too_large_solid", MAX_SOLID),
                                    true);
                            return;
                        }
                    }
                }
            }
        }

        if (cells.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.effecoria.essence_glue.empty_volume"), true);
            return;
        }

        EssenceGlueData data = EssenceGlueData.get(level);
        BlockPos root = null;
        for (BlockPos cell : cells) {
            data.ensure(cell);
            if (root == null) {
                root = cell;
            } else {
                data.connect(root, cell);
            }
        }

        // Merge with any already-glued neighbours so volumes can extend a structure.
        for (BlockPos cell : cells) {
            for (Direction d : Direction.values()) {
                BlockPos adj = cell.relative(d);
                if (data.isGlued(adj) && !cells.contains(adj)) {
                    data.connect(cell, adj);
                }
            }
        }

        Set<BlockPos> batch = EssenceGlueSelection.session(player);
        batch.clear();
        batch.addAll(cells);

        BlockPos soundAt = b;
        level.playSound(null, soundAt, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75f, 1.15f);

        EssenceGlueStructure.Report report = EssenceGlueStructure.inspect(level, root);
        int pct = (int) Math.round(report.integrity() * 100.0);
        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.essence_glue.volume",
                        cells.size(),
                        sx,
                        sy,
                        sz,
                        report.gluedCells(),
                        report.presentBlocks(),
                        report.missingBlocks(),
                        pct),
                true);
    }

    /** Clears pending corner + selection batch — glued blocks stay linked. */
    public static void clearSelection(ServerPlayer player) {
        EssenceGlueSelection.clear(player);
        player.displayClientMessage(Component.translatable("message.effecoria.essence_glue.selection_cleared"), true);
        syncViewAround(player.serverLevel(), player.blockPosition());
    }

    /**
     * Broken blocks stay in the glue graph as missing cells so integrity can drop.
     * Only refreshes client outlines.
     */
    public static void onBlockRemoved(ServerLevel level, BlockPos pos) {
        EssenceGlueData data = EssenceGlueData.get(level);
        if (data.isGlued(pos)) {
            syncViewAround(level, pos);
        }
    }

    /** Explicitly unglue one cell (for tools / future computer API). */
    public static void unglueBlock(ServerLevel level, BlockPos pos) {
        EssenceGlueData data = EssenceGlueData.get(level);
        if (data.isGlued(pos)) {
            data.remove(pos);
            EssenceGlueSelection.removeBlock(pos);
            syncViewAround(level, pos);
        }
    }

    public static void syncViewAround(ServerLevel level, BlockPos center) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) <= (long) VIEW_RADIUS * VIEW_RADIUS) {
                syncViewFor(player);
            }
        }
    }

    public static void syncViewFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Set<BlockPos> glued = EssenceGlueData.get(level).allInRadius(player.blockPosition(), VIEW_RADIUS);
        Set<BlockPos> session = new HashSet<>(EssenceGlueSelection.session(player));
        BlockPos pending = EssenceGlueSelection.pending(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, ModNetworking.EssenceGlueSyncPayload.of(glued, session, pending));
    }

    /** Inclusive AABB of two corners (for client preview helpers). */
    public static AABB cuboid(BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
