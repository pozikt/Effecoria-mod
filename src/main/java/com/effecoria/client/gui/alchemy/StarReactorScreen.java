package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.StarReactorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Star Reactor — tower-console chrome. */
public final class StarReactorScreen extends AbstractContainerScreen<StarReactorMenu> {
    public StarReactorScreen(StarReactorMenu menu, Inventory inv, Component title) {
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
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StarReactorMenu.BUTTON_TOGGLE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TowerChrome.drawReactorShell(graphics, leftPos, topPos);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 80, 35, false);

        float fuel = Math.min(1f, menu.fuelTicks() / 24000f);
        TowerChrome.drawGauge(graphics, leftPos, topPos, 53, 54, 70, 4, fuel, TowerChrome.WARN);
        float omega = Math.min(1f, menu.omegaCentis() / 10000f);
        TowerChrome.drawGauge(graphics, leftPos, topPos, 53, 49, 70, 3, omega, TowerChrome.OMEGA);

        boolean running = menu.running();
        TowerChrome.drawChip(graphics, leftPos, topPos, 61, 58, 54, 16, running, !menu.cooled() && running);
        String toggleKey = running ? "gui.effecoria.star_reactor.stop" : "gui.effecoria.star_reactor.start";
        graphics.drawCenteredString(
                font, Component.translatable(toggleKey), leftPos + 88, topPos + 62, TowerChrome.LABEL);

        if (!menu.formed()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.star_reactor.not_formed"), leftPos, topPos, TowerChrome.BAD);
        } else if (menu.running() && !menu.cooled()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.star_reactor.hot"), leftPos, topPos, TowerChrome.WARN);
        } else if (menu.running()) {
            TowerChrome.drawStatus(
                    graphics,
                    font,
                    Component.translatable("gui.effecoria.star_reactor.running", String.format("%.1f", menu.powerFactor())),
                    leftPos,
                    topPos,
                    TowerChrome.OK);
        } else if (menu.formed()) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.star_reactor.formed"), leftPos, topPos, TowerChrome.IDLE);
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
        if (isHovering(80, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font, Component.translatable("gui.effecoria.star_reactor.fuel", menu.fuelTicks()), mouseX, mouseY);
        } else if (isHovering(61, 58, 54, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.running()
                                    ? "gui.effecoria.star_reactor.stop"
                                    : "gui.effecoria.star_reactor.start"),
                    mouseX,
                    mouseY);
        } else if (isHovering(53, 49, 70, 9, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.effecoria.star_reactor.omega", menu.omegaCentis() / 100, menu.fuelTicks()),
                    mouseX,
                    mouseY);
        }
    }
}
