package com.effecoria.content;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;

/** Simple furnace/campfire fuel item, optional tooltip hint. */
public final class FuelItem extends Item {
    private final int burnTime;
    @Nullable
    private final String hintKey;

    public FuelItem(Properties properties, int burnTime) {
        this(properties, burnTime, null);
    }

    public FuelItem(Properties properties, int burnTime, @Nullable String hintKey) {
        super(properties);
        this.burnTime = burnTime;
        this.hintKey = hintKey;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return burnTime;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (hintKey != null) {
            tooltip.add(Component.translatable(hintKey));
        }
    }
}
