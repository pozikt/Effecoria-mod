package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.AssemblerMenu;
import com.effecoria.block.ArtifactAssemblerBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    public AssemblerScreen(AssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 8;
        int y = topPos + 8;
        addBtn(x, y, AssemblerMenu.BUTTON_STAFF, "gui.effecoria.assembler.staff");
        addBtn(x + 40, y, AssemblerMenu.BUTTON_RING, "gui.effecoria.assembler.ring");
        addBtn(x + 80, y, AssemblerMenu.BUTTON_AMULET, "gui.effecoria.assembler.amulet");
        addBtn(x + 120, y, AssemblerMenu.BUTTON_CHARM, "gui.effecoria.assembler.charm");
    }

    private void addBtn(int x, int y, int id, String key) {
        addRenderableWidget(Button.builder(Component.translatable(key), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                .bounds(x, y, 38, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int max = menu.maxProgress();
        int progress = menu.progress();
        int w = max <= 0 ? 0 : progress * 24 / max;
        graphics.fill(leftPos + 100, topPos + 35, leftPos + 100 + w, topPos + 51, 0xFF88CCFF);
        int tmpl = menu.template();
        graphics.fill(leftPos + 8 + tmpl * 40, topPos + 24, leftPos + 46 + tmpl * 40, topPos + 26, 0xFF2266AA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
