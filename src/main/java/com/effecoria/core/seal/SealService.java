package com.effecoria.core.seal;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;

import com.effecoria.core.psi.ModAttachments;

public final class SealService {
    private static final String LIGHT_X = "lightX";
    private static final String LIGHT_Y = "lightY";
    private static final String LIGHT_Z = "lightZ";

    private SealService() {}

    public static ChunkSealData getChunkData(LevelChunk chunk) {
        return chunk.getData(ModAttachments.CHUNK_SEALS.get());
    }

    public static Optional<SealInstance> get(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return getChunkData(chunk).get(pos);
    }

    /**
     * @param durationTicks {@code -1} for permanent, otherwise lifetime in ticks
     */
    public static SealInstance place(
            ServerLevel level,
            BlockPos pos,
            ResourceLocation typeId,
            UUID casterId,
            float strength,
            int durationTicks,
            CompoundTag params) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData data = getChunkData(chunk);

        data.get(pos).ifPresent(old -> clearGlowLight(level, old));

        CompoundTag sealParams = params == null ? new CompoundTag() : params.copy();
        if (typeId.equals(SealTypes.GLOW)) {
            attachGlowLight(level, pos, strength, sealParams);
        }

        long now = level.getGameTime();
        long expireAt = durationTicks < 0 ? SealInstance.PERMANENT : now + durationTicks;
        SealInstance seal = new SealInstance(
                typeId,
                casterId,
                now,
                expireAt,
                strength,
                sealParams);

        data.put(pos, seal);
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
        return seal;
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData data = getChunkData(chunk);
        Optional<SealInstance> existing = data.get(pos);
        if (existing.isEmpty()) {
            return false;
        }
        clearGlowLight(level, existing.get());
        data.remove(pos);
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
        return true;
    }

    /** Purge expired seals in this chunk and clear any glow lights. */
    public static boolean purgeExpired(ServerLevel level, LevelChunk chunk, long gameTime) {
        ChunkSealData data = getChunkData(chunk);
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, SealInstance>> it = data.seals().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, SealInstance> entry = it.next();
            if (entry.getValue().isExpired(gameTime)) {
                clearGlowLight(level, entry.getValue());
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
            chunk.setUnsaved(true);
            syncChunk(chunk);
        }
        return changed;
    }

    public static void clearGlowLight(ServerLevel level, SealInstance seal) {
        if (!seal.typeId().equals(SealTypes.GLOW)) {
            return;
        }
        CompoundTag params = seal.params();
        if (params == null || !params.contains(LIGHT_X)) {
            return;
        }
        BlockPos lightPos = new BlockPos(params.getInt(LIGHT_X), params.getInt(LIGHT_Y), params.getInt(LIGHT_Z));
        if (level.getBlockState(lightPos).is(Blocks.LIGHT)) {
            level.removeBlock(lightPos, false);
        }
    }

    /** Places an invisible light source in adjacent air; records coords in params. */
    public static void attachGlowLight(ServerLevel level, BlockPos sealedPos, float strength, CompoundTag params) {
        BlockPos lightPos = findLightPlacement(level, sealedPos);
        if (lightPos == null) {
            return;
        }
        int lightLevel = Math.clamp(Math.round(9f + strength / 25f), 8, 15);
        BlockState existing = level.getBlockState(lightPos);
        BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lightLevel);
        if (existing.getFluidState().is(Fluids.WATER)) {
            lightState = lightState.setValue(LightBlock.WATERLOGGED, true);
        }
        level.setBlock(lightPos, lightState, 3);
        params.putInt(LIGHT_X, lightPos.getX());
        params.putInt(LIGHT_Y, lightPos.getY());
        params.putInt(LIGHT_Z, lightPos.getZ());
    }

    /**
     * Re-places glow light if the recorded light block is missing (e.g. after world edit / block replace).
     * Persists updated coords onto the chunk seal when needed.
     */
    public static void ensureGlowLight(ServerLevel level, LevelChunk chunk, BlockPos sealedPos, SealInstance seal) {
        if (!seal.typeId().equals(SealTypes.GLOW)) {
            return;
        }
        CompoundTag params = seal.params();
        if (params != null && params.contains(LIGHT_X)) {
            BlockPos lightPos = new BlockPos(params.getInt(LIGHT_X), params.getInt(LIGHT_Y), params.getInt(LIGHT_Z));
            if (level.getBlockState(lightPos).is(Blocks.LIGHT)) {
                return;
            }
        }
        CompoundTag updated = params == null ? new CompoundTag() : params.copy();
        updated.remove(LIGHT_X);
        updated.remove(LIGHT_Y);
        updated.remove(LIGHT_Z);
        attachGlowLight(level, sealedPos, seal.strength(), updated);
        if (!updated.contains(LIGHT_X)) {
            return;
        }
        ChunkSealData data = getChunkData(chunk);
        data.put(
                sealedPos,
                new SealInstance(
                        seal.typeId(),
                        seal.casterId(),
                        seal.placedAt(),
                        seal.expireAt(),
                        seal.strength(),
                        updated));
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
    }

    private static BlockPos findLightPlacement(ServerLevel level, BlockPos sealedPos) {
        Direction[] order = {
                Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN
        };
        for (Direction dir : order) {
            BlockPos candidate = sealedPos.relative(dir);
            BlockState state = level.getBlockState(candidate);
            if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LIGHT)) {
                return candidate;
            }
            if (state.canBeReplaced()) {
                return candidate;
            }
        }
        return null;
    }

    public static void syncChunk(LevelChunk chunk) {
        chunk.syncData(ModAttachments.CHUNK_SEALS.get());
    }

    public static float fortifyBreakFactor(SealInstance seal) {
        float factor = 1f / (1.5f + Math.max(0.1f, seal.strength() / 40f));
        return Math.clamp(factor, 0.2f, 0.75f);
    }

    public static float trapDamage(SealInstance seal) {
        return Math.max(0.5f, seal.strength() / 25f);
    }
}
