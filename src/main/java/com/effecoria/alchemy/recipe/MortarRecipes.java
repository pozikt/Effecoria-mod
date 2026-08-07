package com.effecoria.alchemy.recipe;

import java.util.Optional;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Code-backed mortar recipes (MVP). Extend via {@link ModItemTags#MORTAR_INPUTS} + this matcher.
 */
public final class MortarRecipes {
    public record Result(ItemStack primary, ItemStack byproduct, ItemStack waste) {}

    private MortarRecipes() {}

    public static boolean isInput(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItemTags.MORTAR_INPUTS) || dustCount(stack) > 0);
    }

    public static int dustCount(ItemStack stack) {
        if (stack.is(ModItems.ESSONITE_SHARD.get())) {
            return 2;
        }
        if (stack.is(ModItems.PURE_ESSONITE.get())) {
            return 4;
        }
        if (stack.is(ModBlocks.ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.GRANITE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.ANDESITE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.DIORITE_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.TUFF_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.BASALT_ESSENITE_ORE.get().asItem())
                || stack.is(ModBlocks.ESSONITE_CRYSTAL.get().asItem())
                || stack.is(ModBlocks.ESSONITE_BLOCK.get().asItem())) {
            return 2;
        }
        return 0;
    }

    /**
     * @param purityPrimary chance in [0,1] that mass goes to primary (rest to waste)
     * @param byproductChance chance for rare gold nugget byproduct
     */
    public static Optional<Result> grind(ItemStack input, RandomSource random, float purityPrimary, float byproductChance) {
        int dust = dustCount(input);
        if (dust <= 0) {
            return Optional.empty();
        }
        ItemStack primary = ItemStack.EMPTY;
        ItemStack waste = ItemStack.EMPTY;
        int primaryCount = 0;
        int wasteCount = 0;
        for (int i = 0; i < dust; i++) {
            if (random.nextFloat() < purityPrimary) {
                primaryCount++;
            } else {
                wasteCount++;
            }
        }
        if (primaryCount > 0) {
            primary = new ItemStack(ModItems.ESSENITE_DUST.get(), primaryCount);
        }
        if (wasteCount > 0) {
            waste = new ItemStack(Items.COBBLESTONE, Math.max(1, wasteCount));
        }
        ItemStack byproduct = ItemStack.EMPTY;
        if (random.nextFloat() < byproductChance) {
            byproduct = new ItemStack(Items.GOLD_NUGGET);
        }
        return Optional.of(new Result(primary, byproduct, waste));
    }

    public static ItemLike wasteExample() {
        return Items.COBBLESTONE;
    }
}
