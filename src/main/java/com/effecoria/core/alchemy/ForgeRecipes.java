package com.effecoria.core.alchemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Hardcoded v1 process table for the Forge Reactor. */
public final class ForgeRecipes {
    public enum Mode {
        ENERGY,
        SMELT,
        SYNTH,
        CLEANSE;

        public Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public record Recipe(Mode mode, Item in1, int n1, Item in2, int n2, ItemStack out, int durationTicks) {}

    private static final List<Recipe> RECIPES = new ArrayList<>();

    static {
        // SMELT
        for (Item ore : List.of(
                ModItems.ESSENITE_ORE.get(),
                ModItems.DEEPSLATE_ESSENITE_ORE.get(),
                ModItems.GRANITE_ESSENITE_ORE.get(),
                ModItems.ANDESITE_ESSENITE_ORE.get(),
                ModItems.DIORITE_ESSENITE_ORE.get(),
                ModItems.TUFF_ESSENITE_ORE.get(),
                ModItems.BASALT_ESSENITE_ORE.get())) {
            RECIPES.add(new Recipe(Mode.SMELT, ore, 1, Items.AIR, 0, new ItemStack(ModItems.PURE_ESSONITE.get()), 400));
        }
        RECIPES.add(new Recipe(
                Mode.SMELT, ModItems.MITHRIL_ORE.get(), 1, Items.AIR, 0, new ItemStack(ModItems.MITHRIL_INGOT.get()), 500));
        RECIPES.add(new Recipe(
                Mode.SMELT,
                ModItems.DEEPSLATE_MITHRIL_ORE.get(),
                1,
                Items.AIR,
                0,
                new ItemStack(ModItems.MITHRIL_INGOT.get()),
                500));
        RECIPES.add(new Recipe(
                Mode.SMELT, ModItems.RAW_MITHRIL.get(), 1, Items.AIR, 0, new ItemStack(ModItems.MITHRIL_INGOT.get()), 300));
        RECIPES.add(new Recipe(
                Mode.SMELT,
                ModItems.VOID_OBSIDIAN.get(),
                1,
                Items.AIR,
                0,
                new ItemStack(ModItems.PURIFIED_OBSIDIAN.get()),
                600));

        // SYNTH — gold stands in for silver (no silver in mod yet)
        RECIPES.add(new Recipe(
                Mode.SYNTH,
                Items.GOLD_INGOT,
                1,
                ModItems.STAR_ESSONITE.get(),
                1,
                new ItemStack(ModItems.MITHRIL_INGOT.get()),
                800));
        RECIPES.add(new Recipe(
                Mode.SYNTH,
                Items.IRON_INGOT,
                1,
                ModItems.ESSENITE_DUST.get(),
                2,
                new ItemStack(ModItems.PHI_STEEL_INGOT.get()),
                600));
        RECIPES.add(new Recipe(
                Mode.SYNTH,
                Items.DIAMOND,
                1,
                ModItems.PURE_ESSONITE.get(),
                1,
                new ItemStack(ModItems.STAR_ESSONITE.get()),
                2400));

        // CLEANSE
        RECIPES.add(new Recipe(
                Mode.CLEANSE,
                ModItems.OMEGA_TAINTED_OBSIDIAN.get(),
                1,
                Items.AIR,
                0,
                new ItemStack(ModItems.VOID_OBSIDIAN.get()),
                500));
        RECIPES.add(new Recipe(
                Mode.CLEANSE,
                ModItems.OMEGA_DUST.get(),
                1,
                Items.AIR,
                0,
                new ItemStack(ModItems.ESSENITE_DUST.get()),
                400));
        RECIPES.add(new Recipe(
                Mode.CLEANSE,
                ModItems.OMEGA_CRYSTAL_SHARD.get(),
                1,
                Items.AIR,
                0,
                new ItemStack(ModItems.ESSENITE_DUST.get(), 2),
                450));
    }

    private ForgeRecipes() {}

    public static Optional<Recipe> match(Mode mode, ItemStack a, ItemStack b) {
        if (mode == Mode.ENERGY) {
            return Optional.empty();
        }
        for (Recipe recipe : RECIPES) {
            if (recipe.mode() != mode) {
                continue;
            }
            if (matches(recipe, a, b) || matches(recipe, b, a)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(Recipe recipe, ItemStack primary, ItemStack secondary) {
        if (!primary.is(recipe.in1()) || primary.getCount() < recipe.n1()) {
            return false;
        }
        if (recipe.in2() == Items.AIR || recipe.n2() <= 0) {
            return true;
        }
        return secondary.is(recipe.in2()) && secondary.getCount() >= recipe.n2();
    }

    public static void consume(Recipe recipe, ItemStack a, ItemStack b) {
        if (a.is(recipe.in1()) && a.getCount() >= recipe.n1()) {
            a.shrink(recipe.n1());
            if (recipe.n2() > 0) {
                b.shrink(recipe.n2());
            }
            return;
        }
        if (b.is(recipe.in1()) && b.getCount() >= recipe.n1()) {
            b.shrink(recipe.n1());
            if (recipe.n2() > 0) {
                a.shrink(recipe.n2());
            }
        }
    }
}
