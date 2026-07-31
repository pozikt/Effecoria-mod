package com.effecoria.effect.spatial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks matter exiled into hyperspace for future Chaos Reefs / spit-back / Ψ-ghosts.
 * MVP: classify + queue only; transformation timelines live in docs/SUBSPACE.md.
 */
public final class SubspaceMatterService {
    private static final int MAX_QUEUE = 2048;
    private static final List<ExiledSample> QUEUE = new ArrayList<>();

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

    /** Remove a realspace block with no drops and enqueue a hyperspace sample. */
    public static boolean exileBlock(ServerLevel level, ServerPlayer caster, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!canExile(state, level, pos)) {
            return false;
        }
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
                    pos.immutable(),
                    level.dimension().location().toString()));
        }
        // No loot — volume is folded into the Φ-sublayer, not broken.
        level.removeBlock(pos, false);
        return true;
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
            // Iron essentializes slowly; still metal — use OTHER conductor-ish bucket for now.
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
