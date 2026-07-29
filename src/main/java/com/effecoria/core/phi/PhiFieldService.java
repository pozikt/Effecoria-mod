package com.effecoria.core.phi;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class PhiFieldService {
    private PhiFieldService() {}

    public static PhiSample sample(Level level, Vec3 position) {
        return sample(level, position, null);
    }

    /** Samples Φ for a player — creative god mode overrides environmental limits. */
    public static PhiSample sample(Level level, Vec3 position, Player player) {
        if (CreativeGodMode.isActive(player)) {
            return new PhiSample(999f, false, isSolarDay(level));
        }
        float value = 1f;
        boolean zeroFlux = false;

        value *= dimensionFactor(level);
        value *= heightFactor(position.y());
        if (level instanceof ServerLevel serverLevel) {
            value *= timeFactor(serverLevel);
            value *= exposureFactor(serverLevel, BlockPos.containing(position));
            value *= weatherFactor(serverLevel);
            zeroFlux = isInsideZeroFluxZone(serverLevel, BlockPos.containing(position));
        } else {
            value *= timeFactor(level);
        }

        if (zeroFlux) {
            return new PhiSample(0f, true, isSolarDay(level));
        }
        if (player != null) {
            value *= Math.max(0f, PsiHelper.get(player).phiMultiplier());
        }
        return new PhiSample(Math.max(0f, value), false, isSolarDay(level));
    }

    public static boolean isSolarDay(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        return level.getDayTime() % 24000L < 12000L;
    }

    private static float dimensionFactor(Level level) {
        if (level.dimension() == Level.NETHER) {
            return 1.2f;
        }
        if (level.dimension() == Level.END) {
            return 0.5f;
        }
        return 1f;
    }

    private static float heightFactor(double y) {
        if (y < 0) {
            return 0.7f;
        }
        if (y > 120) {
            return 1.1f;
        }
        return 1f;
    }

    private static float timeFactor(Level level) {
        return isSolarDay(level)
                ? BalanceConfig.PHI_DAY_MULTIPLIER.get().floatValue()
                : BalanceConfig.PHI_NIGHT_MULTIPLIER.get().floatValue();
    }

    /** Open sky boosts surface Φ; enclosed spaces suppress it. */
    private static float exposureFactor(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return 1f;
        }
        if (level.canSeeSky(pos)) {
            return BalanceConfig.PHI_OPEN_SKY_BONUS.get().floatValue();
        }
        return BalanceConfig.PHI_UNDERGROUND_MULTIPLIER.get().floatValue();
    }

    private static float weatherFactor(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return 1f;
        }
        if (level.isThundering()) {
            return BalanceConfig.PHI_THUNDER_MULTIPLIER.get().floatValue();
        }
        if (level.isRaining()) {
            return BalanceConfig.PHI_RAIN_MULTIPLIER.get().floatValue();
        }
        return 1f;
    }

    private static boolean isInsideZeroFluxZone(ServerLevel level, BlockPos center) {
        int enclosed = 0;
        for (BlockPos offset : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (offset.equals(center)) {
                continue;
            }
            var state = level.getBlockState(offset);
            if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.OBSIDIAN)) {
                enclosed++;
            }
        }
        return enclosed >= 20;
    }
}
