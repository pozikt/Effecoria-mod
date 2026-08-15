package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.ForgeReactorMenu;
import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.core.alchemy.ForgeRecipes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Forge Reactor — split bay + controls, tower-console chrome. */
public final class ForgeReactorScreen extends AbstractContainerScreen<ForgeReactorMenu> {
    private static final int PANEL_W = ForgeReactorMenu.PANEL_WIDTH;

    public ForgeReactorScreen(ForgeReactorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.inventoryLabelX = ForgeReactorMenu.PLAYER_INV_X;
        this.titleLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(TowerChrome.invisible(leftPos + 120, topPos + 54, 86, 9, b -> click(ForgeReactorMenu.BUTTON_MODE)));
        addRenderableWidget(TowerChrome.invisible(leftPos + 120, topPos + 64, 86, 9, b -> click(ForgeReactorMenu.BUTTON_TOGGLE)));
        addRenderableWidget(TowerChrome.invisible(leftPos + 120, topPos + 74, 86, 9, b -> click(ForgeReactorMenu.BUTTON_SCRAM)));
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        TowerChrome.drawWideReactorShell(graphics, leftPos, topPos, PANEL_W);
        TowerChrome.drawPlayerInvSlots(
                graphics, leftPos, topPos, ForgeReactorMenu.PLAYER_INV_X, ForgeReactorMenu.PLAYER_INV_Y);
        TowerChrome.drawDivider(graphics, leftPos, topPos, 110, 18, 78);

        TowerChrome.drawSlot(graphics, leftPos, topPos, 17, 23, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 37, 23, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 65, 23, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 21, 53, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 41, 53, false);
        TowerChrome.drawSlot(graphics, leftPos, topPos, 83, 53, true);

        float prog = menu.progressRatio();
        TowerChrome.drawGauge(graphics, leftPos, topPos, 60, 57, 18, 6, prog, TowerChrome.WARN);

        TowerChrome.drawGauge(graphics, leftPos, topPos, 121, 25, 84, 4, menu.powerPercent() / 100f, TowerChrome.WARN);
        TowerChrome.drawGauge(
                graphics,
                leftPos,
                topPos,
                121,
                35,
                84,
                4,
                menu.temperature() / (float) ForgeReactorBlockEntity.MAX_TEMP,
                TowerChrome.HEAT);
        int omega = menu.omegaPercent();
        TowerChrome.drawGauge(
                graphics, leftPos, topPos, 121, 45, 84, 4, omega / 100f, omega >= 25 ? TowerChrome.OMEGA : 0xFF6622AA);

        TowerChrome.drawChip(graphics, leftPos, topPos, 120, 54, 86, 9, false, false);
        TowerChrome.drawChip(graphics, leftPos, topPos, 120, 64, 86, 9, menu.running(), false);
        TowerChrome.drawChip(graphics, leftPos, topPos, 120, 74, 86, 9, false, true);

        graphics.drawCenteredString(font, modeLabel(menu.mode()), leftPos + 163, topPos + 55, TowerChrome.LABEL);
        String toggle = menu.running() ? "gui.effecoria.forge_reactor.stop" : "gui.effecoria.forge_reactor.start";
        graphics.drawCenteredString(font, Component.translatable(toggle), leftPos + 163, topPos + 65, TowerChrome.LABEL);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.forge_reactor.scram"), leftPos + 163, topPos + 75, 0xFFE0A0A0);

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.forge_reactor.bay"),
                leftPos + 10,
                topPos + 6,
                TowerChrome.TITLE & 0xFFFFFF,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.forge_reactor.controls"),
                leftPos + 116,
                topPos + 6,
                TowerChrome.TITLE & 0xFFFFFF,
                false);

        Component status = null;
        int statusColor = TowerChrome.BAD;
        if (!menu.formed()) {
            status = Component.translatable("gui.effecoria.forge_reactor.not_formed");
        } else if (!menu.cooled() && menu.running()) {
            status = Component.translatable("gui.effecoria.forge_reactor.hot");
            statusColor = TowerChrome.WARN;
        } else if (menu.omegaPercent() >= 10) {
            status = Component.translatable("gui.effecoria.forge_reactor.omega_warn");
            statusColor = TowerChrome.OMEGA;
        }
        if (status != null) {
            graphics.drawString(font, status, leftPos + 10, topPos + 70, statusColor & 0xFFFFFF, false);
        }
    }

    private static Component modeLabel(ForgeRecipes.Mode mode) {
        return Component.translatable("gui.effecoria.forge_reactor.mode." + mode.name().toLowerCase());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TowerChrome.MUTED & 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(121, 24, 84, 5, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font, Component.translatable("gui.effecoria.forge_reactor.power", menu.powerPercent()), mouseX, mouseY);
        } else if (isHovering(121, 34, 84, 5, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.effecoria.forge_reactor.temp", menu.temperature(), ForgeReactorBlockEntity.MAX_TEMP),
                    mouseX,
                    mouseY);
        } else if (isHovering(121, 44, 84, 5, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font, Component.translatable("gui.effecoria.forge_reactor.omega", menu.omegaPercent()), mouseX, mouseY);
        } else if (isHovering(17, 23, 16, 16, mouseX, mouseY) || isHovering(37, 23, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.forge_reactor.fuel"), mouseX, mouseY);
        } else if (isHovering(65, 23, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.forge_reactor.catalyst"), mouseX, mouseY);
        }
    }
}
