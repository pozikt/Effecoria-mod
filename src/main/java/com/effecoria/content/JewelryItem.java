package com.effecoria.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/** Curios-wearable jewelry with a passive hint. */
public class JewelryItem extends Item implements ICurioItem {
    private final String hintKey;
    private final float phiShieldBonus;

    public JewelryItem(Properties properties, String hintKey, float phiShieldBonus) {
        super(properties.stacksTo(1));
        this.hintKey = hintKey;
        this.phiShieldBonus = phiShieldBonus;
    }

    public float phiShieldBonus() {
        return phiShieldBonus;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey));
        tooltip.add(Component.translatable("item.effecoria.curios_wear_hint"));
    }
}
