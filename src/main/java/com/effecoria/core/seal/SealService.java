package com.effecoria.core.seal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

    public static List<SealInstance> getAll(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return getChunkData(chunk).getAll(pos);
    }

    public static Optional<SealInstance> find(Level level, BlockPos pos, ResourceLocation typeId) {
        LevelChunk chunk = level.getChunkAt(pos);
        return getChunkData(chunk).find(pos, typeId);
    }

    /**
     * Places or stacks a seal. Offensive seals replace any existing offensive layer
     * (trap / snare / repulse / program — last cast wins at place time).
     * Fortify and glow may coexist; same type refreshes in place.
     *
     * @param durationTicks {@code -1} for permanent, otherwise lifetime in ticks
     */
    public static SealPlaceOutcome place(
            ServerLevel level,
            BlockPos pos,
            ResourceLocation typeId,
            UUID casterId,
            float strength,
            int durationTicks,
            CompoundTag params) {
        if (typeId.equals(SealTypes.PROGRAM)) {
            placeProgram(level, pos, casterId, strength, params == null ? new CompoundTag() : params.copy());
            return SealPlaceOutcome.of(SealPlaceResult.PLACED, null, getAll(level, pos));
        }
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData data = getChunkData(chunk);
        List<SealInstance> layers = new ArrayList<>(data.getAll(pos));
        SealLayer layer = SealLayer.of(typeId);

        SealPlaceResult result = SealPlaceResult.PLACED;
        ResourceLocation previousOffensive = null;
        if (layer == SealLayer.OFFENSIVE) {
            Optional<SealInstance> existing = data.findOffensive(pos);
            if (existing.isPresent()) {
                previousOffensive = existing.get().typeId();
                result = previousOffensive.equals(typeId)
                        ? SealPlaceResult.REPLACED_SAME
                        : SealPlaceResult.REPLACED_OFFENSIVE;
                layers.removeIf(s -> SealLayer.of(s.typeId()) == SealLayer.OFFENSIVE);
            }
        } else {
            Optional<SealInstance> same = layers.stream()
                    .filter(s -> s.typeId().equals(typeId))
                    .findFirst();
            if (same.isPresent()) {
                result = SealPlaceResult.REPLACED_SAME;
                layers.removeIf(s -> s.typeId().equals(typeId));
                clearGlowLight(level, same.get());
            } else if (layers.stream().anyMatch(s -> SealLayer.of(s.typeId()) == SealLayer.UTILITY)) {
                result = SealPlaceResult.STACKED;
            }
        }

        CompoundTag sealParams = params == null ? new CompoundTag() : params.copy();
        if (typeId.equals(SealTypes.GLOW)) {
            attachGlowLight(level, pos, strength, sealParams);
        }

        long now = level.getGameTime();
        long expireAt = durationTicks < 0 ? SealInstance.PERMANENT : now + durationTicks;
        layers.add(new SealInstance(typeId, casterId, now, expireAt, strength, sealParams));

        data.putLayers(pos, layers);
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
        return SealPlaceOutcome.of(result, previousOffensive, data.getAll(pos));
    }

    /** Replace any seals on the block with a single permanent word program. */
    public static void placeProgram(
            ServerLevel level, BlockPos pos, UUID casterId, float strength, CompoundTag params) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData data = getChunkData(chunk);
        for (SealInstance old : data.getAll(pos)) {
            clearGlowLight(level, old);
        }
        CompoundTag sealParams = params == null ? new CompoundTag() : params.copy();
        long now = level.getGameTime();
        List<SealInstance> layers = new ArrayList<>();
        layers.add(new SealInstance(SealTypes.PROGRAM, casterId, now, SealInstance.PERMANENT, strength, sealParams));
        data.putLayers(pos, layers);
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
        refreshLight(level, pos);
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData data = getChunkData(chunk);
        List<SealInstance> existing = data.getAll(pos);
        if (existing.isEmpty()) {
            return false;
        }
        for (SealInstance seal : existing) {
            clearGlowLight(level, seal);
        }
        data.removeAll(pos);
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
        refreshLight(level, pos);
        return true;
    }

    /** Purge expired seals in this chunk and clear any glow lights. */
    public static boolean purgeExpired(ServerLevel level, LevelChunk chunk, long gameTime) {
        ChunkSealData data = getChunkData(chunk);
        boolean changed = false;
        Iterator<Map.Entry<BlockPos, List<SealInstance>>> it = data.seals().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, List<SealInstance>> entry = it.next();
            List<SealInstance> layers = entry.getValue();
            Iterator<SealInstance> layerIt = layers.iterator();
            while (layerIt.hasNext()) {
                SealInstance seal = layerIt.next();
                if (seal.isExpired(gameTime)) {
                    clearGlowLight(level, seal);
                    layerIt.remove();
                    changed = true;
                }
            }
            if (layers.isEmpty()) {
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
        boolean glow = seal.typeId().equals(SealTypes.GLOW)
                || (SealProgramRuntime.isProgram(seal)
                        && SealProgramRuntime.effectiveGlow(seal, level.getGameTime()) > 0);
        if (!glow && !(seal.typeId().equals(SealTypes.PROGRAM)
                && seal.params() != null
                && seal.params().contains(LIGHT_X))) {
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
     * Re-places glow light if the recorded light block is missing.
     * Persists updated coords onto the chunk seal when needed.
     */
    public static void ensureGlowLight(ServerLevel level, LevelChunk chunk, BlockPos sealedPos, SealInstance seal) {
        long gameTime = level.getGameTime();
        int programGlowLevel = SealProgramRuntime.isProgram(seal)
                ? SealProgramRuntime.effectiveGlow(seal, gameTime)
                : 0;
        boolean programGlow = programGlowLevel > 0;
        if (programGlow) {
            refreshLight(level, sealedPos);
            return;
        }
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
        float strength = seal.strength();
        attachGlowLight(level, sealedPos, strength, updated);
        if (!updated.contains(LIGHT_X)) {
            return;
        }
        ChunkSealData data = getChunkData(chunk);
        List<SealInstance> layers = new ArrayList<>(data.getAll(sealedPos));
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).typeId().equals(SealTypes.GLOW)
                    || layers.get(i).typeId().equals(SealTypes.PROGRAM)) {
                SealInstance old = layers.get(i);
                layers.set(
                        i,
                        new SealInstance(
                                old.typeId(),
                                old.casterId(),
                                old.placedAt(),
                                old.expireAt(),
                                old.strength(),
                                updated));
                break;
            }
        }
        data.putLayers(sealedPos, layers);
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

    /** Recompute vanilla light at the sealed cell (overlay emission, no extra Light block). */
    public static void refreshLight(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level != null) {
            level.getLightEngine().checkBlock(pos);
        }
    }

    public static void syncChunk(LevelChunk chunk) {
        chunk.syncData(ModAttachments.CHUNK_SEALS.get());
    }

    /** Persist in-place NBT mutations (program runtime latches) and sync to clients. */
    public static void markDirty(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkSealData data = getChunkData(chunk);
        chunk.setData(ModAttachments.CHUNK_SEALS.get(), data);
        chunk.setUnsaved(true);
        syncChunk(chunk);
    }

    public static float fortifyBreakFactor(SealInstance seal) {
        if (SealProgramRuntime.isProgram(seal)) {
            return fortifyBreakFactor(seal, 0L);
        }
        float factor = 1f / (1.5f + Math.max(0.1f, seal.strength() / 40f));
        return Math.clamp(factor, 0.2f, 0.75f);
    }

    public static float fortifyBreakFactor(SealInstance seal, long gameTime) {
        if (SealProgramRuntime.isProgram(seal)) {
            float mult = SealProgramRuntime.effectiveHardness(seal, gameTime);
            if (mult > 0f) {
                return Math.clamp(1f / Math.max(1f, mult), 0.1f, 0.9f);
            }
            return 1f;
        }
        float factor = 1f / (1.5f + Math.max(0.1f, seal.strength() / 40f));
        return Math.clamp(factor, 0.2f, 0.75f);
    }

    public static float trapDamage(SealInstance seal) {
        if (SealProgramRuntime.isProgram(seal)) {
            float hurt = SealProgramRuntime.effectiveHurt(seal, Long.MAX_VALUE / 4);
            if (hurt > 0f) {
                return Math.max(0.5f, hurt);
            }
        }
        float mult = 1f;
        if (seal.params() != null && seal.params().contains("trap_damage_mult")) {
            mult = seal.params().getFloat("trap_damage_mult");
        }
        return Math.max(0.5f, seal.strength() / 25f) * mult;
    }
}
