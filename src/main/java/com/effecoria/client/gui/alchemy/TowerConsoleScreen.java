package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.TowerConsoleMenu;
import com.effecoria.client.ClientPhiSonarMap;
import com.effecoria.core.tower.PhiSonarService;
import com.effecoria.core.tower.TowerFacility;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Mage Tower console — no player inventory.
 * Tabs: Status (summary + devices) | Map (Φ-sonar heightmap).
 */
public final class TowerConsoleScreen extends AbstractContainerScreen<TowerConsoleMenu> {
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 210;
    private static final int LIST_X = 148;
    private static final int LIST_Y = 28;
    private static final int LIST_W = 160;
    private static final int LIST_H = 150;
    private static final int ROW_H = 18;
    private static final int MAP_SIZE = 180;

    private static final int BG_OUTER = 0xCC15202C;
    private static final int BG_INNER = 0xEE1E2E3C;
    private static final int BG_LIST = 0xFF162430;
    private static final int BG_ROW = 0xFF1C3040;
    private static final int LINE = 0xFF3A5A70;
    private static final int TITLE = 0xFFB8E0FF;
    private static final int LABEL = 0xFFD0D8E0;
    private static final int MUTED = 0xFF8899AA;
    private static final int OK = 0xFF55CC88;
    private static final int WARN = 0xFFE0B040;
    private static final int BAD = 0xFFE05555;
    private static final int IDLE = 0xFF778899;
    private static final int BAR_BG = 0xFF0A1218;
    private static final int TAB_ON = 0xFF2A4A5C;
    private static final int TAB_OFF = 0xFF152430;
    private static final int BLIP_LIVING = 0xFF55FF88;
    private static final int BLIP_UNDEAD = 0xFFC080FF;
    private static final int BLIP_PLAYER = 0xFFFFD060;
    private static final int CROSSHAIR = 0xFFFF6060;

    private enum Tab {
        STATUS,
        MAP
    }

    private Tab tab = Tab.STATUS;
    private int scroll;
    private Button domeBtn;
    private Button bodyBtn;
    private Button scrollUpBtn;
    private Button scrollDownBtn;
    private Button scanBtn;

    public TowerConsoleScreen(TowerConsoleMenu menu, Inventory inv, Component title) {
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
        domeBtn = addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.tower_console.dome_btn"), b -> dome())
                .bounds(leftPos + 10, topPos + PANEL_H - 28, 60, 18)
                .build());
        bodyBtn = addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.tower_console.body_btn"), b -> body())
                .bounds(leftPos + 74, topPos + PANEL_H - 28, 60, 18)
                .build());
        scrollUpBtn = addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(leftPos + PANEL_W - 22, topPos + LIST_Y, 14, 14)
                .build());
        scrollDownBtn = addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(leftPos + PANEL_W - 22, topPos + LIST_Y + LIST_H - 14, 14, 14)
                .build());
        scanBtn = addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.tower_console.scan_btn"), b -> scan())
                .bounds(leftPos + PANEL_W - 72, topPos + PANEL_H - 28, 62, 18)
                .build());
        applyTabVisibility();
    }

    private void applyTabVisibility() {
        boolean status = tab == Tab.STATUS;
        domeBtn.visible = status;
        bodyBtn.visible = status;
        scrollUpBtn.visible = status;
        scrollDownBtn.visible = status;
        scanBtn.visible = tab == Tab.MAP;
        scanBtn.active = menu.sonarPresent() && menu.linked();
    }

    private void setTab(Tab next) {
        tab = next;
        applyTabVisibility();
    }

    private void dome() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerConsoleMenu.BUTTON_DOME);
        }
    }

    private void body() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerConsoleMenu.BUTTON_BODY);
        }
    }

    private void scan() {
        PacketDistributor.sendToServer(new ModNetworking.PhiSonarRequestPayload(menu.blockEntity().getBlockPos()));
    }

    private int visibleRows() {
        return Math.max(1, LIST_H / ROW_H);
    }

    private void scroll(int delta) {
        List<TowerFacility.MonitorEntry> rows = menu.monitors();
        int max = Math.max(0, rows.size() - visibleRows());
        scroll = Math.max(0, Math.min(scroll + delta, max));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.STATUS
                && mouseX >= leftPos + LIST_X
                && mouseX <= leftPos + LIST_X + LIST_W
                && mouseY >= topPos + LIST_Y
                && mouseY <= topPos + LIST_Y + LIST_H) {
            scroll(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (hitTab(mouseX, mouseY, 0)) {
                setTab(Tab.STATUS);
                return true;
            }
            if (hitTab(mouseX, mouseY, 1)) {
                setTab(Tab.MAP);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean hitTab(double mouseX, double mouseY, int index) {
        int x0 = leftPos + 90 + index * 54;
        int y0 = topPos + 4;
        return mouseX >= x0 && mouseX < x0 + 50 && mouseY >= y0 && mouseY < y0 + 14;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (scanBtn != null) {
            scanBtn.active = menu.sonarPresent() && menu.linked();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos - 4, topPos - 4, leftPos + PANEL_W + 4, topPos + PANEL_H + 4, BG_OUTER);
        graphics.fill(leftPos, topPos, leftPos + PANEL_W, topPos + PANEL_H, BG_INNER);
        drawTabs(graphics);

        if (!menu.linked()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.no_link"),
                    leftPos + PANEL_W / 2,
                    topPos + PANEL_H / 2 - 4,
                    BAD);
            return;
        }

        if (tab == Tab.STATUS) {
            graphics.fill(leftPos + LIST_X - 6, topPos + 22, leftPos + LIST_X - 5, topPos + PANEL_H - 34, LINE);
            drawSummary(graphics);
            drawMonitorList(graphics, mouseX, mouseY);
        } else {
            drawMapTab(graphics, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics graphics) {
        drawTabChip(graphics, 0, "gui.effecoria.tower_console.tab.status", tab == Tab.STATUS);
        drawTabChip(graphics, 1, "gui.effecoria.tower_console.tab.map", tab == Tab.MAP);
    }

    private void drawTabChip(GuiGraphics graphics, int index, String key, boolean on) {
        int x0 = leftPos + 90 + index * 54;
        int y0 = topPos + 4;
        graphics.fill(x0, y0, x0 + 50, y0 + 14, on ? TAB_ON : TAB_OFF);
        graphics.fill(x0, y0 + 13, x0 + 50, y0 + 14, on ? TITLE : LINE);
        graphics.drawCenteredString(font, Component.translatable(key), x0 + 25, y0 + 3, on ? TITLE : MUTED);
    }

    private void drawMapTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int mapX = leftPos + (PANEL_W - MAP_SIZE) / 2;
        int mapY = topPos + 22;
        graphics.fill(mapX - 2, mapY - 2, mapX + MAP_SIZE + 2, mapY + MAP_SIZE + 2, LINE);
        graphics.fill(mapX, mapY, mapX + MAP_SIZE, mapY + MAP_SIZE, BG_LIST);

        if (!menu.sonarPresent()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.sonar_missing"),
                    leftPos + PANEL_W / 2,
                    mapY + MAP_SIZE / 2 - 4,
                    WARN);
            return;
        }

        if (!ClientPhiSonarMap.hasMap()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.sonar_empty"),
                    leftPos + PANEL_W / 2,
                    mapY + MAP_SIZE / 2 - 4,
                    MUTED);
            String ready = menu.sonarReady()
                    ? "gui.effecoria.tower_console.sonar_ready"
                    : "gui.effecoria.tower_console.sonar_busy";
            graphics.drawCenteredString(
                    font,
                    Component.translatable(ready),
                    leftPos + PANEL_W / 2,
                    mapY + MAP_SIZE / 2 + 8,
                    menu.sonarReady() ? OK : WARN);
            return;
        }

        byte[] heights = ClientPhiSonarMap.heights();
        int width = ClientPhiSonarMap.width();
        if (heights == null || width <= 0) {
            return;
        }

        int minH = 127;
        int maxH = -128;
        for (byte h : heights) {
            minH = Math.min(minH, h);
            maxH = Math.max(maxH, h);
        }
        int span = Math.max(1, maxH - minH);
        float cell = MAP_SIZE / (float) width;

        for (int iz = 0; iz < width; iz++) {
            for (int ix = 0; ix < width; ix++) {
                int idx = iz * width + ix;
                int h = heights[idx];
                float t = (h - minH) / (float) span;
                int color = heightColor(t);
                int x0 = mapX + Math.round(ix * cell);
                int y0 = mapY + Math.round(iz * cell);
                int x1 = mapX + Math.round((ix + 1) * cell);
                int y1 = mapY + Math.round((iz + 1) * cell);
                graphics.fill(x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1), color);
            }
        }

        // Center crosshair = sonar
        int cx = mapX + MAP_SIZE / 2;
        int cy = mapY + MAP_SIZE / 2;
        graphics.fill(cx - 2, cy, cx + 3, cy + 1, CROSSHAIR);
        graphics.fill(cx, cy - 2, cx + 1, cy + 3, CROSSHAIR);

        float scale = MAP_SIZE / (float) (ClientPhiSonarMap.radius() * 2 + 1);
        for (PhiSonarService.Blip blip : ClientPhiSonarMap.blips()) {
            int px = mapX + MAP_SIZE / 2 + Math.round(blip.relX() * scale);
            int pz = mapY + MAP_SIZE / 2 + Math.round(blip.relZ() * scale);
            int color = switch (blip.kind()) {
                case PhiSonarService.BLIP_PLAYER -> BLIP_PLAYER;
                case PhiSonarService.BLIP_UNDEAD -> BLIP_UNDEAD;
                default -> BLIP_LIVING;
            };
            graphics.fill(px - 1, pz - 1, px + 2, pz + 2, color);
        }

        // Cursor world coords
        if (mouseX >= mapX && mouseX < mapX + MAP_SIZE && mouseY >= mapY && mouseY < mapY + MAP_SIZE) {
            float u = (mouseX - mapX) / (float) MAP_SIZE;
            float v = (mouseY - mapY) / (float) MAP_SIZE;
            int relX = Math.round((u - 0.5f) * 2f * ClientPhiSonarMap.radius());
            int relZ = Math.round((v - 0.5f) * 2f * ClientPhiSonarMap.radius());
            int worldX = ClientPhiSonarMap.originX() + relX;
            int worldZ = ClientPhiSonarMap.originZ() + relZ;
            int gi = Math.min(width - 1, Math.max(0, Math.round(v * (width - 1))));
            int gj = Math.min(width - 1, Math.max(0, Math.round(u * (width - 1))));
            int surface = ClientPhiSonarMap.originY() + heights[gi * width + gj];
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.effecoria.tower_console.sonar_cursor", worldX, surface, worldZ),
                    leftPos + 10,
                    topPos + PANEL_H - 26,
                    LABEL,
                    false);
        } else {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "gui.effecoria.tower_console.sonar_origin",
                            ClientPhiSonarMap.originX(),
                            ClientPhiSonarMap.originY(),
                            ClientPhiSonarMap.originZ()),
                    leftPos + 10,
                    topPos + PANEL_H - 26,
                    MUTED,
                    false);
        }
    }

    private static int heightColor(float t) {
        // Low dark slate → mid teal → high pale sand
        float clamped = Math.max(0f, Math.min(1f, t));
        int r;
        int g;
        int b;
        if (clamped < 0.5f) {
            float u = clamped * 2f;
            r = (int) (18 + 30 * u);
            g = (int) (40 + 70 * u);
            b = (int) (48 + 60 * u);
        } else {
            float u = (clamped - 0.5f) * 2f;
            r = (int) (48 + 140 * u);
            g = (int) (110 + 80 * u);
            b = (int) (108 - 40 * u);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void drawSummary(GuiGraphics graphics) {
        int x = leftPos + 10;
        int y = topPos + 24;

        drawFlag(graphics, x, y, "gui.effecoria.tower_console.consecrated", menu.consecrated());
        y += 11;
        drawFlag(graphics, x, y, "gui.effecoria.tower_console.bound", menu.bound());
        y += 11;
        drawFlag(graphics, x, y, "gui.effecoria.tower_console.amulet", menu.amuletCharged());
        y += 11;
        drawFlag(graphics, x, y, "gui.effecoria.tower_console.phi_power", menu.phiPower());
        y += 13;

        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.reactor",
                        Component.translatable(
                                "gui.effecoria.tower_console.reactor."
                                        + menu.reactorClass().name().toLowerCase())),
                x,
                y,
                LABEL,
                false);
        y += 12;

        drawMeter(
                graphics,
                x,
                y,
                Component.translatable("gui.effecoria.tower_console.integrity_label"),
                menu.integrity(),
                integrityColor(menu.integrity()));
        y += 14;
        drawMeter(
                graphics,
                x,
                y,
                Component.translatable("gui.effecoria.tower_console.omega_label"),
                menu.omega(),
                omegaColor(menu.omega()));
        y += 14;

        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.cells", menu.presentBlocks(), menu.gluedCells()),
                x,
                y,
                MUTED,
                false);
        y += 10;
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.metrics",
                        String.format("%.2f", menu.verticality()),
                        String.format("%.2f", menu.scatter())),
                x,
                y,
                MUTED,
                false);
        y += 12;

        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.dome_state",
                        menu.domePowered()
                                ? Component.translatable("gui.effecoria.tower_console.status_on")
                                : Component.translatable("gui.effecoria.tower_console.status_off"),
                        menu.domeCombat()
                                ? Component.translatable("gui.effecoria.tower_console.combat")
                                : Component.translatable("gui.effecoria.tower_console.passive")),
                x,
                y,
                menu.domePowered() ? OK : MUTED,
                false);
        y += 10;
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.effecoria.tower_console.body_line",
                        Component.translatable(
                                "gui.effecoria.tower_console.body." + menu.bodyType().getSerializedName())),
                x,
                y,
                LABEL,
                false);
        y += 10;
        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.tower_console.revives", menu.reviveCount()),
                x,
                y,
                MUTED,
                false);
    }

    private void drawMonitorList(GuiGraphics graphics, int mouseX, int mouseY) {
        int lx = leftPos + LIST_X;
        int ly = topPos + LIST_Y;

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.tower_console.section.devices"),
                lx,
                topPos + 16,
                TITLE,
                false);

        graphics.fill(lx, ly, lx + LIST_W, ly + LIST_H, BG_LIST);

        List<TowerFacility.MonitorEntry> rows = menu.monitors();
        int maxScroll = Math.max(0, rows.size() - visibleRows());
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        if (rows.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.devices_empty"),
                    lx + LIST_W / 2,
                    ly + LIST_H / 2 - 4,
                    MUTED);
            return;
        }

        int end = Math.min(rows.size(), scroll + visibleRows());
        for (int i = scroll; i < end; i++) {
            TowerFacility.MonitorEntry entry = rows.get(i);
            int rowY = ly + (i - scroll) * ROW_H;
            boolean hover = mouseX >= lx
                    && mouseX < lx + LIST_W - 16
                    && mouseY >= rowY
                    && mouseY < rowY + ROW_H;
            graphics.fill(lx + 1, rowY + 1, lx + LIST_W - 16, rowY + ROW_H - 1, hover ? 0xFF243848 : BG_ROW);

            int sevColor = severityColor(entry.severity());
            graphics.fill(lx + 2, rowY + 3, lx + 5, rowY + ROW_H - 3, sevColor);

            Component name = Component.translatable("gui.effecoria.tower_console.device." + entry.kind());
            Component status = Component.translatable("gui.effecoria.tower_console.devstat." + entry.status());
            graphics.drawString(font, name, lx + 8, rowY + 5, LABEL, false);

            int statusX = lx + LIST_W - 20 - font.width(status);
            graphics.drawString(font, status, statusX, rowY + 5, sevColor, false);
        }

        if (rows.size() > visibleRows()) {
            int trackH = LIST_H - 32;
            int thumbH = Math.max(10, trackH * visibleRows() / rows.size());
            int thumbY = ly + 16 + (maxScroll == 0 ? 0 : (trackH - thumbH) * scroll / maxScroll);
            graphics.fill(leftPos + PANEL_W - 20, ly + 16, leftPos + PANEL_W - 16, ly + LIST_H - 16, BAR_BG);
            graphics.fill(leftPos + PANEL_W - 20, thumbY, leftPos + PANEL_W - 16, thumbY + thumbH, LINE);
        }
    }

    private void drawFlag(GuiGraphics graphics, int x, int y, String key, boolean ok) {
        graphics.drawString(font, Component.translatable(key), x, y, LABEL, false);
        Component state = ok
                ? Component.translatable("gui.effecoria.tower_console.status_ok")
                : Component.translatable("gui.effecoria.tower_console.status_bad");
        graphics.drawString(font, state, x + 78, y, ok ? OK : BAD, false);
    }

    private void drawMeter(GuiGraphics graphics, int x, int y, Component label, int percent, int fillColor) {
        graphics.drawString(font, label, x, y, LABEL, false);
        int pct = Math.max(0, Math.min(100, percent));
        int barX = x;
        int barY = y + 9;
        int barW = 126;
        graphics.fill(barX, barY, barX + barW, barY + 3, BAR_BG);
        int fill = Math.round(barW * (pct / 100f));
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + 3, fillColor);
        }
        graphics.drawString(font, pct + "%", x + 108, y, fillColor, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TITLE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (tab == Tab.STATUS && menu.linked()) {
            List<TowerFacility.MonitorEntry> rows = menu.monitors();
            int lx = leftPos + LIST_X;
            int ly = topPos + LIST_Y;
            int end = Math.min(rows.size(), scroll + visibleRows());
            for (int i = scroll; i < end; i++) {
                int rowY = ly + (i - scroll) * ROW_H;
                if (mouseX >= lx && mouseX < lx + LIST_W - 16 && mouseY >= rowY && mouseY < rowY + ROW_H) {
                    TowerFacility.MonitorEntry e = rows.get(i);
                    graphics.renderTooltip(
                            font,
                            Component.translatable(
                                    "gui.effecoria.tower_console.device_tip",
                                    e.x(),
                                    e.y(),
                                    e.z()),
                            mouseX,
                            mouseY);
                    break;
                }
            }

            if (isHovering(10, PANEL_H - 28, 60, 18, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font,
                        Component.translatable(
                                menu.domeCombat()
                                        ? "gui.effecoria.tower_console.dome_tip_off"
                                        : "gui.effecoria.tower_console.dome_tip_on"),
                        mouseX,
                        mouseY);
            } else if (isHovering(74, PANEL_H - 28, 60, 18, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font, Component.translatable("gui.effecoria.tower_console.body_tip"), mouseX, mouseY);
            }
        } else if (tab == Tab.MAP && isHovering(PANEL_W - 72, PANEL_H - 28, 62, 18, mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.effecoria.tower_console.scan_tip"));
            if (!menu.sonarPresent()) {
                tip.add(Component.translatable("gui.effecoria.tower_console.sonar_missing"));
            } else if (!menu.sonarReady()) {
                tip.add(Component.translatable("gui.effecoria.tower_console.sonar_busy"));
            }
            graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    private static int severityColor(int severity) {
        return switch (severity) {
            case TowerFacility.MonitorEntry.OK -> OK;
            case TowerFacility.MonitorEntry.WARN -> WARN;
            case TowerFacility.MonitorEntry.BAD -> BAD;
            default -> IDLE;
        };
    }

    private static int integrityColor(int pct) {
        if (pct >= 85) {
            return OK;
        }
        if (pct >= 60) {
            return WARN;
        }
        return BAD;
    }

    private static int omegaColor(int pct) {
        if (pct <= 25) {
            return OK;
        }
        if (pct <= 60) {
            return WARN;
        }
        return BAD;
    }
}
