package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.SealInscriberMenu;
import com.effecoria.core.artifact.ItemSealCatalog;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class SealInscriberScreen extends AbstractContainerScreen<SealInscriberMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/anvil.png");

    public SealInscriberScreen(SealInscriberMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_PREV_SEAL))
                .bounds(leftPos + 20, topPos + 10, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_NEXT_SEAL))
                .bounds(leftPos + 136, topPos + 10, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.literal("-"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_LEVEL_DOWN))
                .bounds(leftPos + 40, topPos + 56, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_LEVEL_UP))
                .bounds(leftPos + 116, topPos + 56, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_inscriber.apply"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_APPLY))
                .bounds(leftPos + 40, topPos + 74, 50, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_inscriber.strip"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_STRIP))
                .bounds(leftPos + 96, topPos + 74, 50, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        List<ResourceLocation> known = new ArrayList<>();
        if (minecraft != null && minecraft.player != null) {
            known.addAll(PsiHelper.get(minecraft.player).knownItemSeals());
        }
        if (known.isEmpty()) {
            known.addAll(ItemSealCatalog.starterIds());
        }
        String sealName = "?";
        if (!known.isEmpty()) {
            ResourceLocation id = known.get(Math.floorMod(menu.sealIndex(), known.size()));
            sealName = Component.translatable("item_seal.effecoria." + id.getPath()).getString();
        }
        graphics.drawCenteredString(font, sealName + " " + menu.sealLevel(), leftPos + imageWidth / 2, topPos + 14, 0x404040);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
