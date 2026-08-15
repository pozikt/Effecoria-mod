package com.effecoria.content;

import java.util.List;

import com.effecoria.core.fabricator.FabricatorCatalog;
import com.effecoria.core.fabricator.FabricatorRecipeDefinition;
import com.effecoria.core.fabricator.MemoryCrystalData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Ψ memory crystal — blank until a fabricator scans a sample item into it. */
public final class MemoryCrystalItem extends Item {
    public MemoryCrystalItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (MemoryCrystalData.isBlank(stack)) {
            tooltip.add(Component.translatable("item.effecoria.memory_crystal.blank"));
            return;
        }
        MemoryCrystalData.recipeId(stack).ifPresent(id -> {
            tooltip.add(Component.translatable("item.effecoria.memory_crystal.inscribed", id.toString()));
            FabricatorCatalog.byId(id).ifPresent(recipe -> {
                ItemStack result = FabricatorCatalog.resultStack(recipe);
                if (!result.isEmpty()) {
                    tooltip.add(Component.translatable(
                            "item.effecoria.memory_crystal.result",
                            result.getHoverName(),
                            recipe.resultCount()));
                }
                tooltip.add(Component.translatable("item.effecoria.memory_crystal.class", recipe.minClass()));
            });
        });
    }
}
