package com.effecoria.content;

import com.effecoria.core.artifact.ModularPartData;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Carved shaft / faceted focus / jewelry blanks with part NBT. */
public class ModularPartItem extends Item {
    private final String kindHint;

    public ModularPartItem(Properties properties, String kindHint) {
        super(properties);
        this.kindHint = kindHint;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.modular_part." + kindHint));
        if (ModularPartData.isPart(stack)) {
            tooltip.add(Component.literal(ModularPartData.formOrCut(stack).toString()));
            tooltip.add(Component.literal(ModularPartData.material(stack).toString()));
            ModularPartData.phonemes(stack).forEach(p -> tooltip.add(Component.translatable(
                    "item.effecoria.essonite_armor.phoneme." + p.id())));
        }
    }
}
