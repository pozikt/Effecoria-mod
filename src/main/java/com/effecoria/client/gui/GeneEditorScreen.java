package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.effecoria.effect.organic.gene.GeneAnatomySlot;
import com.effecoria.effect.organic.gene.GeneMod;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Organic gene editor — clickable host preview, per-part graft list. */
public class GeneEditorScreen extends Screen {
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 8;
    private static final int LIST_W = 158;
    private static final int PREVIEW_W = 128;
    private static final int PREVIEW_H = 176;

    private final int targetEntityId;
    private final String targetName;
    private final List<String> unlocked;
    private final int maxSlots;
    private final boolean canLock;
    private final Set<String> selected = new LinkedHashSet<>();
    private boolean dnaLocked;
    private String status = "";
    private int scroll;
    private String focusId = "";
    private GeneAnatomySlot slot = GeneAnatomySlot.HEAD;

    public GeneEditorScreen(
            int targetEntityId,
            String targetName,
            List<String> current,
            List<String> unlocked,
            int maxSlots,
            boolean dnaLocked,
            boolean canLock) {
        super(Component.translatable("gui.effecoria.gene_editor"));
        this.targetEntityId = targetEntityId;
        this.targetName = targetName == null ? "?" : targetName;
        this.unlocked = List.copyOf(unlocked);
        this.maxSlots = Math.max(1, maxSlots);
        this.dnaLocked = dnaLocked;
        this.canLock = canLock;
        this.selected.addAll(current);
        while (this.selected.size() > this.maxSlots) {
            this.selected.remove(this.selected.iterator().next());
        }
        pickInitialFocus();
    }

    private void pickInitialFocus() {
        List<String> visible = visibleMods();
        if (!visible.isEmpty()) {
            focusId = visible.get(0);
        } else if (!unlocked.isEmpty()) {
            focusId = unlocked.get(0);
            GeneMod.byId(focusId).ifPresent(mod -> slot = mod.slot());
        }
    }

    private LivingEntity host() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        var entity = this.minecraft.level.getEntity(targetEntityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private int previewLeft() {
        return 16;
    }

    private int previewTop() {
        return 44;
    }

    private int listLeft() {
        return previewLeft() + PREVIEW_W + 12;
    }

    private int detailLeft() {
        return listLeft() + LIST_W + 12;
    }

    private int detailWidth() {
        return Math.max(110, this.width - detailLeft() - 16);
    }

    private List<String> visibleMods() {
        LivingEntity host = host();
        List<String> out = new ArrayList<>();
        for (String id : unlocked) {
            GeneMod.byId(id).ifPresent(mod -> {
                if (mod.slot() != slot) {
                    return;
                }
                if (host != null && !GeneAnatomySlot.presentOn(host, slot)) {
                    return;
                }
                out.add(id);
            });
        }
        return out;
    }

    private Set<GeneMod> selectedMods() {
        Set<GeneMod> set = new LinkedHashSet<>();
        for (String id : selected) {
            GeneMod.byId(id).ifPresent(set::add);
        }
        return set;
    }

    @Override
    protected void init() {
        List<String> visible = visibleMods();
        int maxScroll = Math.max(0, visible.size() - VISIBLE_ROWS);
        scroll = Math.min(scroll, maxScroll);
        int end = Math.min(visible.size(), scroll + VISIBLE_ROWS);
        int y = previewTop();
        int left = listLeft();
        for (int i = scroll; i < end; i++) {
            String modId = visible.get(i);
            addRenderableWidget(Button.builder(buttonLabel(modId), b -> toggle(modId))
                    .bounds(left, y, LIST_W, 18)
                    .build());
            y += ROW_H;
        }
        int bottom = this.height - 28;
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.gene_editor.apply"), b -> apply())
                .bounds(cx - 150, bottom, 72, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.gene_editor.clear"), b -> clear())
                .bounds(cx - 74, bottom, 64, 20)
                .build());
        if (canLock) {
            Component lockLabel = Component.translatable(
                    dnaLocked ? "gui.effecoria.gene_editor.unlock_dna" : "gui.effecoria.gene_editor.lock_dna");
            addRenderableWidget(Button.builder(lockLabel, b -> toggleLock())
                    .bounds(cx - 6, bottom, 92, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx + 92, bottom, 50, 20)
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
        if (dnaLocked) {
            status = Component.translatable("gui.effecoria.gene_editor.dna_locked").getString();
            return;
        }
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
        Set<GeneMod> trial = selectedMods();
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
        if (dnaLocked) {
            status = Component.translatable("gui.effecoria.gene_editor.dna_locked").getString();
            return;
        }
        PacketDistributor.sendToServer(
                new ModNetworking.ApplyGeneModsPayload(targetEntityId, new ArrayList<>(selected)));
        onClose();
    }

    private void clear() {
        if (dnaLocked) {
            status = Component.translatable("gui.effecoria.gene_editor.dna_locked").getString();
            return;
        }
        PacketDistributor.sendToServer(new ModNetworking.ClearGeneModsPayload(targetEntityId));
        selected.clear();
        onClose();
    }

    private void toggleLock() {
        if (!canLock) {
            return;
        }
        boolean next = !dnaLocked;
        if (next && selected.isEmpty()) {
            status = Component.translatable("gui.effecoria.gene_editor.lock_empty").getString();
            return;
        }
        if (next) {
            PacketDistributor.sendToServer(
                    new ModNetworking.ApplyGeneModsPayload(targetEntityId, new ArrayList<>(selected)));
        }
        PacketDistributor.sendToServer(new ModNetworking.LockGeneDnaPayload(targetEntityId, next));
        dnaLocked = next;
        status = Component.translatable(
                        next ? "gui.effecoria.gene_editor.dna_locked" : "gui.effecoria.gene_editor.dna_unlocked")
                .getString();
        rebuildButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<String> visible = visibleMods();
        int maxScroll = Math.max(0, visible.size() - VISIBLE_ROWS);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GeneAnatomySlot hit = hitSlot(mouseX, mouseY);
        if (hit != null) {
            slot = hit;
            scroll = 0;
            List<String> visible = visibleMods();
            if (!visible.isEmpty()) {
                focusId = visible.get(0);
            }
            rebuildButtons();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private GeneAnatomySlot hitSlot(double mouseX, double mouseY) {
        int x = previewLeft();
        int y = previewTop();
        if (mouseX < x || mouseY < y || mouseX >= x + PREVIEW_W || mouseY >= y + PREVIEW_H) {
            return null;
        }
        float u = (float) ((mouseX - x) / PREVIEW_W);
        float v = (float) ((mouseY - y) / PREVIEW_H);
        LivingEntity host = host();
        GeneAnatomySlot[] order = {
            GeneAnatomySlot.HEAD,
            GeneAnatomySlot.DORSUM,
            GeneAnatomySlot.FORE,
            GeneAnatomySlot.TAIL,
            GeneAnatomySlot.HIND,
            GeneAnatomySlot.TORSO
        };
        for (GeneAnatomySlot candidate : order) {
            if (host != null && !GeneAnatomySlot.presentOn(host, candidate)) {
                continue;
            }
            if (inBox(u, v, candidate)) {
                return candidate;
            }
        }
        return GeneAnatomySlot.TORSO;
    }

    private static boolean inBox(float u, float v, GeneAnatomySlot slot) {
        return switch (slot) {
            case HEAD -> u >= 0.28f && u <= 0.72f && v >= 0.04f && v <= 0.28f;
            case DORSUM -> u >= 0.06f && u <= 0.34f && v >= 0.18f && v <= 0.50f;
            case FORE -> u >= 0.66f && u <= 0.96f && v >= 0.28f && v <= 0.62f;
            case TORSO -> u >= 0.30f && u <= 0.70f && v >= 0.28f && v <= 0.58f;
            case HIND -> u >= 0.28f && u <= 0.72f && v >= 0.58f && v <= 0.86f;
            case TAIL -> u >= 0.68f && u <= 0.98f && v >= 0.62f && v <= 0.94f;
        };
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88081010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFE8F8E0);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.gene_editor.host", targetName),
                this.width / 2,
                24,
                0xFFB8D8A8);

        int px = previewLeft();
        int py = previewTop();
        graphics.fill(px - 1, py - 1, px + PREVIEW_W + 1, py + PREVIEW_H + 1, 0xFF1A2A18);
        graphics.fill(px, py, px + PREVIEW_W, py + PREVIEW_H, 0xFF0C140C);
        LivingEntity living = host();
        if (living != null) {
            graphics.enableScissor(px, py, px + PREVIEW_W, py + PREVIEW_H);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, px, py, px + PREVIEW_W, py + PREVIEW_H, 42, 0.0625f, mouseX, mouseY, living);
            graphics.disableScissor();
        }
        drawSlotHints(graphics, mouseX, mouseY);

        int infoX = detailLeft();
        int infoW = detailWidth();
        int yMax = this.height - 56;
        int y = previewTop();
        y = drawWrapped(
                graphics,
                Component.translatable("gui.effecoria.gene_editor.part", slot.title()),
                infoX,
                y,
                infoW,
                0xFFE8FFD0,
                yMax);
        y += 4;
        y = drawWrapped(
                graphics,
                Component.translatable("gui.effecoria.gene_editor.hint", maxSlots),
                infoX,
                y,
                infoW,
                0xFFA8C898,
                yMax);
        y += 6;
        if (dnaLocked) {
            y = drawWrapped(
                    graphics,
                    Component.translatable("gui.effecoria.gene_editor.dna_locked"),
                    infoX,
                    y,
                    infoW,
                    0xFFE0C070,
                    yMax);
            y += 6;
        }
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

        if (!status.isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 48, 0xFFFFAA66);
        }
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawSlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        GeneAnatomySlot hover = hitSlot(mouseX, mouseY);
        LivingEntity host = host();
        for (GeneAnatomySlot candidate : GeneAnatomySlot.values()) {
            if (host != null && !GeneAnatomySlot.presentOn(host, candidate)) {
                continue;
            }
            int color = candidate == slot ? 0x66C8F0A0 : candidate == hover ? 0x44FFFFFF : 0x22000000;
            fillSlot(graphics, candidate, color);
        }
    }

    private void fillSlot(GuiGraphics graphics, GeneAnatomySlot candidate, int color) {
        float[] box = switch (candidate) {
            case HEAD -> new float[] {0.28f, 0.04f, 0.72f, 0.28f};
            case DORSUM -> new float[] {0.06f, 0.18f, 0.34f, 0.50f};
            case FORE -> new float[] {0.66f, 0.28f, 0.96f, 0.62f};
            case TORSO -> new float[] {0.30f, 0.28f, 0.70f, 0.58f};
            case HIND -> new float[] {0.28f, 0.58f, 0.72f, 0.86f};
            case TAIL -> new float[] {0.68f, 0.62f, 0.98f, 0.94f};
        };
        int x = previewLeft();
        int y = previewTop();
        graphics.fill(
                x + (int) (box[0] * PREVIEW_W),
                y + (int) (box[1] * PREVIEW_H),
                x + (int) (box[2] * PREVIEW_W),
                y + (int) (box[3] * PREVIEW_H),
                color);
    }

    private void updateFocusFromHover(int mouseX, int mouseY) {
        List<String> visible = visibleMods();
        int end = Math.min(visible.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            int row = i - scroll;
            int by = previewTop() + row * ROW_H;
            if (mouseX >= listLeft() && mouseX <= listLeft() + LIST_W && mouseY >= by && mouseY < by + 18) {
                focusId = visible.get(i);
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
