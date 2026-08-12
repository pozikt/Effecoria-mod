package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiBeaconMenu;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Name a Φ-beacon for portal targeting. */
public final class PhiBeaconScreen extends AbstractContainerScreen<PhiBeaconMenu> {
    private EditBox nameBox;

    public PhiBeaconScreen(PhiBeaconMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AlchemyGui.WIDTH;
        this.imageHeight = AlchemyGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        nameBox = new EditBox(font, leftPos + 28, topPos + 36, 120, 16, Component.literal("name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(menu.beaconName());
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.phi_beacon.save"), b -> save())
                .bounds(leftPos + 48, topPos + 58, 80, 18)
                .build());
    }

    private void save() {
        PacketDistributor.sendToServer(new ModNetworking.PhiBeaconRenamePayload(
                menu.blockEntity().getBlockPos(), nameBox.getValue()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AlchemyGui.blitPanel(graphics, AlchemyGui.SPARK_REACTOR, leftPos, topPos);
        graphics.drawString(
                font, Component.translatable("gui.effecoria.phi_beacon.name"), leftPos + 28, topPos + 22, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            if (keyCode == 256) {
                minecraft.player.closeContainer();
                return true;
            }
            return nameBox.keyPressed(keyCode, scanCode, modifiers)
                    || nameBox.canConsumeInput()
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
