package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.AlembicMenu;
import com.effecoria.core.alchemy.HeatLevel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Alembic GUI — water(26,35), reagents @ x=62, output(116,35). */
public final class AlembicScreen extends AbstractContainerScreen<AlembicMenu> {
    private static final int ARROW_X = 84;
    private static final int ARROW_Y = 35;

    public AlembicScreen(AlembicMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.ALEMBIC, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ARROW_X, ARROW_Y, ratio, 0xFF55DDFF);
        HeatLevel heat = menu.heatLevel();
        float fill = heat.isPresent() ? heat.ordinal() / 3f : 0f;
        int color = switch (heat) {
            case LOW -> 0xFF55AAFF;
            case MEDIUM -> 0xFF88DDFF;
            case HIGH -> 0xFFFFAA44;
            case NONE -> 0xFF333333;
        };
        AlchemyGui.heatGauge(graphics, leftPos, topPos, 8, 18, fill, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(8, 16, 10, 48, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.alembic.heat." + menu.heatLevel().name().toLowerCase()),
                    mouseX,
                    mouseY);
        } else if (isHovering(26, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.alembic.water"), mouseX, mouseY);
        } else if (isHovering(62, 17, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.alembic.reagent1"), mouseX, mouseY);
        } else if (isHovering(116, 35, 16, 16, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.effecoria.alembic.output"), mouseX, mouseY);
        }
    }
}
