package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.SparkReactorMenu;
import com.effecoria.block.SparkReactorBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Spark Reactor — tower-console chrome. */
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
        addRenderableWidget(TowerChrome.invisible(leftPos + 61, topPos + 58, 54, 16, b -> toggle()));
    }

    private void toggle() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SparkReactorMenu.BUTTON_TOGGLE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TowerChrome.drawReactorShell(graphics, leftPos, topPos);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 56, 35, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 116, 35, true);

        float fuel = menu.fuelRatio();
        TowerChrome.drawGauge(graphics, leftPos, topPos, 53, 54, 70, 4, fuel, powerColor(fuel));

        float charge = menu.chargeProgress() / (float) SparkReactorBlockEntity.CHARGE_TICKS;
        TowerChrome.drawVGauge(graphics, leftPos, topPos, 117, 54, 13, 14, charge, TowerChrome.ACCENT);

        boolean running = menu.running();
        TowerChrome.drawChip(graphics, leftPos, topPos, 61, 58, 54, 16, running, menu.overheatCooldown() > 0);
        String toggleKey = running ? "gui.effecoria.spark_reactor.stop" : "gui.effecoria.spark_reactor.start";
        graphics.drawCenteredString(
                font, Component.translatable(toggleKey), leftPos + 88, topPos + 62, TowerChrome.LABEL);

        if (menu.overheatCooldown() > 0) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.spark_reactor.overheated"), leftPos, topPos, TowerChrome.BAD);
        } else if (menu.boostTicks() > 0) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.spark_reactor.boost"), leftPos, topPos, TowerChrome.WARN);
        } else if (menu.powerFactor() > 0f && menu.powerFactor() < 0.75f) {
            TowerChrome.drawStatus(
                    graphics, font, Component.translatable("gui.effecoria.spark_reactor.throttled"), leftPos, topPos, TowerChrome.WARN);
        }
    }

    private static int powerColor(float fuelRatio) {
        if (fuelRatio <= 0.30f) {
            return TowerChrome.ACCENT;
        }
        if (fuelRatio <= 0.70f) {
            return TowerChrome.WARN;
        }
        return TowerChrome.HEAT;
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
            graphics.renderTooltip(
                    font, Component.translatable("gui.effecoria.spark_reactor.fuel", menu.fuelTicks()), mouseX, mouseY);
        } else if (isHovering(116, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.spark_reactor.charge"), mouseX, mouseY);
        } else if (isHovering(61, 58, 54, 16, mouseX, mouseY)) {
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
