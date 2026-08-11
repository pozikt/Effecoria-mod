package com.effecoria.core.alchemy;

import com.effecoria.content.ModItems;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Φ-artifact defense turret kinds (barrel / emitter type). */
public enum TurretKind implements StringRepresentable {
    PLASMA(24, 20, 20, 8, false, 1.0f),
    KINETIC(32, 40, 40, 12, true, 1.0f),
    SPATIAL(16, 200, 100, 18, false, 1.5f),
    MENTAL(28, 30, 25, 6, false, 1.0f),
    OMEGA(20, 120, 60, 14, true, 1.0f),
    /** Mount has no barrel yet. */
    NONE(0, 0, 0, 0, false, 1.0f);

    private final int range;
    private final int fuelCost;
    private final int cooldownTicks;
    private final int heatPerShot;
    private final boolean needsAmmo;
    private final float minPowerFactor;

    TurretKind(int range, int fuelCost, int cooldownTicks, int heatPerShot, boolean needsAmmo, float minPowerFactor) {
        this.range = range;
        this.fuelCost = fuelCost;
        this.cooldownTicks = cooldownTicks;
        this.heatPerShot = heatPerShot;
        this.needsAmmo = needsAmmo;
        this.minPowerFactor = minPowerFactor;
    }

    public int range() {
        return range;
    }

    public int fuelCost() {
        return fuelCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int heatPerShot() {
        return heatPerShot;
    }

    public boolean needsAmmo() {
        return needsAmmo;
    }

    public float minPowerFactor() {
        return minPowerFactor;
    }

    public boolean isEmitter() {
        return this != NONE;
    }

    public boolean isValidAmmo(ItemStack stack) {
        if (!needsAmmo || stack.isEmpty()) {
            return !needsAmmo;
        }
        return switch (this) {
            case KINETIC -> stack.is(ModItems.MITHRIL_BOLT.get())
                    || stack.is(ModItems.MITHRIL_NUGGET.get())
                    || stack.is(Items.IRON_NUGGET);
            case OMEGA -> stack.is(ModItems.OMEGA_DUST.get())
                    || stack.is(ModItems.OMEGA_TAINTED_OBSIDIAN.get())
                    || stack.is(ModItems.OMEGA_CRYSTAL_SHARD.get());
            default -> false;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public static TurretKind fromBlockId(String path) {
        return switch (path) {
            case "kinetic_turret" -> KINETIC;
            case "spatial_turret" -> SPATIAL;
            case "mental_turret" -> MENTAL;
            case "omega_turret" -> OMEGA;
            case "plasma_turret" -> PLASMA;
            default -> NONE;
        };
    }
}
