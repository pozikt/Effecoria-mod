package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.FormSelectMenu;
import com.effecoria.core.artifact.ArtifactCatalog;
import com.effecoria.core.artifact.ShaftFormDefinition;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class FormSelectScreen extends AbstractContainerScreen<FormSelectMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");

    public FormSelectScreen(FormSelectMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FormSelectMenu.BUTTON_PREV))
                .bounds(leftPos + 20, topPos + 14, 20, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FormSelectMenu.BUTTON_NEXT))
                .bounds(leftPos + 136, topPos + 14, 20, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int max = menu.maxProgress();
        int progress = menu.progress();
        int w = max <= 0 ? 0 : progress * 22 / max;
        graphics.fill(leftPos + 79, topPos + 35, leftPos + 79 + w, topPos + 51, 0xFF55AAFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, formLabel(), leftPos + imageWidth / 2, topPos + 18, 0x404040);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private String formLabel() {
        int idx = menu.formIndex();
        if (menu.mode() == FormSelectMenu.Mode.LATHE) {
            var forms = ArtifactCatalog.shaftForms();
            if (forms.isEmpty()) {
                return "?";
            }
            ShaftFormDefinition form = forms.get(Math.floorMod(idx, forms.size()));
            return Component.translatable("gui.effecoria.shaft_form." + form.id().getPath()).getString()
                    + String.format(" · %.1fm", form.lengthMeters());
        }
        var cuts = ArtifactCatalog.focusCuts();
        if (cuts.isEmpty()) {
            return "?";
        }
        return cuts.get(Math.floorMod(idx, cuts.size())).id().getPath();
    }
}
