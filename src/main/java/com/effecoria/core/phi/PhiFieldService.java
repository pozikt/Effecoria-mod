package com.effecoria.core.phi;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Local Φ sampling. Environmental modifiers stack as signed bonuses/penalties on a
 * base of 1.0 — night no longer multiplies (and thus halves) every other buff.
 *
 * <p>Must behave the same on client (HUD) and server (cast/regen).
 */
public final class PhiFieldService {
    private PhiFieldService() {}

    public static PhiSample sample(Level level, Vec3 position) {
        return sample(level, position, null);
    }

    /** Samples Φ for a player — creative god mode overrides environmental limits. */
    public static PhiSample sample(Level level, Vec3 position, Player player) {
        if (CreativeGodMode.isActive(player)) {
            float phi = BalanceConfig.CREATIVE_PHI_OVERRIDE.get().floatValue();
            return new PhiSample(phi, false, isSolarDay(level));
        }
        BlockPos pos = BlockPos.containing(position);
        float value = 1f;

        value += dimensionBonus(level);
        value += heightBonus(position.y());
        value += timeBonus(level);
        value += exposureBonus(level, pos);
        value += weatherBonus(level);
        value += fluidBonus(level, pos, player);

        if (isInsideZeroFluxZone(level, pos)) {
            return new PhiSample(0f, true, isSolarDay(level));
        }
        if (player != null) {
            // phiMultiplier is stored as a factor around 1.0 → convert to signed bonus.
            value += Math.max(0f, PsiHelper.get(player).phiMultiplier()) - 1f;
        }
        return new PhiSample(Math.max(0f, value), false, isSolarDay(level));
    }

    public static boolean isSolarDay(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        return level.getDayTime() % 24000L < 12000L;
    }

    /** Config multipliers are interpreted as {@code bonus = multiplier - 1}. */
    private static float fromMultiplier(float multiplier) {
        return multiplier - 1f;
    }

    private static float dimensionBonus(Level level) {
        if (level.dimension() == Level.NETHER) {
            return 0.2f;
        }
        if (level.dimension() == Level.END) {
            return -0.5f;
        }
        return 0f;
    }

    private static float heightBonus(double y) {
        if (y < 0) {
            return -0.3f;
        }
        if (y > 120) {
            return 0.1f;
        }
        return 0f;
    }

    private static float timeBonus(Level level) {
        float mult = isSolarDay(level)
                ? BalanceConfig.PHI_DAY_MULTIPLIER.get().floatValue()
                : BalanceConfig.PHI_NIGHT_MULTIPLIER.get().floatValue();
        return fromMultiplier(mult);
    }

    private static float exposureBonus(Level level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return 0f;
        }
        if (level.canSeeSky(pos)) {
            return fromMultiplier(BalanceConfig.PHI_OPEN_SKY_BONUS.get().floatValue());
        }
        return fromMultiplier(BalanceConfig.PHI_UNDERGROUND_MULTIPLIER.get().floatValue());
    }

    private static float weatherBonus(Level level) {
        if (level.dimension() != Level.OVERWORLD) {
            return 0f;
        }
        if (level.isThundering()) {
            return fromMultiplier(BalanceConfig.PHI_THUNDER_MULTIPLIER.get().floatValue());
        }
        if (level.isRaining()) {
            return fromMultiplier(BalanceConfig.PHI_RAIN_MULTIPLIER.get().floatValue());
        }
        return 0f;
    }

    private static float fluidBonus(Level level, BlockPos pos, Player player) {
        boolean underwater = player != null
                ? player.isUnderWater()
                : level.getFluidState(pos).is(FluidTags.WATER)
                        && level.getFluidState(pos.above()).is(FluidTags.WATER);
        if (underwater) {
            return fromMultiplier(BalanceConfig.PHI_UNDERWATER_MULTIPLIER.get().floatValue());
        }
        boolean inWater = player != null
                ? player.isInWaterOrBubble()
                : level.getFluidState(pos).is(FluidTags.WATER);
        if (inWater) {
            return fromMultiplier(BalanceConfig.PHI_IN_WATER_MULTIPLIER.get().floatValue());
        }
        return 0f;
    }

    private static boolean isInsideZeroFluxZone(Level level, BlockPos center) {
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
