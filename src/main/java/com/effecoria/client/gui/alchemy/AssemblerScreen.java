package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.AssemblerMenu;
import com.effecoria.block.ArtifactAssemblerBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Artifact assembler — technomagic Φ-bonding bay with blueprint tabs (staff + Curios jewelry). */
public final class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {
    public AssemblerScreen(AssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AssemblerGui.WIDTH;
        this.imageHeight = AssemblerGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AssemblerGui.drawShell(graphics, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        long time = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        AssemblerGui.drawBondProgress(graphics, leftPos, topPos, ratio, time);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, TowerChrome.TITLE, false);
        Component status = statusLine();
        int statusColor =
                menu.progress() > 0 ? TowerChrome.ACCENT : TowerChrome.MUTED;
        graphics.drawString(font, status, 8, 70, statusColor, false);
        graphics.drawString(
                font,
                playerInventoryTitle,
                8,
                inventoryLabelY,
                TowerChrome.LABEL,
                false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int selected = Math.floorMod(menu.template(), AssemblerGui.TEMPLATE_COUNT);
        AssemblerGui.drawTemplateTabs(graphics, font, leftPos, topPos, selected, mouseX, mouseY);
        renderTemplateIcons(graphics, mouseX, mouseY);
        renderSlotHints(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            for (int i = 0; i < AssemblerGui.TEMPLATE_COUNT; i++) {
                if (AssemblerGui.templateHit(leftPos, topPos, i, mouseX, mouseY)) {
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

    private Component statusLine() {
        if (menu.progress() > 0) {
            int pct = Math.round(100f * menu.progress() / menu.maxProgress());
            return Component.translatable("gui.effecoria.assembler.bonding", pct);
        }
        return Component.translatable(
                "gui.effecoria.assembler.ready",
                AssemblerGui.templateName(Math.floorMod(menu.template(), AssemblerGui.TEMPLATE_COUNT)));
    }

    private void renderTemplateIcons(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < AssemblerGui.TEMPLATE_COUNT; i++) {
            int x = leftPos + AssemblerGui.chipX(i) + AssemblerGui.CHIP_W - 12;
            int y = topPos + AssemblerGui.CHIP_Y + 1;
            ItemStack icon = ArtifactPreviewIcons.assemblerTemplate(i);
            graphics.renderItem(icon, x, y);
            if (AssemblerGui.templateHit(leftPos, topPos, i, mouseX, mouseY)) {
                graphics.renderTooltip(font, AssemblerGui.templateName(i), mouseX, mouseY);
            }
        }
    }

    private void renderSlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.template() == ArtifactAssemblerBlockEntity.TEMPLATE_STAFF) {
            hint(graphics, mouseX, mouseY, AssemblerGui.SLOT_A_X, AssemblerGui.SLOT_A_Y, "gui.effecoria.assembler.slot.shaft");
            hint(graphics, mouseX, mouseY, AssemblerGui.SLOT_B_X, AssemblerGui.SLOT_B_Y, "gui.effecoria.assembler.slot.focus");
        } else {
            hint(graphics, mouseX, mouseY, AssemblerGui.SLOT_A_X, AssemblerGui.SLOT_A_Y, "gui.effecoria.assembler.slot.band");
            hint(graphics, mouseX, mouseY, AssemblerGui.SLOT_B_X, AssemblerGui.SLOT_B_Y, "gui.effecoria.assembler.slot.gem");
        }
        hint(graphics, mouseX, mouseY, AssemblerGui.SLOT_OUT_X, AssemblerGui.SLOT_OUT_Y, "gui.effecoria.assembler.slot.output");
    }

    private void hint(GuiGraphics graphics, int mouseX, int mouseY, int slotX, int slotY, String key) {
        if (isHovering(slotX, slotY, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        }
    }
}
