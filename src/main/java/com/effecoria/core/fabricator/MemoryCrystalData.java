package com.effecoria.core.fabricator;

import com.effecoria.content.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/** CustomData recipe pointer on {@code memory_crystal}. */
public final class MemoryCrystalData {
    public static final String RECIPE_KEY = "FabricatorRecipe";

    private MemoryCrystalData() {}

    public static boolean isBlank(ItemStack stack) {
        return isCrystal(stack) && recipeId(stack).isEmpty();
    }

    public static boolean isInscribed(ItemStack stack) {
        return isCrystal(stack) && recipeId(stack).isPresent();
    }

    public static boolean isCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.MEMORY_CRYSTAL.get());
    }

    public static Optional<ResourceLocation> recipeId(ItemStack stack) {
        if (!isCrystal(stack)) {
            return Optional.empty();
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains(RECIPE_KEY)) {
            return Optional.empty();
        }
        String raw = tag.getString(RECIPE_KEY);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ResourceLocation.parse(raw));
    }

    public static void writeRecipe(ItemStack stack, ResourceLocation recipeId) {
        if (!isCrystal(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(RECIPE_KEY, recipeId.toString()));
    }

    public static void clearRecipe(ItemStack stack) {
        if (!isCrystal(stack)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(RECIPE_KEY));
    }
}
