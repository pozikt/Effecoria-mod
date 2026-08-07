package com.effecoria.alchemy.recipe;

import java.util.UUID;

import javax.annotation.Nullable;

import com.effecoria.content.BloodVialEmptyItem;
import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeatLevel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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

    /** Blood optional reagents extend brew duration (Φ-capacity of the sample). */
    public static float bloodDurationMultiplier(ItemStack r2, ItemStack r3) {
        float mult = 1f;
        for (ItemStack stack : new ItemStack[] {r2, r3}) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.WYVERN_BLOOD_VIAL.get())) {
                mult = Math.max(mult, 1.5f);
            } else if (stack.is(ModItems.MAGE_BLOOD_VIAL.get())) {
                mult = Math.max(mult, 1.35f);
            } else if (stack.is(ModItems.BLOOD_VIAL.get()) || stack.is(ModItemTags.ALEMBIC_REAGENT_BLOOD)) {
                mult = Math.max(mult, 1.1f);
            }
        }
        return mult;
    }

    /** Prefer mage / wyvern donor UUID for Ψ-anchor potions. */
    @Nullable
    public static UUID bloodDonorUuid(ItemStack r2, ItemStack r3) {
        UUID donor = null;
        float best = 0f;
        for (ItemStack stack : new ItemStack[] {r2, r3}) {
            if (stack.isEmpty() || !stack.is(ModItemTags.ALEMBIC_REAGENT_BLOOD)) {
                continue;
            }
            float rank = stack.is(ModItems.WYVERN_BLOOD_VIAL.get())
                    ? 3f
                    : stack.is(ModItems.MAGE_BLOOD_VIAL.get()) ? 2f : 1f;
            if (rank < best) {
                continue;
            }
            CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            if (custom == null) {
                continue;
            }
            CompoundTag tag = custom.copyTag();
            if (tag.hasUUID(BloodVialEmptyItem.NBT_DONOR)) {
                donor = tag.getUUID(BloodVialEmptyItem.NBT_DONOR);
                best = rank;
            }
        }
        return donor;
    }

    /** Optional reagents are ignored in MVP but reserved for future flora / distillate. */
    public static boolean optionalReagentsOk(ItemStack r2, ItemStack r3) {
        return (r2.isEmpty() || r2.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL))
                && (r3.isEmpty() || r3.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL));
    }
}
