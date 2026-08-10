package com.effecoria.content;

import com.effecoria.core.artifact.AssembledGearData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Assembled jewelry that also occupies Curios slots via tags. */
public class AssembledJewelryItem extends JewelryItem {
    private final String curiosSlot;

    public AssembledJewelryItem(Properties properties, String hintKey, float phiShieldBonus, String curiosSlot) {
        super(properties, hintKey, phiShieldBonus);
        this.curiosSlot = curiosSlot;
    }

    public String curiosSlot() {
        return curiosSlot;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (AssembledGearData.hasGearConductivity(stack) || AssembledGearData.isAssembled(stack)) {
            float c = AssembledGearData.conductivity(stack);
            tooltip.add(Component.translatable(
                    "item.effecoria.modular_part.conductivity", String.format("%.0f%%", c * 100f)));
        }
        AssembledGearData.seals(stack).forEach((id, lvl) -> tooltip.add(Component.translatable(
                "item_seal.effecoria." + id.getPath())));
    }
}
