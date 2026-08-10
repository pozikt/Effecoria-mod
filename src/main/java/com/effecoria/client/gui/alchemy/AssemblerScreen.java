package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.AssemblerMenu;
import com.effecoria.block.ArtifactAssemblerBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Artifact assembler — mortar panel, template pool (staff + Curios jewelry), three craft slots. */
public final class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {
    private static final int ARROW_X = 98;
    private static final int ARROW_Y = 35;
    private static final int TEMPLATE_COUNT = 4;

    public AssemblerScreen(AssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.MORTAR, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ARROW_X, ARROW_Y, ratio, 0xFF88CCFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTemplatePool(graphics, mouseX, mouseY);
        renderSlotHints(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            for (int i = 0; i < TEMPLATE_COUNT; i++) {
                if (StonecutterRecipeGrid.hit(leftPos, topPos, i, mouseX, mouseY)) {
                    int menuButton = switch (i) {
                        case 0 -> AssemblerMenu.BUTTON_STAFF;
                        case 1 -> AssemblerMenu.BUTTON_RING;
                        case 2 -> AssemblerMenu.BUTTON_AMULET;
                        default -> AssemblerMenu.BUTTON_CHARM;
                    };
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menuButton);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplatePool(GuiGraphics graphics, int mouseX, int mouseY) {
        int selected = Math.floorMod(menu.template(), TEMPLATE_COUNT);
        StonecutterRecipeGrid.blitSelected(graphics, leftPos, topPos, selected);
        for (int i = 0; i < TEMPLATE_COUNT; i++) {
            int x = StonecutterRecipeGrid.cellX(leftPos, i);
            int y = StonecutterRecipeGrid.cellY(topPos, i);
            ItemStack icon = ArtifactPreviewIcons.assemblerTemplate(i);
            graphics.renderItem(icon, x, y);
            graphics.renderItemDecorations(font, icon, x, y);
            if (StonecutterRecipeGrid.hit(leftPos, topPos, i, mouseX, mouseY)) {
                graphics.renderTooltip(font, templateTooltip(i), mouseX, mouseY);
            }
        }
    }

    private Component templateTooltip(int template) {
        return switch (template) {
            case 0 -> Component.translatable("gui.effecoria.assembler.staff");
            case 1 -> Component.translatable("gui.effecoria.assembler.ring");
            case 2 -> Component.translatable("gui.effecoria.assembler.amulet");
            default -> Component.translatable("gui.effecoria.assembler.charm");
        };
    }

    private void renderSlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.template() == ArtifactAssemblerBlockEntity.TEMPLATE_STAFF) {
            hint(graphics, mouseX, mouseY, 44, 35, "gui.effecoria.assembler.slot.shaft");
            hint(graphics, mouseX, mouseY, 80, 35, "gui.effecoria.assembler.slot.focus");
        } else {
            hint(graphics, mouseX, mouseY, 44, 35, "gui.effecoria.assembler.slot.band");
            hint(graphics, mouseX, mouseY, 80, 35, "gui.effecoria.assembler.slot.gem");
        }
        hint(graphics, mouseX, mouseY, 134, 35, "gui.effecoria.assembler.slot.output");
    }

    private void hint(GuiGraphics graphics, int mouseX, int mouseY, int slotX, int slotY, String key) {
        if (isHovering(slotX, slotY, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        }
    }
}
