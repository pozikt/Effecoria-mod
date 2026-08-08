package com.effecoria.armor;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Φ-contour armor piece with a fixed tier. */
public class EssoniteArmorItem extends ArmorItem {
    private final EssoniteArmorTier tier;

    public EssoniteArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, EssoniteArmorTier tier) {
        super(material, type, properties);
        this.tier = tier;
    }

    public EssoniteArmorTier tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EssoniteArmorData.appendTooltip(stack, tier, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
