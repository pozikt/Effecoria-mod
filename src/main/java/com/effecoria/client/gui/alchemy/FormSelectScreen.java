package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.FormSelectMenu;
import com.effecoria.core.artifact.ArtifactCatalog;
import com.effecoria.core.artifact.FocusCutDefinition;
import com.effecoria.core.artifact.ShaftFormDefinition;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shaft lathe / facet cutter — stonecutter panel, input/output slots, clickable variant grid. */
public final class FormSelectScreen extends AbstractContainerScreen<FormSelectMenu> {

    public FormSelectScreen(FormSelectMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = ArtifactStationGui.WIDTH;
        this.imageHeight = ArtifactStationGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FormSelectMenu.BUTTON_PREV))
                .bounds(
                        leftPos + ArtifactStationGui.SCROLL_LEFT_X,
                        topPos + ArtifactStationGui.SCROLL_Y,
                        ArtifactStationGui.SCROLL_W,
                        ArtifactStationGui.SCROLL_H)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FormSelectMenu.BUTTON_NEXT))
                .bounds(
                        leftPos + ArtifactStationGui.SCROLL_RIGHT_X,
                        topPos + ArtifactStationGui.SCROLL_Y,
                        ArtifactStationGui.SCROLL_W,
                        ArtifactStationGui.SCROLL_H)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ArtifactStationGui.blitStonecutter(graphics, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ArtifactStationGui.ARROW_X, ArtifactStationGui.ARROW_Y, ratio, 0xFF6E6E6E);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderVariantPool(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            int count = variantCount();
            for (int i = 0; i < count; i++) {
                if (StonecutterRecipeGrid.hit(leftPos, topPos, i, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId, FormSelectMenu.SELECT_INDEX_BASE + i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderVariantPool(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack material = menu.getSlot(0).getItem();
        int selected = menu.formIndex();
        int count = variantCount();
        if (count <= 0) {
            return;
        }
        selected = Math.floorMod(selected, count);
        StonecutterRecipeGrid.blitSelected(graphics, leftPos, topPos, selected);

        for (int i = 0; i < count; i++) {
            int x = StonecutterRecipeGrid.cellX(leftPos, i);
            int y = StonecutterRecipeGrid.cellY(topPos, i);
            ItemStack icon = variantIcon(i, material);
            boolean valid = variantValid(i, material);
            if (!valid) {
                graphics.fill(x, y, x + StonecutterRecipeGrid.CELL, y + StonecutterRecipeGrid.CELL, 0x99000000);
            }
            graphics.renderItem(icon, x, y);
            graphics.renderItemDecorations(font, icon, x, y);
            if (StonecutterRecipeGrid.hit(leftPos, topPos, i, mouseX, mouseY)) {
                graphics.renderTooltip(font, variantTooltip(i), mouseX, mouseY);
            }
        }
    }

    private int variantCount() {
        if (menu.mode() == FormSelectMenu.Mode.LATHE) {
            return ArtifactCatalog.shaftForms().size();
        }
        return ArtifactCatalog.focusCuts().size();
    }

    private ItemStack variantIcon(int index, ItemStack material) {
        if (menu.mode() == FormSelectMenu.Mode.LATHE) {
            List<ShaftFormDefinition> forms = ArtifactCatalog.shaftForms();
            if (forms.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ShaftFormDefinition form = forms.get(Math.floorMod(index, forms.size()));
            return ArtifactPreviewIcons.shaftOption(form, material);
        }
        List<FocusCutDefinition> cuts = ArtifactCatalog.focusCuts();
        if (cuts.isEmpty()) {
            return ItemStack.EMPTY;
        }
        FocusCutDefinition cut = cuts.get(Math.floorMod(index, cuts.size()));
        return ArtifactPreviewIcons.focusOption(cut, material);
    }

    private boolean variantValid(int index, ItemStack material) {
        if (menu.mode() == FormSelectMenu.Mode.LATHE) {
            List<ShaftFormDefinition> forms = ArtifactCatalog.shaftForms();
            if (forms.isEmpty()) {
                return false;
            }
            return ArtifactPreviewIcons.shaftOptionValid(forms.get(Math.floorMod(index, forms.size())), material);
        }
        List<FocusCutDefinition> cuts = ArtifactCatalog.focusCuts();
        if (cuts.isEmpty()) {
            return false;
        }
        return ArtifactPreviewIcons.focusOptionValid(cuts.get(Math.floorMod(index, cuts.size())), material);
    }

    private Component variantTooltip(int index) {
        if (menu.mode() == FormSelectMenu.Mode.LATHE) {
            List<ShaftFormDefinition> forms = ArtifactCatalog.shaftForms();
            if (forms.isEmpty()) {
                return Component.literal("?");
            }
            ShaftFormDefinition form = forms.get(Math.floorMod(index, forms.size()));
            return Component.translatable("gui.effecoria.shaft_form." + form.id().getPath())
                    .append(Component.literal(String.format(" · %.1fm", form.lengthMeters())));
        }
        List<FocusCutDefinition> cuts = ArtifactCatalog.focusCuts();
        if (cuts.isEmpty()) {
            return Component.literal("?");
        }
        FocusCutDefinition cut = cuts.get(Math.floorMod(index, cuts.size()));
        return Component.translatable("gui.effecoria.focus_cut." + cut.id().getPath());
    }
}
