package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiFabricatorMenu;
import com.effecoria.block.PhiFabricatorBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated Φ-fabricator panel: memory column, mats tray, progress arrow, output. */
public final class PhiFabricatorScreen extends AbstractContainerScreen<PhiFabricatorMenu> {
    public PhiFabricatorScreen(PhiFabricatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Φ"), b -> write())
                .bounds(leftPos + 16, topPos + 44, 30, 12)
                .build());
    }

    private void write() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PhiFabricatorMenu.BUTTON_WRITE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.PHI_FABRICATOR, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, 126, 38, ratio, 0xFF46BEE6);
        if (menu.hasPower()) {
            graphics.fill(leftPos + 153, topPos + 19, leftPos + 169, topPos + 23, 0xFF46BEE6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x1E4A5C, false);
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.fabricator.class", menu.fabricatorClass()),
                58,
                18,
                menu.hasPower() ? 0x2E6A8B : 0xAA5544,
                false);

        Component status = switch (menu.writeStatus()) {
            case PhiFabricatorBlockEntity.WRITE_NEED ->
                    Component.translatable("gui.effecoria.fabricator.status.need");
            case PhiFabricatorBlockEntity.WRITE_FAIL ->
                    Component.translatable("gui.effecoria.fabricator.status.fail");
            case PhiFabricatorBlockEntity.WRITE_OK ->
                    Component.translatable("gui.effecoria.fabricator.status.ok");
            case PhiFabricatorBlockEntity.WRITE_NO_POWER ->
                    Component.translatable("gui.effecoria.fabricator.status.no_power");
            default -> Component.translatable("gui.effecoria.fabricator.status.hint");
        };
        int statusColor = switch (menu.writeStatus()) {
            case PhiFabricatorBlockEntity.WRITE_OK -> 0x2E8B57;
            case PhiFabricatorBlockEntity.WRITE_FAIL,
                    PhiFabricatorBlockEntity.WRITE_NEED,
                    PhiFabricatorBlockEntity.WRITE_NO_POWER -> 0xAA5544;
            default -> 0x4A6A7A;
        };
        // Bottom of machine panel — short line; full text also goes to chat
        graphics.drawString(font, status, 54, 68, statusColor, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
