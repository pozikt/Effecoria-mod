package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** Lightweight in-mod primer — Patchouli substitute for Stage I teaching. */
public class MagicGuideScreen extends Screen {
    private static final int LINE_H = 12;

    public enum Chapter {
        CAST_LOOP,
        PSI_PHI,
        ENTROPY,
        BREATHING,
        HUB_KEYS,
        SEALS,
        SCHOOL;

        public Component title() {
            return Component.translatable("guide.effecoria.chapter." + name().toLowerCase());
        }

        public Component body() {
            return Component.translatable("guide.effecoria.body." + name().toLowerCase());
        }
    }

    private final Screen parent;
    private Chapter chapter;
    private final List<FormattedCharSequence> wrapped = new ArrayList<>();
    private int scrollPx;

    public MagicGuideScreen() {
        this(null, Chapter.CAST_LOOP);
    }

    public MagicGuideScreen(Screen parent) {
        this(parent, Chapter.CAST_LOOP);
    }

    public MagicGuideScreen(Chapter start) {
        this(null, start != null ? start : Chapter.CAST_LOOP);
    }

    public MagicGuideScreen(Screen parent, Chapter start) {
        super(Component.translatable("guide.effecoria.title"));
        this.parent = parent;
        this.chapter = start != null ? start : Chapter.CAST_LOOP;
    }

    @Override
    protected void init() {
        clearWidgets();
        int left = 12;
        int y = 28;
        for (Chapter ch : Chapter.values()) {
            if (ch == Chapter.SEALS && !showSealsChapter()) {
                continue;
            }
            Chapter target = ch;
            addRenderableWidget(Button.builder(ch.title(), b -> select(target))
                    .bounds(left, y, 110, 18)
                    .build());
            y += 20;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());
        rebuildBody();
    }

    private boolean showSealsChapter() {
        if (minecraft == null || minecraft.player == null) {
            return true;
        }
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        return !data.initiated() || data.school() == MagicSchool.SEALS;
    }

    private void select(Chapter next) {
        chapter = next;
        scrollPx = 0;
        rebuildBody();
    }

    private void rebuildBody() {
        wrapped.clear();
        if (this.font == null) {
            return;
        }
        int textWidth = Math.max(120, this.width - 168);
        wrapped.addAll(this.font.split(chapter.body(), textWidth));
        clampScroll();
    }

    private int bodyViewportH() {
        return Math.max(40, this.height - 36 - 24 - 26 - 8);
    }

    private int maxScroll() {
        int content = wrapped.size() * LINE_H;
        return Math.max(0, content - bodyViewportH());
    }

    private void clampScroll() {
        scrollPx = Mth.clamp(scrollPx, 0, maxScroll());
    }

    /** Solid veil only — menu blur would smear body text drawn in the same frame. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF100E14);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelLeft = 128;
        int panelTop = 24;
        int panelRight = this.width - 12;
        int panelBottom = this.height - 36;
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF1A1520);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFFC9A06A);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF8A6A48);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFF0D8);
        graphics.drawString(this.font, chapter.title(), panelLeft + 10, panelTop + 8, 0xFFE8B878, false);

        int textTop = panelTop + 26;
        int textBottom = panelBottom - 8;
        int y = textTop - scrollPx;
        for (FormattedCharSequence line : wrapped) {
            if (y + LINE_H >= textTop && y <= textBottom) {
                graphics.drawString(this.font, line, panelLeft + 10, y, 0xFFFFF6E8, false);
            }
            y += LINE_H;
        }
        if (maxScroll() > 0) {
            graphics.drawString(
                    this.font,
                    Component.translatable("guide.effecoria.scroll_hint"),
                    panelLeft + 10,
                    panelBottom - 12,
                    0xFF9A8A78,
                    false);
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollPx -= (int) Math.round(scrollY * LINE_H * 2);
        clampScroll();
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
