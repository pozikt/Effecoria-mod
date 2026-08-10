package com.effecoria.client.gui.alchemy;

import com.effecoria.content.ModItems;
import com.effecoria.core.artifact.ArtifactCatalog;
import com.effecoria.core.artifact.FocusCutDefinition;
import com.effecoria.core.artifact.ModularPartData;
import com.effecoria.core.artifact.ShaftFormDefinition;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Preview stacks for stonecutter-style variant grids. */
public final class ArtifactPreviewIcons {
    private ArtifactPreviewIcons() {}

    public static ItemStack shaftOption(ShaftFormDefinition form, ItemStack materialIn) {
        Item material = materialIn.isEmpty() ? Items.STICK : materialIn.getItem();
        return ModularPartData.createShaft(material, form.id(), form.lengthMeters());
    }

    public static ItemStack focusOption(FocusCutDefinition cut, ItemStack materialIn) {
        Item material = materialIn.isEmpty() ? Items.AMETHYST_SHARD : materialIn.getItem();
        return ModularPartData.createFocus(material, cut.id());
    }

    public static ItemStack assemblerTemplate(int template) {
        return switch (template) {
            case 0 -> new ItemStack(ModItems.MODULAR_STAFF.get());
            case 1 -> new ItemStack(ModItems.ASSEMBLED_RING.get());
            case 2 -> new ItemStack(ModItems.ASSEMBLED_AMULET.get());
            case 3 -> new ItemStack(ModItems.ASSEMBLED_CHARM.get());
            default -> ItemStack.EMPTY;
        };
    }

    public static boolean focusOptionValid(FocusCutDefinition cut, ItemStack materialIn) {
        return materialIn.isEmpty() || ArtifactCatalog.materialMatchesFocus(materialIn, cut);
    }

    public static boolean shaftOptionValid(ShaftFormDefinition form, ItemStack materialIn) {
        return materialIn.isEmpty() || ArtifactCatalog.materialMatchesShaft(materialIn, form);
    }
}
