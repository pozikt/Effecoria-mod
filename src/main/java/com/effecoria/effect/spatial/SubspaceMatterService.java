package com.effecoria.effect.spatial;

import com.effecoria.world.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Tracks matter exiled into hyperspace for future Chaos Reefs / spit-back / Ψ-ghosts.
 * Physical copies land in a personal dump yard next to the caster's voyage rendezvous.
 */
public final class SubspaceMatterService {
    private static final int MAX_QUEUE = 2048;
    /** Offset from voyage feet so dumps don't bury the portal landing. */
    private static final int DUMP_OFFSET_X = 6;
    private static final int DUMP_OFFSET_Z = 6;
    /** Horizontal radius of the chaotic debris field around the yard. */
    private static final int SCATTER_RADIUS = 10;
    /** Extra vertical loft for floating junk (Φ drift). */
    private static final int SCATTER_HEIGHT = 7;
    private static final int SCATTER_TRIES = 12;
    private static final List<ExiledSample> QUEUE = new ArrayList<>();
    /** Spill counter when the field is too dense. */
    private static final Map<UUID, Integer> DUMP_COLUMN = new HashMap<>();

    private SubspaceMatterService() {}

    public enum MatterClass {
        ORGANIC,
        METAL_CONDUCTOR,
        METAL_INSULATOR,
        STONE,
        ARTIFACT,
        OMEGA_TAINTED,
        OTHER
    }

    public record ExiledSample(
            UUID casterId,
            long gameTime,
            MatterClass matterClass,
            String blockId,
            BlockPos originPos,
            String originDim) {}

    public record ExileResult(int removed, @Nullable BlockPos dumpCorner, boolean placedInSubspace) {}

    /**
     * Exile realspace blocks into hyperspace as a chaotic debris field beside the voyage yard
     * (not a rebuilt cube — Φ currents scramble the volume).
     */
    public static ExileResult exileVolume(ServerLevel level, ServerPlayer caster, Iterable<BlockPos> positions) {
        List<BlockPos> valid = new ArrayList<>();
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!canExile(state, level, pos)) {
                continue;
            }
            BlockPos key = pos.immutable();
            valid.add(key);
            states.put(key, state);
        }
        if (valid.isEmpty()) {
            return new ExileResult(0, null, false);
        }

        BlockPos yardOrigin = resolveDumpOrigin(caster);
        ServerLevel subspace = ModDimensions.subspace(level.getServer());
        boolean placed = false;
        // Seed mixes caster + tick so each cast scatters differently, but stays stable mid-cast.
        java.util.Random rng = new java.util.Random(
                caster.getUUID().getMostSignificantBits()
                        ^ Long.rotateLeft(level.getGameTime(), 17)
                        ^ (valid.size() * 31L));

        for (BlockPos pos : valid) {
            BlockState state = states.get(pos);
            MatterClass kind = classify(state);
            String blockId = state.getBlock().builtInRegistryHolder().key().location().toString();
            synchronized (QUEUE) {
                if (QUEUE.size() >= MAX_QUEUE) {
                    QUEUE.removeFirst();
                }
                QUEUE.add(new ExiledSample(
                        caster.getUUID(),
                        level.getGameTime(),
                        kind,
                        blockId,
                        pos,
                        level.dimension().location().toString()));
            }
            // No loot — volume is folded into the Φ-sublayer, not broken.
            level.removeBlock(pos, false);

            if (subspace != null) {
                BlockPos dest = pickChaoticCell(subspace, caster.getUUID(), yardOrigin, rng);
                forceChunk(subspace, dest);
                // Only prop grounded debris; floating junk stays suspended in Φ.
                if (dest.getY() <= yardOrigin.getY()) {
                    ensureSupport(subspace, dest);
                }
                subspace.setBlock(dest, state, 3);
                placed = true;
            }
        }

        if (subspace != null && placed) {
            BlockPos marker = yardOrigin.above(4);
            forceChunk(subspace, marker);
            if (subspace.getBlockState(marker).isAir()) {
                subspace.setBlock(marker, Blocks.END_ROD.defaultBlockState(), 3);
            }
        }

        return new ExileResult(valid.size(), yardOrigin, placed);
    }

    /** Remove a realspace block with no drops and enqueue a hyperspace sample. */
    public static boolean exileBlock(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        return exileVolume(level, caster, List.of(pos.immutable())).removed() > 0;
    }

    /**
     * Dump yard sits beside the active voyage entry when present; otherwise at the caster's
     * personal rendezvous (same hash as {@link SubspaceVoyageService#subspaceAnchor(UUID)}).
     */
    public static BlockPos resolveDumpOrigin(ServerPlayer caster) {
        SubspaceVoyageData voyage = SubspaceVoyageService.get(caster);
        BlockPos entry = voyage.entrySubspacePos();
        if (entry != null && (voyage.active() || voyage.pendingEntry())) {
            return entry.offset(DUMP_OFFSET_X, 0, DUMP_OFFSET_Z);
        }
        return personalYard(caster.getUUID());
    }

    /** Personal matter yard — shared with the host's voyage landing when entry uses player UUID. */
    public static BlockPos personalYard(UUID casterId) {
        return SubspaceVoyageService.subspaceAnchor(casterId).offset(DUMP_OFFSET_X, 0, DUMP_OFFSET_Z);
    }

    /** Random free cell in a disk around the yard — mostly ground scrap, some floating shards. */
    private static BlockPos pickChaoticCell(
            ServerLevel subspace, UUID casterId, BlockPos yardOrigin, java.util.Random rng) {
        int minY = subspace.getMinBuildHeight() + 1;
        int maxY = subspace.getMaxBuildHeight() - 2;
        for (int attempt = 0; attempt < SCATTER_TRIES; attempt++) {
            // Polar scatter: denser near center, ragged rim.
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(rng.nextDouble()) * SCATTER_RADIUS;
            int dx = (int) Math.round(Math.cos(angle) * dist);
            int dz = (int) Math.round(Math.sin(angle) * dist);
            // ~35% float as Φ-drift debris; rest tumble near the sheet.
            int dy;
            if (rng.nextFloat() < 0.35f) {
                dy = 1 + rng.nextInt(SCATTER_HEIGHT);
            } else {
                dy = rng.nextInt(3) == 0 ? rng.nextInt(2) : 0;
            }
            int y = Math.max(minY, Math.min(maxY, yardOrigin.getY() + dy));
            BlockPos candidate = new BlockPos(yardOrigin.getX() + dx, y, yardOrigin.getZ() + dz);
            forceChunk(subspace, candidate);
            BlockState existing = subspace.getBlockState(candidate);
            if (existing.isAir() || existing.canBeReplaced()) {
                return candidate;
            }
        }
        // Field packed — golden-angle spiral outward (never stack a tower).
        int column;
        synchronized (DUMP_COLUMN) {
            column = DUMP_COLUMN.merge(casterId, 1, Integer::sum);
        }
        double angle = column * 2.399963;
        int ring = SCATTER_RADIUS + 2 + column / 6;
        int dx = (int) Math.round(Math.cos(angle) * ring);
        int dz = (int) Math.round(Math.sin(angle) * ring);
        int dy = column % 5;
        BlockPos spill = new BlockPos(yardOrigin.getX() + dx, yardOrigin.getY() + dy, yardOrigin.getZ() + dz);
        forceChunk(subspace, spill);
        return spill;
    }

    private static void forceChunk(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        // Touch the chunk so setBlock cannot no-op on an unloaded section.
        chunk.setUnsaved(true);
    }

    private static void ensureSupport(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        if (level.getBlockState(floor).isAir()
                || level.getBlockState(floor).canBeReplaced()
                || level.getBlockState(floor).is(Blocks.END_STONE)) {
            // Keep dumps standing on the translucent Φ membrane.
            level.setBlock(floor, SubspaceTerrain.floorState(), 3);
        }
    }

    public static MatterClass classify(BlockState state) {
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(BlockTags.DIRT)) {
            return MatterClass.ORGANIC;
        }
        if (state.is(Blocks.GOLD_BLOCK)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.RAW_GOLD_BLOCK)
                || state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)
                || state.is(Blocks.GILDED_BLACKSTONE)) {
            return MatterClass.METAL_INSULATOR;
        }
        if (state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.RAW_IRON_BLOCK)
                || state.is(Blocks.IRON_BARS)
                || state.is(Blocks.IRON_DOOR)
                || state.is(Blocks.IRON_TRAPDOOR)
                || state.is(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE)
                || state.is(Blocks.CHAIN)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL)) {
            return MatterClass.METAL_CONDUCTOR;
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)) {
            if (state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.OBSIDIAN)) {
                return MatterClass.OMEGA_TAINTED;
            }
            return MatterClass.STONE;
        }
        return MatterClass.OTHER;
    }

    public static boolean canExile(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.isAir() || state.liquid()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0f) {
            return false;
        }
        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.STRUCTURE_VOID)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_GATEWAY)) {
            return false;
        }
        return true;
    }

    /** Snapshot for future reef generation / spit-back ticks. */
    public static List<ExiledSample> snapshotQueue() {
        synchronized (QUEUE) {
            return List.copyOf(QUEUE);
        }
    }

    public static void clearQueue() {
        synchronized (QUEUE) {
            QUEUE.clear();
        }
    }
}
