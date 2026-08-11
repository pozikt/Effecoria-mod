package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiCrusherMenu;
import com.effecoria.block.PhiCrusherBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Φ-crusher GUI — slots match {@link PhiCrusherMenu} / {@code textures/gui/phi_crusher.png}. */
public final class PhiCrusherScreen extends AbstractContainerScreen<PhiCrusherMenu> {
    private static final int ARROW_X = 74;
    private static final int ARROW_Y = 35;

    public PhiCrusherScreen(PhiCrusherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.empty(), b -> toggleMode())
                .bounds(leftPos + 62, topPos + 18, 52, 16)
                .build());
    }

    private void toggleMode() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PhiCrusherMenu.BUTTON_MODE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.PHI_CRUSHER, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ARROW_X, ARROW_Y, ratio, 0xFF55AAFF);

        float power = menu.powerCenti() / 100f;
        AlchemyGui.heatGauge(graphics, leftPos, topPos, 8, 22, power, 0xFF3388FF);

        String modeKey = menu.fineMode() ? "gui.effecoria.phi_crusher.fine" : "gui.effecoria.phi_crusher.coarse";
        graphics.drawCenteredString(font, Component.translatable(modeKey), leftPos + 88, topPos + 22, 0x404040);

        if (menu.cooldown() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_crusher.overheat"),
                    leftPos + 8,
                    topPos + 6,
                    0xAA3333,
                    false);
        } else if (menu.omega() >= PhiCrusherBlockEntity.OMEGA_LIMIT) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_crusher.omega"),
                    leftPos + 8,
                    topPos + 6,
                    0x9933AA,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(26, 53, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.phi_crusher.drive"), mouseX, mouseY);
        } else if (isHovering(44, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.phi_crusher.input"), mouseX, mouseY);
        } else if (isHovering(116, 17, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.phi_crusher.primary"), mouseX, mouseY);
        } else if (isHovering(134, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.phi_crusher.byproduct"), mouseX, mouseY);
        } else if (isHovering(116, 53, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.phi_crusher.waste"), mouseX, mouseY);
        } else if (isHovering(62, 18, 52, 16, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            menu.fineMode() ? "gui.effecoria.phi_crusher.fine" : "gui.effecoria.phi_crusher.coarse"),
                    mouseX,
                    mouseY);
        } else if (isHovering(8, 18, 12, 52, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.phi_crusher.power", menu.powerCenti()),
                    mouseX,
                    mouseY);
        } else if (isHovering(8, 6, 80, 10, mouseX, mouseY) && (menu.heat() > 0 || menu.omega() > 0)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.phi_crusher.meters", menu.heat(), menu.omega()),
                    mouseX,
                    mouseY);
        }
    }
}
