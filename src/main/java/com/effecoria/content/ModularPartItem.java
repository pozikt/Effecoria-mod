package com.effecoria.content;

import com.effecoria.core.artifact.ArtifactCatalog;
import com.effecoria.core.artifact.MaterialConductivity;
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
        if (!ModularPartData.isPart(stack)) {
            return;
        }
        tooltip.add(Component.translatable(
                "item.effecoria.modular_part.material", ModularPartData.material(stack).getPath()));
        float length = ModularPartData.lengthMeters(stack);
        if (length > 0f) {
            tooltip.add(Component.translatable("item.effecoria.modular_part.length", String.format("%.1f", length)));
            ArtifactCatalog.shaftForm(ModularPartData.formOrCut(stack)).ifPresent(form -> tooltip.add(
                    Component.translatable("gui.effecoria.shaft_form." + form.id().getPath())));
        } else {
            tooltip.add(Component.literal(ModularPartData.formOrCut(stack).getPath()));
        }
        float c = MaterialConductivity.ofStack(stack);
        tooltip.add(Component.translatable(
                "item.effecoria.modular_part.conductivity", String.format("%.0f%%", c * 100f)));
        ModularPartData.phonemes(stack).forEach(p -> tooltip.add(Component.translatable(
                "item.effecoria.essonite_armor.phoneme." + p.id())));
    }
}
