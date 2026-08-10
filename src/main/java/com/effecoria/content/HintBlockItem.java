package com.effecoria.content;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** BlockItem with a single translation-key tooltip line. */
public final class HintBlockItem extends BlockItem {
    private final String hintKey;

    public HintBlockItem(Block block, Properties properties, String hintKey) {
        super(block, properties);
        this.hintKey = hintKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey));
    }
}
