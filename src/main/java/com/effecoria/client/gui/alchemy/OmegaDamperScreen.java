package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.OmegaDamperMenu;
import com.effecoria.block.OmegaDamperBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Dedicated Ω-damper panel: three rod wells + facility Ω readout. */
public final class OmegaDamperScreen extends AbstractContainerScreen<OmegaDamperMenu> {
    public OmegaDamperScreen(OmegaDamperMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.OMEGA_DAMPER, leftPos, topPos);
        if (menu.scrubbing()) {
            graphics.fill(leftPos + 132, topPos + 30, leftPos + 162, topPos + 56, 0x886628A0);
        }
        // Fill the three tip gauges from rod saturation (slots 0..2)
        int[] slotXs = {53, 80, 107};
        for (int i = 0; i < 3; i++) {
            var stack = menu.getSlot(i).getItem();
            if (!com.effecoria.content.OmegaRodItem.isOmegaRod(stack)) {
                continue;
            }
            int pct = com.effecoria.content.OmegaRodItem.saturationPercent(stack);
            int maxH = 10;
            int h = Math.max(0, Math.min(maxH, Math.round(maxH * pct / 100f)));
            if (h <= 0) {
                continue;
            }
            int x0 = leftPos + slotXs[i] + 6;
            int y1 = topPos + 30;
            int color = pct >= 75 ? 0xFFC44CFF : pct >= 50 ? 0xFF8844CC : 0xFF553388;
            graphics.fill(x0, y1 - h, x0 + 3, y1, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xC8B8E0, false);

        String statusKey = switch (menu.status()) {
            case OmegaDamperBlockEntity.STATUS_NEED_RODS -> "need_rods";
            case OmegaDamperBlockEntity.STATUS_SCRUBBING -> "scrubbing";
            case OmegaDamperBlockEntity.STATUS_SATURATED -> "saturated";
            default -> "idle";
        };
        int statusColor = switch (menu.status()) {
            case OmegaDamperBlockEntity.STATUS_SCRUBBING -> 0xB070E0;
            case OmegaDamperBlockEntity.STATUS_SATURATED -> 0xC44C66;
            case OmegaDamperBlockEntity.STATUS_NEED_RODS -> 0xAA8844;
            default -> 0x8877AA;
        };
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.omega_damper.status." + statusKey),
                8,
                18,
                statusColor,
                false);

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.omega_damper.meter.tower", menu.towerOmegaPercent()),
                130,
                34,
                0xA090C0,
                false);
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.omega_damper.meter.forge", menu.forgeOmegaPercent()),
                130,
                44,
                0xA090C0,
                false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
