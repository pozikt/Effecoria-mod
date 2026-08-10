package com.effecoria.content;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Item with a single translation-key tooltip line. */
public final class HintItem extends Item {
    private final String hintKey;

    public HintItem(Properties properties, String hintKey) {
        super(properties);
        this.hintKey = hintKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey));
    }
}
