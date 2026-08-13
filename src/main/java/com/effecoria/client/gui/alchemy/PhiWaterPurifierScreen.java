package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiWaterPurifierMenu;
import com.effecoria.block.PhiWaterPurifierBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Φ-water purifier — reuses burner panel chrome with three process slots. */
public final class PhiWaterPurifierScreen extends AbstractContainerScreen<PhiWaterPurifierMenu> {
    public PhiWaterPurifierScreen(PhiWaterPurifierMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.BURNER, leftPos, topPos);
        float ratio = menu.progress() / (float) PhiWaterPurifierBlockEntity.PROCESS_TICKS;
        AlchemyGui.progressArrow(graphics, leftPos, topPos, 74, 35, ratio, 0xFF55AAFF);
        if (!menu.powered()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.phi_water_purifier.no_power"),
                    leftPos + 8,
                    topPos + 6,
                    0xAA3333,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
