package com.effecoria.client.gui.alchemy;

import com.effecoria.alchemy.menu.TowerConsoleMenu;
import com.effecoria.client.ClientPhiSonarMap;
import com.effecoria.core.loci.LexLociCompiler;
import com.effecoria.core.loci.LociActuator;
import com.effecoria.core.loci.LociAddress;
import com.effecoria.core.loci.LociEvent;
import com.effecoria.core.tower.PhiSonarService;
import com.effecoria.core.tower.TowerFacility;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dedicated Mage Tower console — no player inventory.
 * Tabs: Status | Map | Edicts (Lex Loci Phoenix + symbol table).
 */
public final class TowerConsoleScreen extends AbstractContainerScreen<TowerConsoleMenu> {
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 210;
    private static final int LIST_X = 148;
    private static final int LIST_Y = 28;
    private static final int LIST_W = 160;
    private static final int LIST_H = 150;
    private static final int ROW_H = 18;
    private static final int MAP_SIZE = 168;
    private static final int EDICT_LIST_Y = 132;
    private static final int EDICT_LIST_H = 36;
    private static final int CHIP_H = 16;
    private static final int CHIP_PROGRAM = 0xFF3A4A5C;
    private static final int CHIP_SENSE = 0xFF1A4A5C;
    private static final int CHIP_TRIGGER = 0xFF4A3A20;
    private static final int CHIP_ADDR = 0xFF2A4050;
    private static final int CHIP_HOVER = 0xFF554488;
    private static final int WORD_BOX_Y = 86;
    private static final int WORD_BOX_H = 16;
    private static final int SUGGEST_MAX = 6;
    private static final int SUGGEST_ROW_H = 12;

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

    private enum Tab {
        STATUS,
        MAP,
        EDICTS
    }

    private Tab tab = Tab.STATUS;
    private int scroll;
    private int edictScroll;
    private PhiSonarService.Mode scanMode = PhiSonarService.Mode.ACTIVE;
    private final PhiSonarMapPainter mapPainter = new PhiSonarMapPainter();
    private Button domeBtn;
    private Button bodyBtn;
    private Button scrollUpBtn;
    private Button scrollDownBtn;
    private Button scanBtn;
    private Button modeBtn;
    private Button phoenixBtn;
    private Button lociApplyBtn;
    private Button lociResetBtn;
    private EditBox lociWordBox;
    private final List<String> lociDraft = new ArrayList<>();
    private boolean lociDirty;

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
        modeBtn = addRenderableWidget(Button.builder(modeLabel(), b -> cycleMode())
                .bounds(leftPos + 10, topPos + PANEL_H - 28, 88, 18)
                .build());
        scanBtn = addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.tower_console.scan_btn"), b -> scan())
                .bounds(leftPos + PANEL_W - 72, topPos + PANEL_H - 28, 62, 18)
                .build());
        phoenixBtn = addRenderableWidget(Button.builder(phoenixLabel(), b -> phoenix())
                .bounds(leftPos + 10, topPos + PANEL_H - 28, 80, 18)
                .build());
        lociApplyBtn = addRenderableWidget(
                Button.builder(Component.translatable("gui.effecoria.tower_console.edict.apply"), b -> applyLoci())
                        .bounds(leftPos + 94, topPos + PANEL_H - 28, 70, 18)
                        .build());
        lociResetBtn = addRenderableWidget(
                Button.builder(Component.translatable("gui.effecoria.tower_console.edict.reset"), b -> resetLoci())
                        .bounds(leftPos + 168, topPos + PANEL_H - 28, 70, 18)
                        .build());
        lociWordBox = new EditBox(
                font,
                leftPos + 10,
                topPos + WORD_BOX_Y,
                200,
                WORD_BOX_H,
                Component.translatable("gui.effecoria.tower_console.edict.word_hint"));
        lociWordBox.setMaxLength(48);
        lociWordBox.setBordered(true);
        lociWordBox.setHint(Component.translatable("gui.effecoria.tower_console.edict.word_hint"));
        addRenderableWidget(lociWordBox);
        if (!lociDirty) {
            lociDraft.clear();
            lociDraft.addAll(LexLociCompiler.effectiveTokens(menu.lociTokens()));
        }
        applyTabVisibility();
    }

    private Component phoenixLabel() {
        return Component.translatable(
                menu.phoenixEdictEnabled()
                        ? "gui.effecoria.tower_console.phoenix_on"
                        : "gui.effecoria.tower_console.phoenix_off");
    }

    private Component modeLabel() {
        return Component.translatable("gui.effecoria.phi_sonar.mode." + scanMode.name().toLowerCase());
    }

    private void cycleMode() {
        scanMode = scanMode.next();
        modeBtn.setMessage(modeLabel());
    }

    private void applyTabVisibility() {
        boolean status = tab == Tab.STATUS;
        domeBtn.visible = status;
        bodyBtn.visible = status;
        scrollUpBtn.visible = status;
        scrollDownBtn.visible = status;
        boolean map = tab == Tab.MAP;
        scanBtn.visible = map;
        modeBtn.visible = map;
        scanBtn.active = menu.sonarPresent() && menu.linked();
        boolean edicts = tab == Tab.EDICTS;
        phoenixBtn.visible = edicts;
        phoenixBtn.active = menu.linked() && menu.bound();
        lociApplyBtn.visible = edicts;
        lociResetBtn.visible = edicts;
        lociApplyBtn.active = menu.linked() && menu.bound() && lociDraftValid();
        lociResetBtn.active = menu.linked() && menu.bound();
        if (lociWordBox != null) {
            lociWordBox.visible = edicts && menu.linked();
            lociWordBox.active = edicts && menu.linked() && menu.bound();
            if (!edicts) {
                lociWordBox.setFocused(false);
            }
        }
        if (edicts) {
            phoenixBtn.setMessage(phoenixLabel());
        }
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

    private void phoenix() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TowerConsoleMenu.BUTTON_PHOENIX);
        }
    }

    private void applyLoci() {
        if (!lociDraftValid()) {
            return;
        }
        List<String> send = LexLociCompiler.isDefault(lociDraft) ? List.of() : List.copyOf(lociDraft);
        PacketDistributor.sendToServer(
                new ModNetworking.ApplyLociProgramPayload(menu.blockEntity().getBlockPos(), send));
        lociDirty = false;
    }

    private void resetLoci() {
        PacketDistributor.sendToServer(
                new ModNetworking.ApplyLociProgramPayload(menu.blockEntity().getBlockPos(), List.of()));
        lociDraft.clear();
        lociDraft.addAll(LexLociCompiler.defaultPhoenixTokens());
        lociDirty = false;
    }

    private boolean lociDraftValid() {
        return lociDraft.isEmpty() || LexLociCompiler.compile(lociDraft).ok();
    }

    private void scan() {
        PacketDistributor.sendToServer(
                new ModNetworking.TowerRemoteCommandPayload(
                        menu.blockEntity().getBlockPos(),
                        com.effecoria.core.tower.TowerRemoteService.ACTION_SCAN,
                        BlockPos.ZERO,
                        scanMode.id()));
    }

    private int mapX() {
        return leftPos + (PANEL_W - MAP_SIZE) / 2;
    }

    private int mapY() {
        return topPos + 22;
    }

    private int visibleRows() {
        return Math.max(1, LIST_H / ROW_H);
    }

    private void scroll(int delta) {
        List<TowerFacility.MonitorEntry> rows = menu.monitors();
        int max = Math.max(0, rows.size() - visibleRows());
        scroll = Math.max(0, Math.min(scroll + delta, max));
    }

    private int edictVisibleRows() {
        return Math.max(1, EDICT_LIST_H / 12);
    }

    private List<String> symbolRows() {
        List<String> out = new ArrayList<>();
        out.add("душа");
        out.add("владелец");
        out.add("шина:life");
        out.add("шина:industry");
        out.add("шина:defense");
        out.add("шина:psi");
        out.add("шина:broadband");
        Set<String> stars = new LinkedHashSet<>();
        for (TowerFacility.MonitorEntry e : menu.monitors()) {
            if (LociAddress.isAddressableKind(e.kind())) {
                stars.add(e.kind() + "*");
            }
        }
        out.addAll(stars);
        for (TowerFacility.MonitorEntry e : menu.monitors()) {
            out.add(e.symbol());
        }
        return out;
    }

    private void scrollEdicts(int delta) {
        List<String> rows = symbolRows();
        int max = Math.max(0, rows.size() - edictVisibleRows());
        edictScroll = Math.max(0, Math.min(edictScroll + delta, max));
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
        if (tab == Tab.EDICTS
                && mouseX >= leftPos + 10
                && mouseX <= leftPos + PANEL_W - 10
                && mouseY >= topPos + EDICT_LIST_Y
                && mouseY <= topPos + EDICT_LIST_Y + EDICT_LIST_H) {
            scrollEdicts(scrollY > 0 ? -1 : 1);
            return true;
        }
        if (tab == Tab.MAP && mapPainter.mouseScrolled(mapX(), mapY(), MAP_SIZE, mouseX, mouseY, scrollY)) {
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
            if (hitTab(mouseX, mouseY, 2)) {
                setTab(Tab.EDICTS);
                return true;
            }
        }
        if (tab == Tab.STATUS && button == 0 && menu.linked()) {
            List<TowerFacility.MonitorEntry> rows = menu.monitors();
            int lx = leftPos + LIST_X;
            int ly = topPos + LIST_Y;
            int end = Math.min(rows.size(), scroll + visibleRows());
            for (int i = scroll; i < end; i++) {
                int rowY = ly + (i - scroll) * ROW_H;
                if (mouseX >= lx && mouseX < lx + LIST_W - 16 && mouseY >= rowY && mouseY < rowY + ROW_H) {
                    TowerFacility.MonitorEntry e = rows.get(i);
                    BlockPos access = menu.blockEntity().getBlockPos();
                    BlockPos target = e.pos();
                    if ("turret".equals(e.kind())) {
                        PacketDistributor.sendToServer(new ModNetworking.TowerRemoteCommandPayload(
                                access,
                                com.effecoria.core.tower.TowerRemoteService.ACTION_TURRET_TOGGLE,
                                target,
                                0));
                        return true;
                    }
                    if ("beacon".equals(e.kind())) {
                        PacketDistributor.sendToServer(new ModNetworking.TowerRemoteCommandPayload(
                                access,
                                com.effecoria.core.tower.TowerRemoteService.ACTION_BEACON_QUERY,
                                target,
                                0));
                        return true;
                    }
                    break;
                }
            }
        }
        if (tab == Tab.EDICTS && button == 0 && menu.linked()) {
            List<String> suggestions = wordSuggestions();
            if (!suggestions.isEmpty() && lociWordBox != null && lociWordBox.isVisible()) {
                int sx = leftPos + 10;
                int sy = topPos + WORD_BOX_Y + WORD_BOX_H + 1;
                for (int i = 0; i < suggestions.size(); i++) {
                    int rowY = sy + i * SUGGEST_ROW_H;
                    if (mouseX >= sx
                            && mouseX < sx + 200
                            && mouseY >= rowY
                            && mouseY < rowY + SUGGEST_ROW_H) {
                        appendLociToken(suggestions.get(i));
                        return true;
                    }
                }
            }
            final boolean[] hit = {false};
            visitProgramChips((id, cx, cy, w, h, last) -> {
                if (hit[0] || !last) {
                    return;
                }
                if (mouseX >= cx && mouseX < cx + w && mouseY >= cy && mouseY < cy + h && !lociDraft.isEmpty()) {
                    lociDraft.remove(lociDraft.size() - 1);
                    lociDirty = true;
                    hit[0] = true;
                }
            });
            if (hit[0]) {
                return true;
            }
            if (mouseX >= leftPos + 10
                    && mouseX <= leftPos + PANEL_W - 10
                    && mouseY >= topPos + EDICT_LIST_Y + 10
                    && mouseY <= topPos + EDICT_LIST_Y + 10 + EDICT_LIST_H) {
                List<String> rows = symbolRows();
                int listY = topPos + EDICT_LIST_Y + 10;
                int end = Math.min(rows.size(), edictScroll + edictVisibleRows());
                for (int i = edictScroll; i < end; i++) {
                    int rowY = listY + (i - edictScroll) * 12;
                    if (mouseY < rowY || mouseY >= rowY + 12) {
                        continue;
                    }
                    String row = rows.get(i);
                    if (LexLociCompiler.isAddressToken(row) && LexLociCompiler.canAppend(lociDraft, row)) {
                        appendLociToken(row);
                        return true;
                    }
                    break;
                }
            }
        }
        if (tab == Tab.MAP && mapPainter.mouseClicked(mapX(), mapY(), MAP_SIZE, mouseX, mouseY, button)) {
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
        if (tab == Tab.MAP && mapPainter.mouseDragged(MAP_SIZE, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean hitTab(double mouseX, double mouseY, int index) {
        int x0 = leftPos + 78 + index * 48;
        int y0 = topPos + 4;
        return mouseX >= x0 && mouseX < x0 + 46 && mouseY >= y0 && mouseY < y0 + 14;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (scanBtn != null) {
            scanBtn.active = menu.sonarPresent() && menu.linked();
        }
        if (phoenixBtn != null && tab == Tab.EDICTS) {
            phoenixBtn.active = menu.linked() && menu.bound();
            phoenixBtn.setMessage(phoenixLabel());
        }
        if (lociApplyBtn != null && tab == Tab.EDICTS) {
            lociApplyBtn.active = menu.linked() && menu.bound() && lociDraftValid();
            lociResetBtn.active = menu.linked() && menu.bound();
        }
        if (!lociDirty) {
            List<String> server = LexLociCompiler.effectiveTokens(menu.lociTokens());
            if (!lociDraft.equals(server)) {
                lociDraft.clear();
                lociDraft.addAll(server);
            }
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
        } else if (tab == Tab.MAP) {
            drawMapTab(graphics, mouseX, mouseY);
        } else {
            drawEdictsTab(graphics, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics graphics) {
        drawTabChip(graphics, 0, "gui.effecoria.tower_console.tab.status", tab == Tab.STATUS);
        drawTabChip(graphics, 1, "gui.effecoria.tower_console.tab.map", tab == Tab.MAP);
        drawTabChip(graphics, 2, "gui.effecoria.tower_console.tab.edicts", tab == Tab.EDICTS);
    }

    private void drawTabChip(GuiGraphics graphics, int index, String key, boolean on) {
        int x0 = leftPos + 78 + index * 48;
        int y0 = topPos + 4;
        graphics.fill(x0, y0, x0 + 46, y0 + 14, on ? TAB_ON : TAB_OFF);
        graphics.fill(x0, y0 + 13, x0 + 46, y0 + 14, on ? TITLE : LINE);
        graphics.drawCenteredString(font, Component.translatable(key), x0 + 23, y0 + 3, on ? TITLE : MUTED);
    }

    private void drawEdictsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 10;
        graphics.drawString(
                font, Component.translatable("gui.effecoria.tower_console.edict.phoenix_title"), x, topPos + 22, TITLE, false);

        if (lociDraft.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.effecoria.tower_console.edict.empty"),
                    x,
                    topPos + 36,
                    MUTED,
                    false);
        } else {
            visitProgramChips((id, cx, cy, w, h, last) -> {
                boolean hover = mouseX >= cx && mouseX < cx + w && mouseY >= cy && mouseY < cy + h;
                graphics.fill(cx, cy, cx + w, cy + h, hover && last ? CHIP_HOVER : chipColor(id));
                graphics.drawCenteredString(font, lociLabel(id), cx + w / 2, cy + 4, LABEL);
                if (LexLociCompiler.isSense(id)) {
                    graphics.drawString(
                            font,
                            Component.translatable("gui.effecoria.tower_console.edict.then"),
                            cx + w + 2,
                            cy + 4,
                            MUTED,
                            false);
                }
            });
        }

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.tower_console.edict.word"),
                x,
                topPos + WORD_BOX_Y - 10,
                TITLE,
                false);

        List<String> suggestions = wordSuggestions();
        if (!suggestions.isEmpty()) {
            int sx = leftPos + 10;
            int sy = topPos + WORD_BOX_Y + WORD_BOX_H + 1;
            int boxH = suggestions.size() * SUGGEST_ROW_H + 2;
            graphics.fill(sx, sy, sx + 200, sy + boxH, BG_LIST);
            for (int i = 0; i < suggestions.size(); i++) {
                String token = suggestions.get(i);
                int rowY = sy + 1 + i * SUGGEST_ROW_H;
                boolean hover = mouseX >= sx
                        && mouseX < sx + 200
                        && mouseY >= rowY
                        && mouseY < rowY + SUGGEST_ROW_H;
                if (hover) {
                    graphics.fill(sx + 1, rowY, sx + 199, rowY + SUGGEST_ROW_H, CHIP_HOVER);
                }
                graphics.drawString(font, lociLabel(token), sx + 4, rowY + 2, hover ? TITLE : LABEL, false);
            }
        } else {
            int flagColor = menu.phoenixEdictEnabled() ? OK : MUTED;
            graphics.drawString(
                    font,
                    Component.translatable(
                            menu.phoenixEdictEnabled()
                                    ? "gui.effecoria.tower_console.edict.active"
                                    : "gui.effecoria.tower_console.edict.inactive"),
                    leftPos + 216,
                    topPos + WORD_BOX_Y,
                    flagColor,
                    false);
            int watchColor = menu.phoenixWatchdogActive() ? OK : MUTED;
            graphics.drawString(
                    font,
                    Component.translatable(
                            menu.phoenixWatchdogActive()
                                    ? "gui.effecoria.tower_console.edict.watchdog_active"
                                    : "gui.effecoria.tower_console.edict.watchdog_idle"),
                    leftPos + 216,
                    topPos + WORD_BOX_Y + 10,
                    watchColor,
                    false);
            boolean signalOn = menu.phoenixWatchdogActive()
                    && LexLociCompiler.compile(menu.lociTokens())
                            .actuatorsFor(LociEvent.SOUL_DEAD)
                            .contains(LociActuator.SIGNAL);
            int signalColor = signalOn ? WARN : MUTED;
            graphics.drawString(
                    font,
                    Component.translatable(
                            signalOn
                                    ? "gui.effecoria.tower_console.edict.signal_alarm"
                                    : "gui.effecoria.tower_console.edict.signal_idle"),
                    leftPos + 216,
                    topPos + WORD_BOX_Y + 20,
                    signalColor,
                    false);
        }

        graphics.drawString(
                font,
                Component.translatable("gui.effecoria.tower_console.edict.symbols"),
                x,
                topPos + EDICT_LIST_Y - 2,
                TITLE,
                false);
        int listY = topPos + EDICT_LIST_Y + 10;
        graphics.fill(leftPos + 8, listY - 2, leftPos + PANEL_W - 8, listY + EDICT_LIST_H, BG_LIST);
        List<String> rows = symbolRows();
        int end = Math.min(rows.size(), edictScroll + edictVisibleRows());
        for (int i = edictScroll; i < end; i++) {
            int rowY = listY + (i - edictScroll) * 12;
            String row = rows.get(i);
            boolean addr = LexLociCompiler.isAddressToken(row);
            boolean hover = addr
                    && mouseX >= leftPos + 10
                    && mouseX < leftPos + PANEL_W - 10
                    && mouseY >= rowY
                    && mouseY < rowY + 12;
            int color = addr ? (hover && LexLociCompiler.canAppend(lociDraft, row) ? TITLE : LABEL) : MUTED;
            graphics.drawString(font, row, x, rowY, color, false);
        }
    }

    private String lociLabel(String token) {
        if (LexLociCompiler.isAddressToken(token)) {
            return token;
        }
        return Component.translatable(LexLociCompiler.wordLabelKey(token)).getString();
    }

    private int chipColor(String token) {
        if (LexLociCompiler.isAddressToken(token)) {
            return CHIP_ADDR;
        }
        if (LexLociCompiler.WHEN_TOKEN.equals(token)) {
            return CHIP_PROGRAM;
        }
        if (LexLociCompiler.isSense(token)) {
            return CHIP_SENSE;
        }
        return CHIP_TRIGGER;
    }

    private int chipWidth(String token) {
        return Math.max(28, font.width(lociLabel(token)) + 8);
    }

    private int thenWidth() {
        return font.width(Component.translatable("gui.effecoria.tower_console.edict.then").getString()) + 6;
    }

    @FunctionalInterface
    private interface ChipVisitor {
        void accept(String id, int x, int y, int w, int h, boolean last);
    }

    private void visitProgramChips(ChipVisitor visitor) {
        int x = leftPos + 10;
        int y = topPos + 34;
        int maxX = leftPos + PANEL_W - 10;
        for (int i = 0; i < lociDraft.size(); i++) {
            String id = lociDraft.get(i);
            int w = chipWidth(id);
            int extra = LexLociCompiler.isSense(id) ? thenWidth() : 0;
            if ((LexLociCompiler.WHEN_TOKEN.equals(id) && i > 0) || x + w + extra > maxX) {
                x = leftPos + 10;
                y += CHIP_H + 2;
            }
            visitor.accept(id, x, y, w, CHIP_H, i == lociDraft.size() - 1);
            x += w + 4 + extra;
        }
    }

    private void appendLociToken(String token) {
        if (!LexLociCompiler.canAppend(lociDraft, token)) {
            return;
        }
        lociDraft.add(token);
        lociDirty = true;
        if (lociWordBox != null) {
            lociWordBox.setValue("");
        }
    }

    /** Palette words (+ address symbols when typing) that match the input and can append. */
    private List<String> wordSuggestions() {
        if (lociWordBox == null || !lociWordBox.isVisible() || !menu.linked() || !menu.bound()) {
            return List.of();
        }
        boolean focused = lociWordBox.isFocused();
        String raw = lociWordBox.getValue() == null ? "" : lociWordBox.getValue().trim();
        if (!focused && raw.isEmpty()) {
            return List.of();
        }
        String ql = raw.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String id : LexLociCompiler.palette()) {
            if (!LexLociCompiler.canAppend(lociDraft, id)) {
                continue;
            }
            if (!ql.isEmpty() && !matchesWordQuery(id, ql)) {
                continue;
            }
            out.add(id);
            if (out.size() >= SUGGEST_MAX) {
                return out;
            }
        }
        if (!ql.isEmpty()) {
            for (String row : symbolRows()) {
                if (!LexLociCompiler.isAddressToken(row) || !LexLociCompiler.canAppend(lociDraft, row)) {
                    continue;
                }
                if (!row.toLowerCase(Locale.ROOT).contains(ql)) {
                    continue;
                }
                out.add(row);
                if (out.size() >= SUGGEST_MAX) {
                    break;
                }
            }
        }
        return out;
    }

    private boolean matchesWordQuery(String token, String ql) {
        if (token.toLowerCase(Locale.ROOT).contains(ql)) {
            return true;
        }
        int colon = token.indexOf(':');
        String path = colon >= 0 ? token.substring(colon + 1) : token;
        if (path.toLowerCase(Locale.ROOT).contains(ql)) {
            return true;
        }
        return lociLabel(token).toLowerCase(Locale.ROOT).contains(ql);
    }

    private void drawMapTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int mx = mapX();
        int my = mapY();

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
            String ready = menu.sonarReady()
                    ? "gui.effecoria.tower_console.sonar_ready"
                    : "gui.effecoria.tower_console.sonar_busy";
            graphics.drawCenteredString(
                    font,
                    Component.translatable(ready),
                    leftPos + PANEL_W / 2,
                    my + MAP_SIZE / 2 + 8,
                    menu.sonarReady() ? OK : WARN);
            return;
        }

        mapPainter.draw(graphics, font, mx, my, MAP_SIZE, mouseX, mouseY, leftPos + 104, topPos + PANEL_H - 26);
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
            if (entry.named()) {
                name = name.copy().append(Component.literal("#" + entry.label()).withColor(MUTED & 0xFFFFFF));
            }
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
        } else if (tab == Tab.MAP) {
            if (isHovering(PANEL_W - 72, PANEL_H - 28, 62, 18, mouseX, mouseY)) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(
                        "gui.effecoria.tower_console.scan_tip_mode",
                        Component.translatable("gui.effecoria.phi_sonar.mode." + scanMode.name().toLowerCase()),
                        scanMode.phiCost()));
                tip.add(Component.translatable("gui.effecoria.phi_sonar.zoom_tip"));
                if (!menu.sonarPresent()) {
                    tip.add(Component.translatable("gui.effecoria.tower_console.sonar_missing"));
                } else if (!menu.sonarReady()) {
                    tip.add(Component.translatable("gui.effecoria.tower_console.sonar_busy"));
                }
                graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
            } else if (isHovering(10, PANEL_H - 28, 88, 18, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font,
                        Component.translatable("gui.effecoria.phi_sonar.mode_tip." + scanMode.name().toLowerCase()),
                        mouseX,
                        mouseY);
            }
        } else if (tab == Tab.EDICTS) {
            String hovered = hoveredLociWord(mouseX, mouseY);
            if (hovered != null) {
                if (LexLociCompiler.isAddressToken(hovered)) {
                    graphics.renderTooltip(
                            font,
                            Component.translatable("gui.effecoria.tower_console.edict.addr_hint"),
                            mouseX,
                            mouseY);
                } else {
                    graphics.renderTooltip(
                            font, Component.translatable(LexLociCompiler.wordLabelKey(hovered)), mouseX, mouseY);
                }
            } else if (hoveredAddressSymbol(mouseX, mouseY) != null) {
                graphics.renderTooltip(
                        font,
                        Component.translatable("gui.effecoria.tower_console.edict.addr_hint"),
                        mouseX,
                        mouseY);
            } else if (isHovering(10, PANEL_H - 28, 80, 18, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font, Component.translatable("gui.effecoria.tower_console.phoenix_tip"), mouseX, mouseY);
            } else if (isHovering(94, PANEL_H - 28, 70, 18, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font, Component.translatable("gui.effecoria.tower_console.edict.apply_tip"), mouseX, mouseY);
            } else if (isHovering(168, PANEL_H - 28, 70, 18, mouseX, mouseY)) {
                graphics.renderTooltip(
                        font, Component.translatable("gui.effecoria.tower_console.edict.reset_tip"), mouseX, mouseY);
            }
        }
    }

    private String hoveredAddressSymbol(int mouseX, int mouseY) {
        if (tab != Tab.EDICTS) {
            return null;
        }
        int listY = topPos + EDICT_LIST_Y + 10;
        if (mouseX < leftPos + 10
                || mouseX > leftPos + PANEL_W - 10
                || mouseY < listY
                || mouseY > listY + EDICT_LIST_H) {
            return null;
        }
        List<String> rows = symbolRows();
        int end = Math.min(rows.size(), edictScroll + edictVisibleRows());
        for (int i = edictScroll; i < end; i++) {
            int rowY = listY + (i - edictScroll) * 12;
            if (mouseY >= rowY && mouseY < rowY + 12) {
                String row = rows.get(i);
                return LexLociCompiler.isAddressToken(row) ? row : null;
            }
        }
        return null;
    }

    private String hoveredLociWord(int mouseX, int mouseY) {
        final String[] found = {null};
        visitProgramChips((id, cx, cy, w, h, last) -> {
            if (found[0] == null && mouseX >= cx && mouseX < cx + w && mouseY >= cy && mouseY < cy + h) {
                found[0] = id;
            }
        });
        if (found[0] != null) {
            return found[0];
        }
        List<String> suggestions = wordSuggestions();
        if (!suggestions.isEmpty()) {
            int sx = leftPos + 10;
            int sy = topPos + WORD_BOX_Y + WORD_BOX_H + 1;
            for (int i = 0; i < suggestions.size(); i++) {
                int rowY = sy + i * SUGGEST_ROW_H;
                if (mouseX >= sx && mouseX < sx + 200 && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H) {
                    return suggestions.get(i);
                }
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab == Tab.EDICTS && lociWordBox != null && lociWordBox.isVisible() && lociWordBox.isFocused()) {
            if (keyCode == 256) {
                lociWordBox.setFocused(false);
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                List<String> suggestions = wordSuggestions();
                if (!suggestions.isEmpty()) {
                    appendLociToken(suggestions.get(0));
                    return true;
                }
            }
            return lociWordBox.keyPressed(keyCode, scanCode, modifiers)
                    || lociWordBox.canConsumeInput()
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
