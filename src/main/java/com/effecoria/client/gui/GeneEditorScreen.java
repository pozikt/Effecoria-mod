package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.effecoria.effect.organic.gene.GeneMod;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/** Organic gene-engineering editor — pick grafts up to the engineer's slot budget. */
public class GeneEditorScreen extends Screen {
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 9;
    private static final int LIST_W = 168;
    private static final int DETAIL_PAD = 12;

    private final int targetEntityId;
    private final String targetName;
    private final List<String> unlocked;
    private final int maxSlots;
    private final Set<String> selected = new LinkedHashSet<>();
    private String status = "";
    private int scroll;
    /** Graft shown in the detail pane (last toggled / hovered). */
    private String focusId = "";

    public GeneEditorScreen(
            int targetEntityId, String targetName, List<String> current, List<String> unlocked, int maxSlots) {
        super(Component.translatable("gui.effecoria.gene_editor"));
        this.targetEntityId = targetEntityId;
        this.targetName = targetName == null ? "?" : targetName;
        this.unlocked = List.copyOf(unlocked);
        this.maxSlots = Math.max(1, maxSlots);
        this.selected.addAll(current);
        while (this.selected.size() > this.maxSlots) {
            this.selected.remove(this.selected.iterator().next());
        }
        if (!this.selected.isEmpty()) {
            this.focusId = this.selected.iterator().next();
        } else if (!this.unlocked.isEmpty()) {
            this.focusId = this.unlocked.get(0);
        }
    }

    private int listLeft() {
        return this.width / 2 - 170;
    }

    private int detailLeft() {
        return listLeft() + LIST_W + DETAIL_PAD;
    }

    private int detailWidth() {
        return Math.max(120, this.width - detailLeft() - 16);
    }

    @Override
    protected void init() {
        int left = listLeft();
        int listTop = 48;
        int maxScroll = Math.max(0, unlocked.size() - VISIBLE_ROWS);
        scroll = Math.min(scroll, maxScroll);
        int end = Math.min(unlocked.size(), scroll + VISIBLE_ROWS);
        int y = listTop;
        for (int i = scroll; i < end; i++) {
            String modId = unlocked.get(i);
            addRenderableWidget(Button.builder(buttonLabel(modId), b -> toggle(modId))
                    .bounds(left, y, LIST_W, 18)
                    .build());
            y += ROW_H;
        }
        int bottom = this.height - 28;
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.gene_editor.apply"), b -> apply())
                .bounds(cx - 100, bottom, 90, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.gene_editor.clear"), b -> clear())
                .bounds(cx - 5, bottom, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx + 70, bottom, 50, 20)
                .build());
    }

    private Component buttonLabel(String id) {
        return GeneMod.byId(id)
                .map(mod -> {
                    String mark = selected.contains(id) ? "[x] " : "[ ] ";
                    String raw = mark + mod.tierLabel().getString() + " · " + mod.title().getString();
                    return Component.literal(this.font.plainSubstrByWidth(raw, LIST_W - 8));
                })
                .orElse(Component.literal(id));
    }

    private void toggle(String id) {
        focusId = id;
        if (selected.contains(id)) {
            selected.remove(id);
            status = "";
            rebuildButtons();
            return;
        }
        if (selected.size() >= maxSlots) {
            status = Component.translatable("gui.effecoria.gene_editor.slots_full", maxSlots).getString();
            return;
        }
        Set<GeneMod> trial = new LinkedHashSet<>();
        for (String s : selected) {
            GeneMod.byId(s).ifPresent(trial::add);
        }
        GeneMod.byId(id).ifPresent(trial::add);
        if (!GeneMod.compatible(trial)) {
            status = Component.translatable("gui.effecoria.gene_editor.incompatible").getString();
            return;
        }
        selected.add(id);
        status = "";
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        init();
    }

    private void apply() {
        PacketDistributor.sendToServer(
                new ModNetworking.ApplyGeneModsPayload(targetEntityId, new ArrayList<>(selected)));
        onClose();
    }

    private void clear() {
        PacketDistributor.sendToServer(new ModNetworking.ClearGeneModsPayload(targetEntityId));
        selected.clear();
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, unlocked.size() - VISIBLE_ROWS);
        if (maxScroll > 0) {
            int before = scroll;
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY)));
            if (scroll != before) {
                rebuildButtons();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88081010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFE8F8E0);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.gene_editor.host", targetName),
                this.width / 2,
                26,
                0xFFB8D8A8);

        int infoX = detailLeft();
        int infoW = detailWidth();
        int yMax = this.height - 56;
        int y = 48;

        y = drawWrapped(
                graphics,
                Component.translatable("gui.effecoria.gene_editor.hint", maxSlots),
                infoX,
                y,
                infoW,
                0xFFA8C898,
                yMax);
        y += 6;

        if (!selected.isEmpty()) {
            y = drawWrapped(
                    graphics,
                    Component.translatable("gui.effecoria.gene_editor.selected", selected.size(), maxSlots),
                    infoX,
                    y,
                    infoW,
                    0xFFC8E0B8,
                    yMax);
            for (String id : selected) {
                var opt = GeneMod.byId(id);
                if (opt.isEmpty() || y >= yMax) {
                    break;
                }
                String mark = id.equals(focusId) ? "› " : "· ";
                y = drawWrapped(
                        graphics,
                        Component.literal(mark).append(opt.get().title()),
                        infoX,
                        y,
                        infoW,
                        id.equals(focusId) ? 0xFFE8FFD0 : 0xFFA0B898,
                        yMax);
            }
            y += 8;
        }

        updateFocusFromHover(mouseX, mouseY);
        if (GeneMod.byId(focusId).isPresent() && y < yMax) {
            GeneMod mod = GeneMod.byId(focusId).get();
            y = drawWrapped(graphics, mod.title(), infoX, y, infoW, 0xFFE8FFD0, yMax);
            y += 2;
            y = drawWrapped(
                    graphics,
                    Component.translatable("gui.effecoria.gene_editor.benefit"),
                    infoX,
                    y,
                    infoW,
                    0xFF7AB87A,
                    yMax);
            y = drawWrapped(graphics, mod.benefit(), infoX, y, infoW, 0xFF88CC88, yMax);
            y += 4;
            y = drawWrapped(
                    graphics,
                    Component.translatable("gui.effecoria.gene_editor.risk"),
                    infoX,
                    y,
                    infoW,
                    0xFFB87878,
                    yMax);
            drawWrapped(graphics, mod.cost(), infoX, y, infoW, 0xFFCC8888, yMax);
        }

        if (unlocked.size() > VISIBLE_ROWS) {
            graphics.drawString(
                    this.font,
                    Component.translatable("gui.effecoria.gene_editor.scroll"),
                    listLeft(),
                    this.height - 48,
                    0xFF889988,
                    false);
        }
        if (!status.isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 48, 0xFFFFAA66);
        }
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void updateFocusFromHover(int mouseX, int mouseY) {
        int listTop = 48;
        int end = Math.min(unlocked.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            int row = i - scroll;
            int by = listTop + row * ROW_H;
            if (mouseX >= listLeft() && mouseX <= listLeft() + LIST_W && mouseY >= by && mouseY < by + 18) {
                focusId = unlocked.get(i);
                return;
            }
        }
    }

    private int drawWrapped(
            GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color, int yMax) {
        if (y >= yMax) {
            return y;
        }
        List<FormattedCharSequence> lines = this.font.split(text, maxWidth);
        for (FormattedCharSequence line : lines) {
            if (y + this.font.lineHeight > yMax) {
                return yMax;
            }
            graphics.drawString(this.font, line, x, y, color, false);
            y += this.font.lineHeight + 1;
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
