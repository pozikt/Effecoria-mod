package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.TowerConsoleMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Mage Tower control console — status + dome/body commands. */
public final class TowerConsoleScreen extends AbstractContainerScreen<TowerConsoleMenu> {
    public TowerConsoleScreen(TowerConsoleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.empty(), b -> dome())
                .bounds(leftPos + 28, topPos + 58, 54, 16)
                .build());
        addRenderableWidget(Button.builder(Component.empty(), b -> body())
                .bounds(leftPos + 94, topPos + 58, 54, 16)
                .build());
    }

    private void dome() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerConsoleMenu.BUTTON_DOME);
        }
    }

    private void body() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerConsoleMenu.BUTTON_BODY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);

        int x = leftPos + 8;
        int y = topPos + 18;
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.tower_console.integrity", menu.integrity()),
                x,
                y,
                0x404040,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.tower_console.omega", menu.omega()),
                x,
                y + 10,
                0x404040,
                false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.dome",
                        menu.domePowered() ? 1 : 0,
                        menu.domeCombat() ? 1 : 0),
                x,
                y + 20,
                0x404040,
                false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.systems",
                        menu.amuletCharged() ? 1 : 0,
                        menu.airOnline() ? 1 : 0,
                        menu.waterOnline() ? 1 : 0,
                        menu.regenOnline() ? 1 : 0,
                        menu.bound() ? 1 : 0),
                x,
                y + 30,
                0x404040,
                false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.body", menu.bodyType().getSerializedName()),
                x,
                y + 40,
                0x404040,
                false);

        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.tower_console.dome_btn"), leftPos + 55, topPos + 62, 0x404040);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.tower_console.body_btn"), leftPos + 121, topPos + 62, 0x404040);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
