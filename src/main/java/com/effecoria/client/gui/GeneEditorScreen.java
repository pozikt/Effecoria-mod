package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.effecoria.content.ModItems;
import com.effecoria.effect.organic.gene.GeneAnatomySlot;
import com.effecoria.effect.organic.gene.GeneMod;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Organic gene editor — clickable host preview, per-part graft list. */
public class GeneEditorScreen extends Screen {
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 8;
    private static final int LIST_W = 158;
    private static final int PREVIEW_W = 128;
    private static final int PREVIEW_H = 176;
    private static final int PREVIEW_SCALE = 42;
    private static final float PREVIEW_Y_OFFSET = 0.0625f;
    private static final double DRAG_THRESHOLD = 4.0;

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

    private float previewYaw;
    private float previewPitch;
    private boolean previewPress;
    private boolean previewDrag;
    private double pressX;
    private double pressY;
    private double lastDragX;
    private double lastDragY;

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
        ensureHostSlot();
        List<String> visible = visibleMods();
        if (!visible.isEmpty()) {
            focusId = visible.get(0);
        } else if (!unlocked.isEmpty()) {
            focusId = unlocked.get(0);
            GeneMod.byId(focusId).ifPresent(mod -> slot = mod.slot());
        }
    }

    private void ensureHostSlot() {
        LivingEntity host = host();
        if (host == null || GeneBodyPick.hasSlot(host, slot)) {
            return;
        }
        for (GeneAnatomySlot candidate : GeneAnatomySlot.values()) {
            if (GeneBodyPick.hasSlot(host, candidate)) {
                slot = candidate;
                return;
            }
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

    private int previewRight() {
        return previewLeft() + PREVIEW_W;
    }

    private int previewBottom() {
        return previewTop() + PREVIEW_H;
    }

    private boolean inPreview(double mouseX, double mouseY) {
        return mouseX >= previewLeft()
                && mouseY >= previewTop()
                && mouseX < previewRight()
                && mouseY < previewBottom();
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
                if (host != null && !GeneBodyPick.hasSlot(host, slot)) {
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
        ensureHostSlot();
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

    private Component tissueHint() {
        int synth = count(ModItems.SYNTH_TISSUE.get());
        int phi = count(ModItems.PHI_SYNTH_TISSUE.get());
        boolean needOrdinary = false;
        boolean needPhi = false;
        for (GeneMod mod : selectedMods()) {
            if (mod.phiField()) {
                needPhi = true;
            } else {
                needOrdinary = true;
            }
        }
        if (selected.isEmpty()) {
            return Component.translatable("gui.effecoria.gene_editor.tissue_idle", synth, phi);
        }
        boolean covered = (!needOrdinary || synth > 0) && (!needPhi || phi > 0);
        if (covered) {
            return Component.translatable("gui.effecoria.gene_editor.tissue_ready", synth, phi);
        }
        return Component.translatable("gui.effecoria.gene_editor.tissue_missing", synth, phi);
    }

    private int tissueHintColor() {
        if (selected.isEmpty()) {
            return 0xFFA8C898;
        }
        boolean needOrdinary = false;
        boolean needPhi = false;
        for (GeneMod mod : selectedMods()) {
            if (mod.phiField()) {
                needPhi = true;
            } else {
                needOrdinary = true;
            }
        }
        boolean covered =
                (!needOrdinary || count(ModItems.SYNTH_TISSUE.get()) > 0)
                        && (!needPhi || count(ModItems.PHI_SYNTH_TISSUE.get()) > 0);
        return covered ? 0xFFB8F0C8 : 0xFFE0A070;
    }

    private int count(net.minecraft.world.item.Item item) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }
        return com.effecoria.effect.organic.gene.GeneEngineeringService.countItem(this.minecraft.player, item);
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
        if (button == 0 && inPreview(mouseX, mouseY)) {
            previewPress = true;
            previewDrag = false;
            pressX = mouseX;
            pressY = mouseY;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (previewPress && button == 0) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) > DRAG_THRESHOLD) {
                previewDrag = true;
            }
            if (previewDrag) {
                previewYaw += (float) (mouseX - lastDragX) * 0.9f;
                previewPitch = Mth.clamp(previewPitch + (float) (mouseY - lastDragY) * 0.55f, -55.0f, 55.0f);
            }
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (previewPress && button == 0) {
            boolean wasDrag = previewDrag;
            previewPress = false;
            previewDrag = false;
            if (!wasDrag) {
                selectPartAt(mouseX, mouseY);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectPartAt(double mouseX, double mouseY) {
        LivingEntity living = host();
        if (living == null || !inPreview(mouseX, mouseY)) {
            return;
        }
        GeneAnatomySlot hit = GeneBodyPick.hit(
                living,
                mouseX,
                mouseY,
                previewLeft(),
                previewTop(),
                previewRight(),
                previewBottom(),
                PREVIEW_SCALE,
                PREVIEW_Y_OFFSET,
                previewYaw,
                previewPitch);
        if (hit == null || hit == slot) {
            return;
        }
        slot = hit;
        scroll = 0;
        List<String> visible = visibleMods();
        if (!visible.isEmpty()) {
            focusId = visible.get(0);
        }
        rebuildButtons();
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
            renderHostPreview(graphics, living);
            drawPartHighlights(graphics, living, mouseX, mouseY);
        }

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
        y += 4;
        y = drawWrapped(graphics, tissueHint(), infoX, y, infoW, tissueHintColor(), yMax);
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

    private void renderHostPreview(GuiGraphics graphics, LivingEntity living) {
        int px = previewLeft();
        int py = previewTop();
        float cx = (px + previewRight()) / 2.0f;
        float cy = (py + previewBottom()) / 2.0f;
        float pitchRad = previewPitch * ((float) Math.PI / 180.0f);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateX(pitchRad);
        Quaternionf camera = new Quaternionf().rotateX(pitchRad);
        float bodyRot = living.yBodyRot;
        float bodyRotO = living.yBodyRotO;
        float yRot = living.getYRot();
        float xRot = living.getXRot();
        float headO = living.yHeadRotO;
        float head = living.yHeadRot;
        living.yBodyRot = 180.0f + previewYaw;
        living.yBodyRotO = living.yBodyRot;
        living.setYRot(living.yBodyRot);
        living.setXRot(-previewPitch);
        living.yHeadRot = living.getYRot();
        living.yHeadRotO = living.getYRot();
        Vector3f translation = new Vector3f(0.0f, living.getBbHeight() / 2.0f + PREVIEW_Y_OFFSET, 0.0f);
        graphics.enableScissor(px, py, previewRight(), previewBottom());
        InventoryScreen.renderEntityInInventory(
                graphics, cx, cy, PREVIEW_SCALE, translation, pose, camera, living);
        graphics.disableScissor();
        living.yBodyRot = bodyRot;
        living.yBodyRotO = bodyRotO;
        living.setYRot(yRot);
        living.setXRot(xRot);
        living.yHeadRotO = headO;
        living.yHeadRot = head;
    }

    private void drawPartHighlights(GuiGraphics graphics, LivingEntity living, int mouseX, int mouseY) {
        GeneAnatomySlot hover = null;
        if (!previewDrag && inPreview(mouseX, mouseY)) {
            hover = GeneBodyPick.hit(
                    living,
                    mouseX,
                    mouseY,
                    previewLeft(),
                    previewTop(),
                    previewRight(),
                    previewBottom(),
                    PREVIEW_SCALE,
                    PREVIEW_Y_OFFSET,
                    previewYaw,
                    previewPitch);
        }
        graphics.enableScissor(previewLeft(), previewTop(), previewRight(), previewBottom());
        if (hover != null && hover != slot) {
            fillPartBoxes(graphics, living, hover, 0x33FFFFFF, 0x88FFFFFF);
        }
        fillPartBoxes(graphics, living, slot, 0x44C8F0A0, 0xFFC8F0A0);
        graphics.disableScissor();
    }

    private void fillPartBoxes(
            GuiGraphics graphics, LivingEntity living, GeneAnatomySlot part, int fill, int outline) {
        for (GeneBodyPick.ScreenBox box : GeneBodyPick.screenBoxesForSlot(
                living,
                part,
                previewLeft(),
                previewTop(),
                previewRight(),
                previewBottom(),
                PREVIEW_SCALE,
                PREVIEW_Y_OFFSET,
                previewYaw,
                previewPitch)) {
            int x0 = Math.max(box.x0(), previewLeft());
            int y0 = Math.max(box.y0(), previewTop());
            int x1 = Math.min(box.x1(), previewRight());
            int y1 = Math.min(box.y1(), previewBottom());
            if (x1 <= x0 || y1 <= y0) {
                continue;
            }
            graphics.fill(x0, y0, x1, y1, fill);
            outlineBox(graphics, x0, y0, x1, y1, outline);
        }
    }

    private static void outlineBox(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);
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
