package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.SparkReactorMenu;
import com.effecoria.block.SparkReactorBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Spark Reactor GUI — fuel(56,35) / charge(116,35); START/STOP; fuel + power bars. */
public final class SparkReactorScreen extends AbstractContainerScreen<SparkReactorMenu> {
    public SparkReactorScreen(SparkReactorMenu menu, Inventory inv, Component title) {
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
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SparkReactorMenu.BUTTON_TOGGLE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);

        float fuel = menu.fuelRatio();
        int powerColor = powerColor(fuel);
        if (fuel > 0) {
            int w = Math.max(1, Math.round(70 * fuel));
            graphics.fill(leftPos + 53, topPos + 54, leftPos + 53 + w, topPos + 58, powerColor);
        }

        float charge = menu.chargeProgress() / (float) SparkReactorBlockEntity.CHARGE_TICKS;
        if (charge > 0) {
            int h = Math.max(1, Math.round(14 * charge));
            graphics.fill(leftPos + 117, topPos + 68 - h, leftPos + 130, topPos + 68, 0xFF55AAFF);
        }

        String toggleKey = menu.running() ? "gui.effecoria.spark_reactor.stop" : "gui.effecoria.spark_reactor.start";
        graphics.drawCenteredString(font, Component.translatable(toggleKey), leftPos + 97, topPos + 62, 0x404040);

        if (menu.overheatCooldown() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.spark_reactor.overheated"),
                    leftPos + 8,
                    topPos + 18,
                    0xAA3333,
                    false);
        } else if (menu.boostTicks() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.spark_reactor.boost"),
                    leftPos + 8,
                    topPos + 18,
                    0xDDAA22,
                    false);
        } else if (menu.powerFactor() > 0f && menu.powerFactor() < 0.75f) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.spark_reactor.throttled"),
                    leftPos + 8,
                    topPos + 18,
                    0xCC7722,
                    false);
        }
    }

    private static int powerColor(float fuelRatio) {
        if (fuelRatio <= 0.30f) {
            return 0xFF3388FF;
        }
        if (fuelRatio <= 0.70f) {
            return 0xFFDDBB33;
        }
        return 0xFFFF5533;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(56, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.spark_reactor.fuel", menu.fuelTicks()),
                    mouseX,
                    mouseY);
        } else if (isHovering(116, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.spark_reactor.charge"), mouseX, mouseY);
        } else if (isHovering(70, 58, 54, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.running()
                                    ? "gui.effecoria.spark_reactor.stop"
                                    : "gui.effecoria.spark_reactor.start"),
                    mouseX,
                    mouseY);
        }
    }
}
