package com.effecoria.core.technomagic;

import com.effecoria.content.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** CustomData imprint flags written by the Ψ-imprinter. */
public final class ImprintData {
    public static final String READY_KEY = "ImprintReady";
    public static final String KIND_KEY = "ImprintKind";
    public static final String TIER_KEY = "ImprintTier";

    public static final String KIND_CONSTRUCT = "construct";
    public static final String KIND_TELEGRAPH = "telegraph";

    private ImprintData() {}

    public static boolean isReady(ItemStack stack) {
        return readBool(stack, READY_KEY);
    }

    public static String kind(ItemStack stack) {
        return readString(stack, KIND_KEY);
    }

    public static int tier(ItemStack stack) {
        return Math.max(0, readInt(stack, TIER_KEY, 0));
    }

    public static boolean isImprintedChassis(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(ModItems.GOLEM_CHASSIS.get())
                && isReady(stack)
                && KIND_CONSTRUCT.equals(kind(stack));
    }

    public static boolean isBlankChassis(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.GOLEM_CHASSIS.get()) && !isReady(stack);
    }

    public static boolean isBlankTelegraphModule(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.TELEGRAPH_MODULE.get()) && !isReady(stack);
    }

    public static void imprintConstruct(ItemStack stack, int tier) {
        write(stack, tag -> {
            tag.putBoolean(READY_KEY, true);
            tag.putString(KIND_KEY, KIND_CONSTRUCT);
            tag.putInt(TIER_KEY, Math.max(0, tier));
        });
    }

    public static void imprintTelegraph(ItemStack stack, int tier) {
        write(stack, tag -> {
            tag.putBoolean(READY_KEY, true);
            tag.putString(KIND_KEY, KIND_TELEGRAPH);
            tag.putInt(TIER_KEY, Math.max(0, tier));
        });
    }

    private static boolean readBool(ItemStack stack, String key) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.getBoolean(key);
    }

    private static String readString(ItemStack stack, String key) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getString(key) : "";
    }

    private static int readInt(ItemStack stack, String key, int fallback) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }

    private static void write(ItemStack stack, java.util.function.Consumer<CompoundTag> editor) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, editor);
    }
}
