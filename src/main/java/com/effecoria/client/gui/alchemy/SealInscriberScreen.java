package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.SealInscriberMenu;
import com.effecoria.core.artifact.AssembledGearData;
import com.effecoria.core.artifact.ItemSealCatalog;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Seal inscriber — stonecutter layout, clickable known-seal grid, level and apply/strip. */
public final class SealInscriberScreen extends AbstractContainerScreen<SealInscriberMenu> {
    private static final int GRID_CELLS = StonecutterRecipeGrid.COLS * StonecutterRecipeGrid.ROWS;

    public SealInscriberScreen(SealInscriberMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = ArtifactStationGui.WIDTH;
        this.imageHeight = ArtifactStationGui.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_PREV_SEAL))
                .bounds(
                        leftPos + ArtifactStationGui.SCROLL_LEFT_X,
                        topPos + ArtifactStationGui.SCROLL_Y,
                        ArtifactStationGui.SCROLL_W,
                        ArtifactStationGui.SCROLL_H)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_NEXT_SEAL))
                .bounds(
                        leftPos + ArtifactStationGui.SCROLL_RIGHT_X,
                        topPos + ArtifactStationGui.SCROLL_Y,
                        ArtifactStationGui.SCROLL_W,
                        ArtifactStationGui.SCROLL_H)
                .build());
        addRenderableWidget(Button.builder(Component.literal("-"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_LEVEL_DOWN))
                .bounds(leftPos + 61, topPos + 56, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_LEVEL_UP))
                .bounds(leftPos + 95, topPos + 56, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_inscriber.apply"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_APPLY))
                .bounds(leftPos + 44, topPos + 74, 44, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_inscriber.strip"), b ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SealInscriberMenu.BUTTON_STRIP))
                .bounds(leftPos + 92, topPos + 74, 44, 16)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ArtifactStationGui.blitStonecutter(graphics, leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSealGrid(graphics, mouseX, mouseY);
        renderSealSummary(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            List<ResourceLocation> known = knownSeals();
            int offset = gridOffset(known.size());
            int count = Math.min(GRID_CELLS, Math.max(0, known.size() - offset));
            for (int i = 0; i < count; i++) {
                if (StonecutterRecipeGrid.hit(leftPos, topPos, i, mouseX, mouseY)) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId, SealInscriberMenu.SELECT_INDEX_BASE + offset + i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderSealGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ResourceLocation> known = knownSeals();
        if (known.isEmpty()) {
            return;
        }
        int offset = gridOffset(known.size());
        int selected = Math.floorMod(menu.sealIndex(), known.size());
        int highlight = selected - offset;
        if (highlight >= 0 && highlight < GRID_CELLS) {
            StonecutterRecipeGrid.blitSelected(graphics, leftPos, topPos, highlight);
        }
        ItemStack target = menu.getSlot(0).getItem();
        int count = Math.min(GRID_CELLS, known.size() - offset);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = known.get(offset + i);
            int x = StonecutterRecipeGrid.cellX(leftPos, i);
            int y = StonecutterRecipeGrid.cellY(topPos, i);
            ItemStack icon = new ItemStack(Items.ENCHANTED_BOOK);
            graphics.renderItem(icon, x, y);
            int onItem = sealLevelOnTarget(target, id);
            if (onItem > 0) {
                graphics.renderItemDecorations(font, icon, x, y, String.valueOf(onItem));
            } else {
                graphics.renderItemDecorations(font, icon, x, y);
            }
            if (StonecutterRecipeGrid.hit(leftPos, topPos, i, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font,
                        Component.translatable("item_seal.effecoria." + id.getPath()),
                        mouseX,
                        mouseY);
            }
        }
    }

    private void renderSealSummary(GuiGraphics graphics) {
        List<ResourceLocation> known = knownSeals();
        if (known.isEmpty()) {
            graphics.drawCenteredString(
                    font, Component.translatable("gui.effecoria.seal_inscriber.no_seals"), leftPos + imageWidth / 2, topPos + 62, 0x404040);
            return;
        }
        ResourceLocation id = known.get(Math.floorMod(menu.sealIndex(), known.size()));
        String name = Component.translatable("item_seal.effecoria." + id.getPath()).getString();
        graphics.drawCenteredString(font, name + "  Lv " + menu.sealLevel(), leftPos + imageWidth / 2, topPos + 62, 0x404040);
        ItemStack target = menu.getSlot(0).getItem();
        if (!target.isEmpty()) {
            int used = AssembledGearData.seals(target).size();
            int cap = AssembledGearData.sealCapacity(target);
            Component slots = Component.translatable("gui.effecoria.seal_inscriber.slots", used, cap);
            graphics.drawCenteredString(font, slots, leftPos + imageWidth / 2, topPos + 50, 0x606060);
        }
    }

    private List<ResourceLocation> knownSeals() {
        List<ResourceLocation> known = new ArrayList<>();
        if (minecraft != null && minecraft.player != null) {
            known.addAll(ItemSealCatalog.knownOrdered(PsiHelper.get(minecraft.player).knownItemSeals()));
        }
        if (known.isEmpty()) {
            known.addAll(ItemSealCatalog.knownOrdered(ItemSealCatalog.starterIds()));
        }
        return known;
    }

    private int gridOffset(int knownCount) {
        if (knownCount <= GRID_CELLS) {
            return 0;
        }
        int idx = Math.floorMod(menu.sealIndex(), knownCount);
        return (idx / GRID_CELLS) * GRID_CELLS;
    }

    private static int sealLevelOnTarget(ItemStack target, ResourceLocation sealId) {
        if (target.isEmpty()) {
            return 0;
        }
        return AssembledGearData.sealLevel(target, sealId);
    }
}
