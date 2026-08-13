package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.PhiCartographyMenu;
import com.effecoria.client.ClientPhiSonarMap;
import com.effecoria.core.tower.PhiSonarService;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Dedicated Φ-cartography desk — large map, modes, zoom/pan. */
public final class PhiCartographyScreen extends AbstractContainerScreen<PhiCartographyMenu> {
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 240;
    private static final int MAP_SIZE = 200;

    private static final int BG_OUTER = 0xCC15202C;
    private static final int BG_INNER = 0xEE1E2E3C;
    private static final int LINE = 0xFF3A5A70;
    private static final int TITLE = 0xFFB8E0FF;
    private static final int MUTED = 0xFF8899AA;
    private static final int OK = 0xFF55CC88;
    private static final int WARN = 0xFFE0B040;
    private static final int BAD = 0xFFE05555;
    private static final int BG_LIST = 0xFF162430;

    private PhiSonarService.Mode scanMode = PhiSonarService.Mode.ACTIVE;
    private final PhiSonarMapPainter mapPainter = new PhiSonarMapPainter();
    private Button scanBtn;
    private Button modeBtn;

    public PhiCartographyScreen(PhiCartographyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.titleLabelX = 10;
        this.titleLabelY = 8;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }

    @Override
    protected void init() {
        super.init();
        modeBtn = addRenderableWidget(Button.builder(modeLabel(), b -> cycleMode())
                .bounds(leftPos + 10, topPos + PANEL_H - 28, 100, 18)
                .build());
        scanBtn = addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.tower_console.scan_btn"), b -> scan())
                .bounds(leftPos + PANEL_W - 72, topPos + PANEL_H - 28, 62, 18)
                .build());
        refreshButtons();
    }

    private Component modeLabel() {
        return Component.translatable("gui.effecoria.phi_sonar.mode." + scanMode.name().toLowerCase());
    }

    private void cycleMode() {
        scanMode = scanMode.next();
        modeBtn.setMessage(modeLabel());
    }

    private void scan() {
        PacketDistributor.sendToServer(
                new ModNetworking.PhiSonarRequestPayload(menu.blockEntity().getBlockPos(), scanMode.id()));
    }

    private void refreshButtons() {
        scanBtn.active = menu.linked() && menu.sonarPresent();
    }

    private int mapX() {
        return leftPos + (PANEL_W - MAP_SIZE) / 2;
    }

    private int mapY() {
        return topPos + 22;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mapPainter.mouseScrolled(mapX(), mapY(), MAP_SIZE, mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mapPainter.mouseClicked(mapX(), mapY(), MAP_SIZE, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mapPainter.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mapPainter.mouseDragged(MAP_SIZE, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 4, topPos - 4, leftPos + PANEL_W + 4, topPos + PANEL_H + 4, BG_OUTER);
        graphics.fill(leftPos, topPos, leftPos + PANEL_W, topPos + PANEL_H, BG_INNER);

        int mx = mapX();
        int my = mapY();

        if (!menu.linked()) {
            graphics.fill(mx - 2, my - 2, mx + MAP_SIZE + 2, my + MAP_SIZE + 2, LINE);
            graphics.fill(mx, my, mx + MAP_SIZE, my + MAP_SIZE, BG_LIST);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.no_link"),
                    leftPos + PANEL_W / 2,
                    my + MAP_SIZE / 2 - 4,
                    BAD);
            return;
        }

        if (!menu.sonarPresent()) {
            graphics.fill(mx - 2, my - 2, mx + MAP_SIZE + 2, my + MAP_SIZE + 2, LINE);
            graphics.fill(mx, my, mx + MAP_SIZE, my + MAP_SIZE, BG_LIST);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.sonar_missing"),
                    leftPos + PANEL_W / 2,
                    my + MAP_SIZE / 2 - 4,
                    WARN);
            return;
        }

        if (!ClientPhiSonarMap.hasMap()) {
            graphics.fill(mx - 2, my - 2, mx + MAP_SIZE + 2, my + MAP_SIZE + 2, LINE);
            graphics.fill(mx, my, mx + MAP_SIZE, my + MAP_SIZE, BG_LIST);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.sonar_empty"),
                    leftPos + PANEL_W / 2,
                    my + MAP_SIZE / 2 - 4,
                    MUTED);
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            menu.sonarReady()
                                    ? "gui.effecoria.tower_console.sonar_ready"
                                    : "gui.effecoria.tower_console.sonar_busy"),
                    leftPos + PANEL_W / 2,
                    my + MAP_SIZE / 2 + 8,
                    menu.sonarReady() ? OK : WARN);
            return;
        }

        mapPainter.draw(graphics, font, mx, my, MAP_SIZE, mouseX, mouseY, leftPos + 10, topPos + PANEL_H - 44);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TITLE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isHovering(PANEL_W - 72, PANEL_H - 28, 62, 18, mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(
                    "gui.effecoria.tower_console.scan_tip_mode",
                    Component.translatable("gui.effecoria.phi_sonar.mode." + scanMode.name().toLowerCase()),
                    scanMode.phiCost()));
            tip.add(Component.translatable("gui.effecoria.phi_sonar.zoom_tip"));
            graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else if (isHovering(10, PANEL_H - 28, 100, 18, mouseX, mouseY)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("gui.effecoria.phi_sonar.mode_tip." + scanMode.name().toLowerCase()),
                    mouseX,
                    mouseY);
        }
    }
}
