package com.effecoria.core.seal;

import com.effecoria.core.psi.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Light from inscribed glow, without changing the block's BlockState.
 *
 * <p>Must never call {@link Level#getChunkAt} — light queries run during worldgen / light
 * engine work, and forcing chunk load from here freezes loading at 0%.
 */
public final class SealLightOverlay {
    private SealLightOverlay() {}

    public static int emission(BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof Level level)) {
            return 0;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null || !chunk.hasData(ModAttachments.CHUNK_SEALS.get())) {
            return 0;
        }
        ChunkSealData data = chunk.getData(ModAttachments.CHUNK_SEALS.get());
        if (data.isEmpty()) {
            return 0;
        }
        int best = 0;
        long gameTime = level.getGameTime();
        for (SealInstance seal : data.getAll(pos)) {
            if (seal.isExpired(gameTime)) {
                continue;
            }
            if (SealProgramRuntime.isProgram(seal)) {
                best = Math.max(best, SealProgramRuntime.effectiveGlow(seal, gameTime));
            } else if (seal.typeId().equals(SealTypes.GLOW)) {
                best = Math.max(best, Mth.clamp(Math.round(8f + seal.strength() / 25f), 1, 15));
            }
        }
        return Mth.clamp(best, 0, 15);
    }
}
