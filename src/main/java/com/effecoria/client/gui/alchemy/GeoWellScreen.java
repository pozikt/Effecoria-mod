package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.GeoWellMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Geo Well GUI — fuel + output slots, START/STOP, omega/fuel/status. */
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
        addRenderableWidget(Button.builder(Component.empty(), b -> toggle())
                .bounds(leftPos + 70, topPos + 58, 54, 16)
                .build());
    }

    private void toggle() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GeoWellMenu.BUTTON_TOGGLE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);

        int fuel = menu.fuelTicks();
        if (fuel > 0) {
            float ratio = Math.min(1f, fuel / 2400f);
            int w = Math.max(1, Math.round(70 * ratio));
            graphics.fill(leftPos + 53, topPos + 54, leftPos + 53 + w, topPos + 58, 0xFF33CCAA);
        }

        int omega = menu.omegaCentis();
        if (omega > 0) {
            int w = Math.max(1, Math.min(70, Math.round(70 * (omega / 10000f))));
            graphics.fill(leftPos + 53, topPos + 50, leftPos + 53 + w, topPos + 53, 0xFFAA33AA);
        }

        String toggleKey = menu.running() ? "gui.effecoria.geo_well.stop" : "gui.effecoria.geo_well.start";
        graphics.drawCenteredString(font, Component.translatable(toggleKey), leftPos + 97, topPos + 62, 0x404040);

        if (!menu.formed()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.geo_well.not_formed"), leftPos + 8, topPos + 18, 0xAA3333, false);
        } else if (menu.running() && !menu.cooled()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.geo_well.hot"), leftPos + 8, topPos + 18, 0xCC7722, false);
        } else if (menu.running()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.geo_well.running"), leftPos + 8, topPos + 18, 0x33AA88, false);
        } else if (menu.formed()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.geo_well.formed"), leftPos + 8, topPos + 18, 0x557788, false);
        }
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
        } else if (isHovering(70, 58, 54, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.running() ? "gui.effecoria.geo_well.stop" : "gui.effecoria.geo_well.start"),
                    mouseX,
                    mouseY);
        } else if (isHovering(53, 50, 70, 8, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.effecoria.geo_well.omega", menu.omegaCentis() / 100, menu.fuelTicks()),
                    mouseX,
                    mouseY);
        }
    }
}
