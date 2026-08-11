package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiTurretMenu;
import com.effecoria.block.PhiTurretBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PhiTurretScreen extends AbstractContainerScreen<PhiTurretMenu> {
    public PhiTurretScreen(PhiTurretMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.empty(), b -> arm())
                .bounds(leftPos + 62, topPos + 58, 52, 16)
                .build());
    }

    private void arm() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PhiTurretMenu.BUTTON_ARM);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.PHI_TURRET, leftPos, topPos);

        int power = menu.powerCenti();
        if (power > 0) {
            int w = Math.max(1, Math.round(70 * Math.min(1f, power / 250f)));
            graphics.fill(leftPos + 53, topPos + 20, leftPos + 53 + w, topPos + 24, 0xFF33CCFF);
        }
        int heat = menu.heat();
        if (heat > 0) {
            int w = Math.max(1, Math.round(70 * heat / (float) PhiTurretBlockEntity.MAX_HEAT));
            graphics.fill(leftPos + 53, topPos + 26, leftPos + 53 + w, topPos + 30, 0xFFFF5533);
        }

        String armKey = menu.armed() ? "gui.effecoria.phi_turret.disarm" : "gui.effecoria.phi_turret.arm";
        graphics.drawCenteredString(font, Component.translatable(armKey), leftPos + 88, topPos + 62, 0x404040);

        if (menu.powerCenti() <= 0) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.phi_turret.no_power"), leftPos + 8, topPos + 48, 0xAA3333, false);
        } else if (menu.cooldown() > 40) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.phi_turret.overheat"), leftPos + 8, topPos + 48, 0xCC7722, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(53, 20, 70, 4, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.phi_turret.power", menu.powerCenti() / 100f),
                    mouseX,
                    mouseY);
        } else if (isHovering(53, 26, 70, 4, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font, Component.translatable("gui.effecoria.phi_turret.heat", menu.heat()), mouseX, mouseY);
        } else if (menu.kind().needsAmmo() && isHovering(80, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.phi_turret.ammo"), mouseX, mouseY);
        }
    }
}
