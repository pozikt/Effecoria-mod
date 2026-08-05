package com.effecoria.core.phi;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlockTags;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.world.EssencePlateauService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Local Φ sampling. Environmental modifiers stack as signed bonuses/penalties on a
 * base of 1.0 — night no longer multiplies (and thus halves) every other buff.
 *
 * <p>Must behave the same on client (HUD) and server (cast/regen).
 */
public final class PhiFieldService {
    private PhiFieldService() {}

    /** Chebyshev radius for dense {@link ModBlockTags#ZERO_FLUX} materials. */
    public static final int ZERO_FLUX_RANGE = 2;
    /** Chebyshev radius for {@link ModBlockTags#COLD_IRON}. */
    public static final int COLD_IRON_RANGE = 1;

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
        value += EssencePlateauService.phiEnvironmentBonus(level, pos);

        if (isInsideZeroFluxZone(level, pos) || isIronInsulated(player)) {
            return new PhiSample(0f, true, isSolarDay(level));
        }
        if (player != null) {
            float mult = Math.max(0f, PsiHelper.get(player).phiMultiplier());
            float cap = BalanceConfig.PHI_MULTIPLIER_BONUS_CAP.get().floatValue();
            float clamped = Math.min(mult, Math.max(0f, cap));
            value += clamped - 1f;
        }
        return new PhiSample(Math.max(0f, value), false, isSolarDay(level));
    }

    public static boolean isSolarDay(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        return level.getDayTime() % 24000L < 12000L;
    }

    /** True if {@code state} is tagged as anti-magic mass (ZNΦ or cold iron). */
    public static boolean isAntiMagicBlock(BlockState state) {
        return state.is(ModBlockTags.ZERO_FLUX) || state.is(ModBlockTags.COLD_IRON);
    }

    /**
     * Tag-based ZNΦ: dense zero-flux materials within 2 blocks, or cold iron within 1.
     * Replaces the old stone-shell enclosure heuristic.
     */
    public static boolean isInsideZeroFluxZone(Level level, BlockPos center) {
        int r = ZERO_FLUX_RANGE;
        for (BlockPos offset : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            BlockState state = level.getBlockState(offset);
            if (state.is(ModBlockTags.ZERO_FLUX)) {
                return true;
            }
            if (state.is(ModBlockTags.COLD_IRON) && chebyshev(center, offset) <= COLD_IRON_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIronInsulated(Player player) {
        if (player == null) {
            return false;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (isIronArmor(stack)) {
                return true;
            }
        }
        return isIronTool(player.getMainHandItem()) || isIronTool(player.getOffhandItem());
    }

    private static boolean isIronArmor(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
            return false;
        }
        return isVanillaIronItem(stack);
    }

    private static boolean isIronTool(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TieredItem)) {
            return false;
        }
        return isVanillaIronItem(stack);
    }

    private static boolean isVanillaIronItem(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "minecraft".equals(key.getNamespace()) && key.getPath().startsWith("iron_");
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(
                Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())),
                Math.abs(a.getZ() - b.getZ()));
    }

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
}
