package com.effecoria.content;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** Reads Resonance Focus / Phi Cell custom data from inventory. */
public final class PhiHarnessItems {
    public static final int FOCUS_MAX_TIER = 3;
    public static final String FOCUS_TIER_KEY = "FocusTier";
    public static final String CELL_CHARGE_KEY = "PhiCharge";

    private PhiHarnessItems() {}

    public static int focusTier(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.RESONANCE_FOCUS.get())) {
            return 0;
        }
        return Math.clamp(readInt(stack, FOCUS_TIER_KEY, 0), 0, FOCUS_MAX_TIER);
    }

    public static void setFocusTier(ItemStack stack, int tier) {
        writeInt(stack, FOCUS_TIER_KEY, Math.clamp(tier, 0, FOCUS_MAX_TIER));
    }

    public static float cellCharge(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.PHI_CELL.get())) {
            return 0f;
        }
        return Math.clamp(readFloat(stack, CELL_CHARGE_KEY, 0f), 0f, 1f);
    }

    public static void setCellCharge(ItemStack stack, float charge) {
        writeFloat(stack, CELL_CHARGE_KEY, Math.clamp(charge, 0f, 1f));
    }

    /** Best focus currently held or anywhere in the inventory. */
    public static int bestFocusTier(Player player) {
        int best = 0;
        for (ItemStack stack : player.getInventory().items) {
            best = Math.max(best, focusTier(stack));
        }
        best = Math.max(best, focusTier(player.getOffhandItem()));
        return best;
    }

    public static ItemStack findPhiCell(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.PHI_CELL.get()) && cellCharge(main) > 0.001f) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.PHI_CELL.get()) && cellCharge(off) > 0.001f) {
            return off;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.PHI_CELL.get()) && cellCharge(stack) > 0.001f) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void appendFocusTooltip(ItemStack stack, List<Component> tooltip) {
        int tier = focusTier(stack);
        tooltip.add(Component.translatable("item.effecoria.resonance_focus.tier", tier, FOCUS_MAX_TIER));
        tooltip.add(Component.translatable("item.effecoria.resonance_focus.hint"));
    }

    public static void appendCellTooltip(ItemStack stack, List<Component> tooltip) {
        int pct = Math.round(cellCharge(stack) * 100f);
        tooltip.add(Component.translatable("item.effecoria.phi_cell.charge", pct));
        tooltip.add(Component.translatable("item.effecoria.phi_cell.hint"));
    }

    private static int readInt(ItemStack stack, String key, int fallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }

    private static float readFloat(ItemStack stack, String key, float fallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getFloat(key) : fallback;
    }

    private static void writeInt(ItemStack stack, String key, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, value));
    }

    private static void writeFloat(ItemStack stack, String key, float value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(key, value));
    }
}
