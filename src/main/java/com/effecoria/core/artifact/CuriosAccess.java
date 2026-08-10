package com.effecoria.core.artifact;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Thin Curios lookups for jewelry passives. */
public final class CuriosAccess {
    private CuriosAccess() {}

    public static boolean hasEquipped(LivingEntity entity, Item item) {
        return findEquipped(entity, stack -> stack.is(item)).isPresent();
    }

    public static boolean hasEquipped(LivingEntity entity, Predicate<ItemStack> predicate) {
        return findEquipped(entity, predicate).isPresent();
    }

    public static Optional<ItemStack> findEquipped(LivingEntity entity, Predicate<ItemStack> predicate) {
        AtomicReference<ItemStack> found = new AtomicReference<>(ItemStack.EMPTY);
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
            Optional<SlotResult> result = inv.findFirstCurio(predicate);
            result.ifPresent(slot -> found.set(slot.stack()));
        });
        ItemStack stack = found.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
    }

    public static boolean anyEquipped(LivingEntity entity, Item... items) {
        AtomicBoolean hit = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> {
            for (Item item : items) {
                if (inv.findFirstCurio(item).isPresent()) {
                    hit.set(true);
                    return;
                }
            }
        });
        return hit.get();
    }
}
