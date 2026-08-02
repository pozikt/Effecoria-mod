package com.effecoria.effect.spatial;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Personal spatial pocket — 9 slots, weightless stash bound to the mage. */
public final class SpatialPocketData {
    public static final int SIZE = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public static SpatialPocketData createDefault() {
        return new SpatialPocketData();
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public SimpleContainer asContainer() {
        SimpleContainer container = new SimpleContainer(SIZE);
        for (int i = 0; i < SIZE; i++) {
            container.setItem(i, items.get(i).copy());
        }
        container.addListener(c -> {
            for (int i = 0; i < SIZE; i++) {
                items.set(i, container.getItem(i).copy());
            }
        });
        return container;
    }

    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        items.clear();
        if (tag.contains("Items", Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(tag, items, provider);
        }
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, provider);
        return tag;
    }
}
