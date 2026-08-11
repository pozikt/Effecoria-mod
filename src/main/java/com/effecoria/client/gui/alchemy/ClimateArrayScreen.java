package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.ClimateArrayMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Climate Array GUI — cycle weather mode + activate. */
public final class ClimateArrayScreen extends AbstractContainerScreen<ClimateArrayMenu> {
    public ClimateArrayScreen(ClimateArrayMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.empty(), b -> cycle())
                .bounds(leftPos + 28, topPos + 52, 54, 16)
                .build());
        addRenderableWidget(Button.builder(Component.empty(), b -> activate())
                .bounds(leftPos + 94, topPos + 52, 54, 16)
                .build());
    }

    private void cycle() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ClimateArrayMenu.BUTTON_CYCLE);
        }
    }

    private void activate() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ClimateArrayMenu.BUTTON_ACTIVATE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);

        Component modeLabel = Component.translatable("gui.effecoria.climate_array.mode." + menu.mode().id());
        graphics.drawCenteredString(font, modeLabel, leftPos + imageWidth / 2, topPos + 28, 0x404040);

        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.climate_array.cycle"), leftPos + 55, topPos + 56, 0x404040);
        graphics.drawCenteredString(
                font,
                Component.translatable("gui.effecoria.climate_array.activate"),
                leftPos + 121,
                topPos + 56,
                0x404040);

        if (menu.cooldown() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.climate_array.cooldown", menu.cooldown() / 20),
                    leftPos + 8,
                    topPos + 18,
                    0xAA7733,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
