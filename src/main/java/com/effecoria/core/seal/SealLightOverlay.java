package com.effecoria.core.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/** Light from inscribed glow, without changing the block's BlockState. */
public final class SealLightOverlay {
    private SealLightOverlay() {}

    public static int emission(BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof Level level)) {
            return 0;
        }
        int best = 0;
        long gameTime = level.getGameTime();
        for (SealInstance seal : SealService.getAll(level, pos)) {
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
