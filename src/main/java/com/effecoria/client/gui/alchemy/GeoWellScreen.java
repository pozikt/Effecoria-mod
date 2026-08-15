package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.GeoWellMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Geo Well — tower-console chrome. */
public final class GeoWellScreen extends AbstractContainerScreen<GeoWellMenu> {
    public GeoWellScreen(GeoWellMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(TowerChrome.invisible(leftPos + 61, topPos + 58, 54, 16, b -> toggle()));
    }

    private void toggle() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GeoWellMenu.BUTTON_TOGGLE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TowerChrome.drawReactorShell(graphics, leftPos, topPos);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 56, 35, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 116, 35, true);

        float fuel = Math.min(1f, menu.fuelTicks() / 2400f);
        TowerChrome.drawGauge(graphics, leftPos, topPos, 53, 54, 70, 4, fuel, 0xFF33CCAA);
        float omega = Math.min(1f, menu.omegaCentis() / 10000f);
        TowerChrome.drawGauge(graphics, leftPos, topPos, 53, 49, 70, 3, omega, TowerChrome.OMEGA);

        boolean running = menu.running();
        TowerChrome.drawChip(graphics, leftPos, topPos, 61, 58, 54, 16, running, false);
        String toggleKey = running ? "gui.effecoria.geo_well.stop" : "gui.effecoria.geo_well.start";
        graphics.drawCenteredString(
                font, Component.translatable(toggleKey), leftPos + 88, topPos + 62, TowerChrome.LABEL);

        if (!menu.formed()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.geo_well.not_formed"), leftPos, topPos, TowerChrome.BAD);
        } else if (menu.running() && !menu.cooled()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.geo_well.hot"), leftPos, topPos, TowerChrome.WARN);
        } else if (menu.running()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.geo_well.running"), leftPos, topPos, TowerChrome.OK);
        } else if (menu.formed()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.geo_well.formed"), leftPos, topPos, TowerChrome.IDLE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TowerChrome.TITLE & 0xFFFFFF, false);
        graphics.drawString(
                font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TowerChrome.MUTED & 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(56, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.geo_well.fuel"), mouseX, mouseY);
        } else if (isHovering(116, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.geo_well.output"), mouseX, mouseY);
        } else if (isHovering(61, 58, 54, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.running() ? "gui.effecoria.geo_well.stop" : "gui.effecoria.geo_well.start"),
                    mouseX,
                    mouseY);
        } else if (isHovering(53, 49, 70, 9, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.effecoria.geo_well.omega", menu.omegaCentis() / 100, menu.fuelTicks()),
                    mouseX,
                    mouseY);
        }
    }
}
