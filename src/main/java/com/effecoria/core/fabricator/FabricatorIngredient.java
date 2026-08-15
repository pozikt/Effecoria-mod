package com.effecoria.core.fabricator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** One ingredient line in a fabricator recipe (item or tag + count). */
public record FabricatorIngredient(@Nullable ResourceLocation itemId, @Nullable TagKey<Item> tag, int count) {
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || count <= 0) {
            return false;
        }
        if (itemId != null) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId);
        }
        return tag != null && stack.is(tag);
    }

    public static FabricatorIngredient ofItem(ResourceLocation itemId, int count) {
        return new FabricatorIngredient(itemId, null, Math.max(1, count));
    }

    public static FabricatorIngredient ofTag(ResourceLocation tagId, int count) {
        return new FabricatorIngredient(null, TagKey.create(Registries.ITEM, tagId), Math.max(1, count));
    }
}
