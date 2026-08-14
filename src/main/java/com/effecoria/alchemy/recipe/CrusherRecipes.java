package com.effecoria.alchemy.recipe;

import java.util.Optional;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Code-backed Φ-crusher recipes (coarse / fine). */
public final class CrusherRecipes {
    public enum Mode {
        COARSE(40, 1, 0.65f, 0.04f, 0.35f),
        FINE(160, 3, 0.92f, 0.12f, 0.08f);

        public final int ticks;
        public final int powerLoad;
        public final float yield;
        public final float byproductChance;
        public final float wasteChance;

        Mode(int ticks, int powerLoad, float yield, float byproductChance, float wasteChance) {
            this.ticks = ticks;
            this.powerLoad = powerLoad;
            this.yield = yield;
            this.byproductChance = byproductChance;
            this.wasteChance = wasteChance;
        }
    }

    public record Result(ItemStack primary, ItemStack byproduct, ItemStack waste, boolean omegaWork) {}

    private CrusherRecipes() {}

    public static boolean isInput(ItemStack stack) {
        return !stack.isEmpty() && footprint(stack, Mode.COARSE).isPresent();
    }

    public static Optional<Result> crush(ItemStack input, Mode mode, RandomSource random) {
        return resolve(input, mode, random, false);
    }

    /**
     * Deterministic worst-case outputs for inventory gating. Random byproduct/waste are assumed
     * present so a machine does not flicker when a per-tick roll sometimes needs a full slot.
     */
    public static Optional<Result> footprint(ItemStack input, Mode mode) {
        return resolve(input, mode, RandomSource.create(0L), true);
    }

    private static Optional<Result> resolve(
            ItemStack input, Mode mode, RandomSource random, boolean footprint) {
        Item item = input.getItem();
        boolean fine = mode == Mode.FINE;

        // Essonite ore family
        if (isEssoniteOre(item)) {
            int n = fine ? (footprint ? 5 : 4 + random.nextInt(2)) : (footprint ? 3 : 2 + random.nextInt(2));
            return Optional.of(roll(
                    new ItemStack(ModItems.ESSENITE_DUST.get(), n),
                    side(random, footprint, mode.byproductChance + (fine ? 0.06f : 0f), Items.GOLD_NUGGET),
                    wasteStone(random, mode, footprint),
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == ModItems.ESSONITE_SHARD.get()) {
            int n = fine ? 2 : 1;
            return Optional.of(roll(
                    new ItemStack(ModItems.ESSENITE_DUST.get(), n),
                    ItemStack.EMPTY,
                    wasteStone(random, mode, footprint),
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == ModItems.PURE_ESSONITE.get()) {
            int n = fine ? 6 : 3;
            return Optional.of(roll(
                    new ItemStack(ModItems.ESSENITE_DUST.get(), n),
                    side(random, footprint, fine ? 0.05f : 0.02f, ModItems.SOUL_SHARD.get()),
                    wasteStone(random, mode, footprint),
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == ModBlocks.PHI_STONE.get().asItem()) {
            return Optional.of(roll(
                    fine
                            ? new ItemStack(ModItems.PHI_STONE_GRIT.get(), 2)
                            : new ItemStack(ModBlocks.PHI_COBBLE.get(), 1),
                    side(random, footprint, fine ? 0.05f : 0.02f, ModItems.ESSENITE_DUST.get()),
                    ItemStack.EMPTY,
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == ModItems.DISTORTED_BONE.get()) {
            return Optional.of(roll(
                    fine
                            ? new ItemStack(ModItems.PHI_BONE_PASTE.get(), 1)
                            : new ItemStack(ModItems.BONE_GRIT.get(), 2),
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == ModBlocks.PHI_LOG.get().asItem()
                || item == ModBlocks.PHI_PLANKS.get().asItem()
                || item == ModBlocks.ANCIENT_ESSENCE_WOOD.get().asItem()) {
            return Optional.of(roll(
                    fine
                            ? new ItemStack(ModItems.PHI_FIBER.get(), 1)
                            : new ItemStack(ModItems.PHI_WOOD_SHAVINGS.get(), 2),
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == ModBlocks.VOID_OBSIDIAN.get().asItem()) {
            return Optional.of(roll(
                    fine
                            ? new ItemStack(ModItems.OMEGA_DUST.get(), 1)
                            : new ItemStack(ModItems.OBSIDIAN_GRIT.get(), 1),
                    side(random, footprint, fine ? 0.05f : 0.02f, ModItems.OMEGA_NUGGET.get()),
                    ItemStack.EMPTY,
                    true,
                    mode,
                    random,
                    footprint));
        }
        if (item == Blocks.STONE.asItem() || item == Blocks.COBBLESTONE.asItem()) {
            return Optional.of(roll(
                    fine ? new ItemStack(Items.SAND) : new ItemStack(Items.GRAVEL),
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    false,
                    mode,
                    random,
                    footprint));
        }
        if (item == Items.GRAVEL) {
            if (!fine) {
                return Optional.empty();
            }
            return Optional.of(roll(
                    new ItemStack(Items.SAND), ItemStack.EMPTY, ItemStack.EMPTY, false, mode, random, footprint));
        }
        return Optional.empty();
    }

    private static boolean isEssoniteOre(Item item) {
        return item == ModBlocks.ESSENITE_ORE.get().asItem()
                || item == ModBlocks.DEEPSLATE_ESSENITE_ORE.get().asItem()
                || item == ModBlocks.GRANITE_ESSENITE_ORE.get().asItem()
                || item == ModBlocks.ANDESITE_ESSENITE_ORE.get().asItem()
                || item == ModBlocks.DIORITE_ESSENITE_ORE.get().asItem()
                || item == ModBlocks.TUFF_ESSENITE_ORE.get().asItem()
                || item == ModBlocks.BASALT_ESSENITE_ORE.get().asItem()
                || item == ModBlocks.ESSONITE_CRYSTAL.get().asItem()
                || item == ModBlocks.ESSONITE_BLOCK.get().asItem();
    }

    private static ItemStack wasteStone(RandomSource random, Mode mode, boolean footprint) {
        if (footprint) {
            return mode.wasteChance > 0f ? new ItemStack(Items.COBBLESTONE) : ItemStack.EMPTY;
        }
        if (random.nextFloat() < mode.wasteChance) {
            return new ItemStack(Items.COBBLESTONE);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack side(RandomSource random, boolean footprint, float p, Item item) {
        if (footprint) {
            return p > 0f ? new ItemStack(item) : ItemStack.EMPTY;
        }
        return random.nextFloat() < p ? new ItemStack(item) : ItemStack.EMPTY;
    }

    private static Result roll(
            ItemStack primary,
            ItemStack byproduct,
            ItemStack waste,
            boolean omega,
            Mode mode,
            RandomSource random,
            boolean footprint) {
        // Soft yield clamp for coarse: sometimes lose one primary (not in footprint — keep max).
        if (!footprint
                && !primary.isEmpty()
                && mode == Mode.COARSE
                && primary.getCount() > 1
                && random.nextFloat() > mode.yield) {
            primary.shrink(1);
        }
        return new Result(primary, byproduct, waste, omega);
    }
}
