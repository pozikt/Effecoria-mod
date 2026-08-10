package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.HeartReactorMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Heart Reactor GUI — catalyst slot + START/STOP + formed/overheat status. */
public final class HeartReactorScreen extends AbstractContainerScreen<HeartReactorMenu> {
    public HeartReactorScreen(HeartReactorMenu menu, Inventory inv, Component title) {
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
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, HeartReactorMenu.BUTTON_TOGGLE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);

        if (menu.powerFactor() > 0f) {
            int w = Math.max(1, Math.round(70 * Math.min(1f, menu.powerFactor() / 2f)));
            graphics.fill(leftPos + 53, topPos + 54, leftPos + 53 + w, topPos + 58, 0xFF33DDFF);
        }

        String toggleKey = menu.running() ? "gui.effecoria.heart_reactor.stop" : "gui.effecoria.heart_reactor.start";
        graphics.drawCenteredString(font, Component.translatable(toggleKey), leftPos + 97, topPos + 62, 0x404040);

        if (!menu.formed()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.heart_reactor.not_formed"), leftPos + 8, topPos + 18, 0xAA3333, false);
        } else if (menu.overheatCooldown() > 0) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.heart_reactor.overheated"), leftPos + 8, topPos + 18, 0xAA3333, false);
        } else if (menu.boostTicks() > 0) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.heart_reactor.boost"), leftPos + 8, topPos + 18, 0xDDAA22, false);
        } else if (menu.running() && !menu.cooled()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.heart_reactor.hot"), leftPos + 8, topPos + 18, 0xCC7722, false);
        } else if (menu.formed() && menu.primed()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.heart_reactor.formed"), leftPos + 8, topPos + 18, 0x33AA88, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(80, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.heart_reactor.catalyst"), mouseX, mouseY);
        } else if (isHovering(70, 58, 54, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.running()
                                    ? "gui.effecoria.heart_reactor.stop"
                                    : "gui.effecoria.heart_reactor.start"),
                    mouseX,
                    mouseY);
        }
    }
}
