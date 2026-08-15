package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiArtilleryMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Manual yaw/pitch siege artillery controls. */
public final class PhiArtilleryScreen extends AbstractContainerScreen<PhiArtilleryMenu> {
    public PhiArtilleryScreen(PhiArtilleryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addBtn(8, 34, 24, "«", PhiArtilleryMenu.BUTTON_YAW_LEFT);
        addBtn(56, 34, 24, "»", PhiArtilleryMenu.BUTTON_YAW_RIGHT);
        addBtn(88, 28, 24, "↑", PhiArtilleryMenu.BUTTON_PITCH_UP);
        addBtn(88, 44, 24, "↓", PhiArtilleryMenu.BUTTON_PITCH_DOWN);
        addBtn(120, 28, 48, "Fire", PhiArtilleryMenu.BUTTON_FIRE);
        addBtn(120, 44, 48, "Hold", PhiArtilleryMenu.BUTTON_HOLD);
    }

    private void addBtn(int x, int y, int w, String label, int id) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> click(id))
                .bounds(leftPos + x, topPos + y, w, 14)
                .build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.PHI_TURRET, leftPos, topPos);
        int heat = menu.heat();
        if (heat > 0) {
            int w = Math.max(1, Math.min(70, Math.round(70 * (heat / 100f))));
            graphics.fill(leftPos + 53, topPos + 62, leftPos + 53 + w, topPos + 66, 0xFFE06020);
        }
        if (menu.firing()) {
            graphics.fill(leftPos + 8, topPos + 62, leftPos + 48, topPos + 66, 0x8846BEE6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        int color = menu.powerFactor() >= 2.5f ? 0x2E8B57 : 0xAA5544;
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.phi_artillery.status",
                        menu.yaw(),
                        menu.pitch(),
                        String.format("%.1f", menu.powerFactor())),
                8,
                18,
                color,
                false);
        if (!menu.formed()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.phi_artillery.need_lens"), 8, 70, 0xAA5544, false);
        } else if (menu.hold()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.phi_artillery.holding"), 8, 70, 0xCC7722, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
