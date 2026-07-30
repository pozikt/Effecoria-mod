package com.effecoria.core.phi;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.formula.PhiSample;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Portable Φ gear: Resonance Focus (tier bonuses) and Phi Cell (low-Φ assist on cast).
 */
public final class PhiHarness {
    private PhiHarness() {}

    public record FocusBonuses(float costFloorRatio, float resonanceWidthBonus) {
        public static final FocusBonuses NONE = new FocusBonuses(0f, 0f);
    }

    public static FocusBonuses focusBonuses(Player player) {
        int tier = PhiHarnessItems.bestFocusTier(player);
        if (tier <= 0) {
            return FocusBonuses.NONE;
        }
        float baseFloor = BalanceConfig.SPELL_COST_FLOOR_RATIO.get().floatValue();
        float floorCut = tier * BalanceConfig.FOCUS_COST_FLOOR_PER_TIER.get().floatValue();
        float floor = Math.max(0.15f, baseFloor - floorCut);
        float width = tier * BalanceConfig.FOCUS_RESONANCE_WIDTH_PER_TIER.get().floatValue();
        return new FocusBonuses(floor, width);
    }

    /**
     * If ambient Φ is low, spend Phi Cell charge to raise the sample for this cast.
     * Returns the (possibly boosted) sample; charge is consumed immediately.
     */
    public static PhiSample assistCast(Player player, PhiSample ambient) {
        if (player == null || ambient.zeroFlux()) {
            return ambient;
        }
        float threshold = BalanceConfig.PHI_CELL_ASSIST_THRESHOLD.get().floatValue();
        float value = ambient.effectiveValue();
        if (value >= threshold) {
            return ambient;
        }
        ItemStack cell = PhiHarnessItems.findPhiCell(player);
        if (cell.isEmpty()) {
            return ambient;
        }
        float charge = PhiHarnessItems.cellCharge(cell);
        if (charge <= 0f) {
            return ambient;
        }
        float need = threshold - value;
        float perCharge = BalanceConfig.PHI_CELL_PHI_PER_CHARGE.get().floatValue();
        float spend = Math.min(charge, need / Math.max(0.01f, perCharge));
        if (spend <= 0f) {
            return ambient;
        }
        float gained = spend * perCharge;
        PhiHarnessItems.setCellCharge(cell, charge - spend);
        return new PhiSample(value + gained, false, ambient.solarDay());
    }

    /** Slow passive recharge when the player is in high ambient Φ. */
    public static void tickRecharge(Player player, PhiSample ambient) {
        if (player == null || ambient.zeroFlux() || ambient.effectiveValue() < 0.9f) {
            return;
        }
        float rate = BalanceConfig.PHI_CELL_RECHARGE_PER_TICK.get().floatValue();
        if (rate <= 0f) {
            return;
        }
        rechargeStack(player.getMainHandItem(), rate);
        rechargeStack(player.getOffhandItem(), rate);
        for (ItemStack stack : player.getInventory().items) {
            rechargeStack(stack, rate * 0.35f);
        }
    }

    private static void rechargeStack(ItemStack stack, float rate) {
        if (stack.isEmpty() || !stack.is(ModItems.PHI_CELL.get())) {
            return;
        }
        float charge = PhiHarnessItems.cellCharge(stack);
        if (charge >= 1f) {
            return;
        }
        PhiHarnessItems.setCellCharge(stack, charge + rate);
    }
}
