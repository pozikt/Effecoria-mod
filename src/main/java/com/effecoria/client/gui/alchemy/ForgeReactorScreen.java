package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.ForgeReactorMenu;
import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.core.alchemy.ForgeRecipes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Forge Reactor GUI — fuels/catalyst/IO + mode / start / scram. */
public final class ForgeReactorScreen extends AbstractContainerScreen<ForgeReactorMenu> {
    public ForgeReactorScreen(ForgeReactorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.empty(), b -> click(ForgeReactorMenu.BUTTON_MODE))
                .bounds(leftPos + 8, topPos + 68, 48, 12)
                .build());
        addRenderableWidget(Button.builder(Component.empty(), b -> click(ForgeReactorMenu.BUTTON_TOGGLE))
                .bounds(leftPos + 60, topPos + 68, 48, 12)
                .build());
        addRenderableWidget(Button.builder(Component.empty(), b -> click(ForgeReactorMenu.BUTTON_SCRAM))
                .bounds(leftPos + 112, topPos + 68, 56, 12)
                .build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);

        int power = menu.powerPercent();
        if (power > 0) {
            int w = Math.max(1, Math.round(70 * power / 100f));
            graphics.fill(leftPos + 98, topPos + 18, leftPos + 98 + w, topPos + 22, 0xFFE8B020);
        }
        int temp = menu.temperature();
        int tw = Math.max(1, Math.round(70 * Math.min(1f, temp / (float) ForgeReactorBlockEntity.MAX_TEMP)));
        graphics.fill(leftPos + 98, topPos + 24, leftPos + 98 + tw, topPos + 28, 0xFFFF5533);
        int omega = menu.omegaPercent();
        if (omega > 0) {
            int ow = Math.max(1, Math.round(70 * omega / 100f));
            int color = omega >= 25 ? 0xFFAA22FF : 0xFF6622AA;
            graphics.fill(leftPos + 98, topPos + 30, leftPos + 98 + ow, topPos + 34, color);
        }
        float prog = menu.progressRatio();
        if (prog > 0) {
            int pw = Math.max(1, Math.round(36 * prog));
            graphics.fill(leftPos + 80, topPos + 56, leftPos + 80 + pw, topPos + 60, 0xFF55AAFF);
        }

        graphics.drawCenteredString(font, modeLabel(menu.mode()), leftPos + 32, topPos + 70, 0x404040);
        String toggle = menu.running() ? "gui.effecoria.forge_reactor.stop" : "gui.effecoria.forge_reactor.start";
        graphics.drawCenteredString(font, Component.translatable(toggle), leftPos + 84, topPos + 70, 0x404040);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.forge_reactor.scram"), leftPos + 140, topPos + 70, 0x802020);

        if (!menu.formed()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.not_formed"),
                    leftPos + 8,
                    topPos + 6,
                    0xAA3333,
                    false);
        } else if (!menu.cooled() && menu.running()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.hot"),
                    leftPos + 8,
                    topPos + 6,
                    0xCC7722,
                    false);
        } else if (menu.omegaPercent() >= 10) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.omega_warn"),
                    leftPos + 8,
                    topPos + 6,
                    0xAA22AA,
                    false);
        }
    }

    private static Component modeLabel(ForgeRecipes.Mode mode) {
        return Component.translatable("gui.effecoria.forge_reactor.mode." + mode.name().toLowerCase());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(98, 18, 70, 4, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.power", menu.powerPercent()),
                    mouseX,
                    mouseY);
        } else if (isHovering(98, 24, 70, 4, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.effecoria.forge_reactor.temp", menu.temperature(), ForgeReactorBlockEntity.MAX_TEMP),
                    mouseX,
                    mouseY);
        } else if (isHovering(98, 30, 70, 4, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.forge_reactor.omega", menu.omegaPercent()),
                    mouseX,
                    mouseY);
        }
    }
}
