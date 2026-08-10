package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.ImprinterMenu;
import com.effecoria.block.PsiImprinterBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ψ-imprinter GUI — reuses mortar panel art; mode buttons select imprint kind. */
public final class ImprinterScreen extends AbstractContainerScreen<ImprinterMenu> {
    private static final int ARROW_X = 84;
    private static final int ARROW_Y = 35;

    public ImprinterScreen(ImprinterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(modeButton(leftPos + 98, topPos + 10, ImprinterMenu.BUTTON_CONSTRUCT, "gui.effecoria.imprinter.construct"));
        addRenderableWidget(modeButton(leftPos + 98, topPos + 28, ImprinterMenu.BUTTON_TELEGRAPH, "gui.effecoria.imprinter.telegraph"));
    }

    private Button modeButton(int x, int y, int id, String key) {
        return Button.builder(Component.translatable(key), b -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                    }
                })
                .bounds(x, y, 70, 16)
                .build();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.MORTAR, leftPos, topPos);
        float ratio = menu.progress() / (float) menu.maxProgress();
        AlchemyGui.progressArrow(graphics, leftPos, topPos, ARROW_X, ARROW_Y, ratio, 0xFFAA88FF);
        int mode = menu.mode();
        int by = mode == PsiImprinterBlockEntity.MODE_TELEGRAPH ? 28 : 10;
        graphics.fill(leftPos + 96, topPos + by, leftPos + 98, topPos + by + 16, 0xFFAA88FF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
