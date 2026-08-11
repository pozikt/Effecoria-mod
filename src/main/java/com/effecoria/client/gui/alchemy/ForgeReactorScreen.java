package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.ForgeReactorMenu;
import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.core.alchemy.ForgeRecipes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Split Forge GUI — left forge bay, right reactor controls. */
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
        // Right control column
        addRenderableWidget(Button.builder(Component.empty(), b -> click(ForgeReactorMenu.BUTTON_MODE))
                .bounds(leftPos + 120, topPos + 54, 86, 9)
                .build());
        addRenderableWidget(Button.builder(Component.empty(), b -> click(ForgeReactorMenu.BUTTON_TOGGLE))
                .bounds(leftPos + 120, topPos + 64, 86, 9)
                .build());
        addRenderableWidget(Button.builder(Component.empty(), b -> click(ForgeReactorMenu.BUTTON_SCRAM))
                .bounds(leftPos + 120, topPos + 74, 86, 9)
                .build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.FORGE_REACTOR, leftPos, topPos, PANEL_W, AlchemyGui.HEIGHT);

        // Left progress arrow fill
        float prog = menu.progressRatio();
        if (prog > 0) {
            int pw = Math.max(1, Math.round(18 * prog));
            graphics.fill(leftPos + 60, topPos + 57, leftPos + 60 + pw, topPos + 63, 0xFFE8A020);
        }

        // Right gauges
        int power = menu.powerPercent();
        if (power > 0) {
            int w = Math.max(1, Math.round(84 * power / 100f));
            graphics.fill(leftPos + 121, topPos + 25, leftPos + 121 + w, topPos + 28, 0xFFE8B020);
        }
        int temp = menu.temperature();
        int tw = Math.max(1, Math.round(84 * Math.min(1f, temp / (float) ForgeReactorBlockEntity.MAX_TEMP)));
        graphics.fill(leftPos + 121, topPos + 35, leftPos + 121 + tw, topPos + 38, 0xFFFF5533);
        int omega = menu.omegaPercent();
        if (omega > 0) {
            int ow = Math.max(1, Math.round(84 * omega / 100f));
            int color = omega >= 25 ? 0xFFAA22FF : 0xFF6622AA;
            graphics.fill(leftPos + 121, topPos + 45, leftPos + 121 + ow, topPos + 48, color);
        }

        // Button labels (right)
        graphics.drawCenteredString(font, modeLabel(menu.mode()), leftPos + 163, topPos + 55, 0x303030);
        String toggle = menu.running() ? "gui.effecoria.forge_reactor.stop" : "gui.effecoria.forge_reactor.start";
        graphics.drawCenteredString(font, Component.translatable(toggle), leftPos + 163, topPos + 65, 0x303030);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.forge_reactor.scram"), leftPos + 163, topPos + 75, 0x802020);

        // Section captions
        graphics.drawString(font, Component.translatable("gui.effecoria.forge_reactor.bay"), leftPos + 10, topPos + 6, 0x404040, false);
        graphics.drawString(
                font, Component.translatable("gui.effecoria.forge_reactor.controls"), leftPos + 116, topPos + 6, 0x404040, false);

        Component status = null;
        int statusColor = 0xAA3333;
        if (!menu.formed()) {
            status = Component.translatable("gui.effecoria.forge_reactor.not_formed");
        } else if (!menu.cooled() && menu.running()) {
            status = Component.translatable("gui.effecoria.forge_reactor.hot");
            statusColor = 0xCC7722;
        } else if (menu.omegaPercent() >= 10) {
            status = Component.translatable("gui.effecoria.forge_reactor.omega_warn");
            statusColor = 0xAA22AA;
        }
        if (status != null) {
            graphics.drawString(font, status, leftPos + 10, topPos + 70, statusColor, false);
        }
    }

    private static Component modeLabel(ForgeRecipes.Mode mode) {
        return Component.translatable("gui.effecoria.forge_reactor.mode." + mode.name().toLowerCase());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Title is replaced by section captions in renderBg — skip default title overlap.
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(121, 24, 84, 5, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.power", menu.powerPercent()),
                    mouseX,
                    mouseY);
        } else if (isHovering(121, 34, 84, 5, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.effecoria.forge_reactor.temp", menu.temperature(), ForgeReactorBlockEntity.MAX_TEMP),
                    mouseX,
                    mouseY);
        } else if (isHovering(121, 44, 84, 5, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.omega", menu.omegaPercent()),
                    mouseX,
                    mouseY);
        } else if (isHovering(17, 23, 16, 16, mouseX, mouseY) || isHovering(37, 23, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.forge_reactor.fuel"), mouseX, mouseY);
        } else if (isHovering(65, 23, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.forge_reactor.catalyst"), mouseX, mouseY);
        }
    }
}
