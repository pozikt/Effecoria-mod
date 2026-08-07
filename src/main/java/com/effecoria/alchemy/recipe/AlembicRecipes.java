package com.effecoria.alchemy.recipe;

import javax.annotation.Nullable;

import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeatLevel;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Baseline alembic matrix: water + power reagent (+ optional tags) → potion; heat tweaks duration. */
public final class AlembicRecipes {
    private AlembicRecipes() {}

    public static boolean isWater(ItemStack stack) {
        return stack.is(ModItems.PHI_FLASK_WATER.get()) || stack.is(ModItemTags.ALEMBIC_WATER);
    }

    public static boolean isPowerReagent(ItemStack stack) {
        return potionForPowerReagent(stack) != null || stack.is(ModItemTags.ALEMBIC_REAGENT_POWER);
    }

    @Nullable
    public static Item potionForPowerReagent(ItemStack stack) {
        if (stack.is(ModItems.ESSENITE_DUST.get())) {
            return ModItems.POTION_PHI_TONIC.get();
        }
        if (stack.is(ModItems.ESSONITE_SHARD.get())) {
            return ModItems.POTION_PHI_RESONANCE.get();
        }
        if (stack.is(ModItems.PURE_ESSONITE.get())) {
            return ModItems.POTION_PHI_STIMULANT.get();
        }
        return null;
    }

    /** Duration multiplier vs base potion: LOW 0.85, MED 1.0, HIGH 1.2. */
    public static float durationFactor(HeatLevel heat) {
        return switch (heat) {
            case LOW -> 0.85f;
            case HIGH -> 1.2f;
            case MEDIUM -> 1.0f;
            default -> 1.0f;
        };
    }

    /** Optional reagents are ignored in MVP but reserved for future flora / distillate. */
    public static boolean optionalReagentsOk(ItemStack r2, ItemStack r3) {
        return (r2.isEmpty() || r2.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL))
                && (r3.isEmpty() || r3.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL));
    }
}
