package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiIncubatorMenu;
import com.effecoria.core.tower.TowerBodyType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated incubator panel: 3 ingredient wells, recipe text on the right. */
public final class PhiIncubatorScreen extends AbstractContainerScreen<PhiIncubatorMenu> {
    private static final int ARROW_X = 84;
    private static final int ARROW_Y = 35;
    private static final int INFO_X = 118;

    public PhiIncubatorScreen(PhiIncubatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.PHI_INCUBATOR, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ARROW_X, ARROW_Y, ratio, 0xFF44DDBB);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        TowerBodyType target = menu.targetBody();
        Component bodyName = Component.translatable("gui.effecoria.tower_console.body." + target.getSerializedName());
        int infoColor = menu.linked() && menu.hasPower() ? 0x2E8B6A : 0x6A5A4A;

        if (!menu.linked()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_incubator.unlinked"),
                    INFO_X,
                    16,
                    0xAA5544,
                    false);
            drawWrapped(
                    graphics,
                    Component.translatable("gui.effecoria.phi_incubator.unlinked_hint"),
                    INFO_X,
                    28,
                    50,
                    0x6A5A4A);
            return;
        }

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.phi_incubator.target", bodyName),
                INFO_X,
                16,
                infoColor,
                false);

        drawWrapped(
                graphics,
                Component.translatable("gui.effecoria.phi_incubator.needs." + target.getSerializedName()),
                INFO_X,
                28,
                50,
                0x404040);

        TowerBodyType ready = menu.readyBody();
        if (ready != null) {
            Component readyName =
                    Component.translatable("gui.effecoria.tower_console.body." + ready.getSerializedName());
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_incubator.ready", readyName),
                    INFO_X,
                    56,
                    0x2E8B6A,
                    false);
        } else if (target == TowerBodyType.BASIC) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_incubator.basic_hint"),
                    INFO_X,
                    56,
                    0x6A5A4A,
                    false);
        } else {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_incubator.waiting"),
                    INFO_X,
                    56,
                    0x6A5A4A,
                    false);
        }
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) {
            graphics.drawString(font, line, x, y, color, false);
            y += font.lineHeight;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
