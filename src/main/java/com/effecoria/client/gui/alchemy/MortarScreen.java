package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.MortarMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Mortar GUI — slot coords must match {@link MortarMenu} and {@code textures/gui/mortar.png}. */
public final class MortarScreen extends AbstractContainerScreen<MortarMenu> {
    private static final int ARROW_X = 74;
    private static final int ARROW_Y = 35;

    public MortarScreen(MortarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.MORTAR, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ARROW_X, ARROW_Y, ratio, 0xFF55AAFF);
        if (menu.autoMode()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.mortar.auto"),
                    leftPos + 8,
                    topPos + 18,
                    0x3A6A8A,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(26, 53, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.mortar.drive"), mouseX, mouseY);
        } else if (isHovering(44, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.mortar.input"), mouseX, mouseY);
        } else if (isHovering(116, 17, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.mortar.primary"), mouseX, mouseY);
        } else if (isHovering(134, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.mortar.byproduct"), mouseX, mouseY);
        } else if (isHovering(116, 53, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.mortar.waste"), mouseX, mouseY);
        }
    }
}
