package com.effecoria.content;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Heartwood of an ancient canopy giant — phylactery feedstock. */
public final class AncientHeartwoodItem extends Item {
    public AncientHeartwoodItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.ancient_heartwood.hint"));
    }
}
