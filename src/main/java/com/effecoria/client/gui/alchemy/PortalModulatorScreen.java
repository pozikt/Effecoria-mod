package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PortalModulatorMenu;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Portal modulator computer — roomy layout: mode, destination, open/close above the inventory. */
public final class PortalModulatorScreen extends AbstractContainerScreen<PortalModulatorMenu> {
    private static final int PANEL_W = PortalModulatorMenu.PANEL_WIDTH;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private Button modeCoords;
    private Button modeBeacon;
    private Button beaconPrev;
    private Button beaconNext;
    private Button applyBtn;
    private Button openBtn;
    private Button closeBtn;
    private int beaconIndex;

    public PortalModulatorScreen(PortalModulatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PortalModulatorMenu.PANEL_HEIGHT;
        this.inventoryLabelX = PortalModulatorMenu.PLAYER_INV_X;
        this.inventoryLabelY = PortalModulatorMenu.PLAYER_INV_Y - 11;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();

        xBox = edit(28, 48, 48, String.valueOf(menu.targetX()));
        yBox = edit(90, 48, 48, String.valueOf(menu.targetY()));
        zBox = edit(152, 48, 48, String.valueOf(menu.targetZ()));
        addRenderableWidget(xBox);
        addRenderableWidget(yBox);
        addRenderableWidget(zBox);

        modeCoords = Button.builder(Component.translatable("gui.effecoria.portal_modulator.mode.coords"), b -> {
                    click(PortalModulatorMenu.BUTTON_MODE_COORDS);
                })
                .bounds(leftPos + 12, topPos + 26, 70, 16)
                .build();
        modeBeacon = Button.builder(Component.translatable("gui.effecoria.portal_modulator.mode.beacon"), b -> {
                    click(PortalModulatorMenu.BUTTON_MODE_BEACON);
                })
                .bounds(leftPos + 88, topPos + 26, 70, 16)
                .build();
        addRenderableWidget(modeCoords);
        addRenderableWidget(modeBeacon);

        beaconPrev = Button.builder(Component.literal("<"), b -> cycleBeacon(-1))
                .bounds(leftPos + 12, topPos + 48, 20, 18)
                .build();
        beaconNext = Button.builder(Component.literal(">"), b -> cycleBeacon(1))
                .bounds(leftPos + 188, topPos + 48, 20, 18)
                .build();
        addRenderableWidget(beaconPrev);
        addRenderableWidget(beaconNext);

        applyBtn = Button.builder(Component.translatable("gui.effecoria.portal_modulator.apply"), b -> apply())
                .bounds(leftPos + 164, topPos + 26, 44, 16)
                .build();
        addRenderableWidget(applyBtn);

        openBtn = Button.builder(Component.translatable("gui.effecoria.portal_modulator.open"), b -> {
                    apply();
                    click(PortalModulatorMenu.BUTTON_OPEN);
                })
                .bounds(leftPos + 28, topPos + 78, 78, 20)
                .build();
        closeBtn = Button.builder(Component.translatable("gui.effecoria.portal_modulator.close"), b -> {
                    click(PortalModulatorMenu.BUTTON_CLOSE);
                })
                .bounds(leftPos + 114, topPos + 78, 78, 20)
                .build();
        addRenderableWidget(openBtn);
        addRenderableWidget(closeBtn);

        List<String> names = menu.beaconNames();
        String sel = menu.selectedBeacon();
        beaconIndex = Math.max(0, names.indexOf(sel));
        syncModeWidgets();
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private EditBox edit(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, w, 16, Component.literal(""));
        box.setMaxLength(9);
        box.setValue(value);
        box.setFilter(s -> s.isEmpty() || s.matches("-?\\d*"));
        return box;
    }

    private void cycleBeacon(int delta) {
        List<String> names = menu.beaconNames();
        if (names.isEmpty()) {
            return;
        }
        beaconIndex = Math.floorMod(beaconIndex + delta, names.size());
    }

    private void syncModeWidgets() {
        boolean coords = menu.mode() == 0;
        if (xBox != null) {
            xBox.visible = coords;
            yBox.visible = coords;
            zBox.visible = coords;
        }
        if (beaconPrev != null) {
            beaconPrev.visible = !coords;
            beaconNext.visible = !coords;
        }
        if (modeCoords != null) {
            modeCoords.active = !coords;
            modeBeacon.active = coords;
        }
    }

    private void apply() {
        int mode = menu.mode();
        int x = parse(xBox.getValue(), menu.targetX());
        int y = parse(yBox.getValue(), menu.targetY());
        int z = parse(zBox.getValue(), menu.targetZ());
        String beacon = "";
        List<String> names = menu.beaconNames();
        if (!names.isEmpty()) {
            beacon = names.get(Math.floorMod(beaconIndex, names.size()));
        }
        PacketDistributor.sendToServer(new ModNetworking.PortalModulatorConfigPayload(
                menu.blockEntity().getBlockPos(), mode, x, y, z, beacon));
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void containerTick() {
        syncModeWidgets();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Composite: forge top strip + mid fill + inventory strip (taller computer).
        graphics.blit(
                AlchemyGui.FORGE_REACTOR,
                leftPos,
                topPos,
                0,
                0,
                PANEL_W,
                84,
                AlchemyGui.TEXTURE_SIZE,
                AlchemyGui.TEXTURE_SIZE);
        graphics.fill(leftPos + 3, topPos + 84, leftPos + PANEL_W - 3, topPos + PortalModulatorMenu.PLAYER_INV_Y - 1, 0xFFC6C6C6);
        graphics.fill(leftPos + 7, topPos + 18, leftPos + PANEL_W - 7, topPos + 104, 0xFFB0B8C0);
        graphics.blit(
                AlchemyGui.FORGE_REACTOR,
                leftPos,
                topPos + PortalModulatorMenu.PLAYER_INV_Y - 1,
                0,
                83,
                PANEL_W,
                AlchemyGui.HEIGHT - 83,
                AlchemyGui.TEXTURE_SIZE,
                AlchemyGui.TEXTURE_SIZE);

        boolean coords = menu.mode() == 0;
        if (coords) {
            graphics.drawString(font, "X", leftPos + 18, topPos + 52, 0x404040, false);
            graphics.drawString(font, "Y", leftPos + 80, topPos + 52, 0x404040, false);
            graphics.drawString(font, "Z", leftPos + 142, topPos + 52, 0x404040, false);
        } else {
            List<String> names = menu.beaconNames();
            String beaconLabel = names.isEmpty()
                    ? Component.translatable("gui.effecoria.portal_modulator.no_beacons").getString()
                    : names.get(Math.floorMod(beaconIndex, names.size()));
            graphics.drawCenteredString(font, beaconLabel, leftPos + imageWidth / 2, topPos + 53, 0x204060);
        }

        int statusY = topPos + 108;
        if (!menu.frameOk()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.portal_modulator.no_frame"),
                    leftPos + 12,
                    statusY,
                    0xAA3333,
                    false);
        } else if (menu.open()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.portal_modulator.open_status"),
                    leftPos + 12,
                    statusY,
                    0x33AA88,
                    false);
        } else if (!menu.cooled()) {
            graphics.drawString(
                    font, Component.translatable("gui.effecoria.portal_modulator.hot"), leftPos + 12, statusY, 0xCC7722, false);
        }

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.portal_modulator.power", menu.powerCenti()),
                leftPos + 130,
                statusY,
                menu.powerCenti() >= 100 ? 0x338833 : 0xAA7733,
                false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            minecraft.player.closeContainer();
            return true;
        }
        if ((xBox != null && xBox.isFocused())
                || (yBox != null && yBox.isFocused())
                || (zBox != null && zBox.isFocused())) {
            return xBox.keyPressed(keyCode, scanCode, modifiers)
                    || yBox.keyPressed(keyCode, scanCode, modifiers)
                    || zBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
