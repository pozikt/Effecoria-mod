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

/** Lightweight in-mod primer — Patchouli substitute for Stage I teaching. */
public class MagicGuideScreen extends Screen {
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
        rebuildBody();
    }

    private void rebuildBody() {
        wrapped.clear();
        if (this.font == null) {
            return;
        }
        int textWidth = Math.max(120, this.width - 168);
        wrapped.addAll(this.font.split(chapter.body(), textWidth));
    }

    /** Solid veil only — menu blur would smear body text drawn in the same frame. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF0A1018);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int panelLeft = 128;
        int panelTop = 24;
        int panelRight = this.width - 12;
        int panelBottom = this.height - 36;
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF141A24);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF3A4868);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0xFF3A4868);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFE8F0FF);
        graphics.drawString(this.font, chapter.title(), panelLeft + 10, panelTop + 8, 0xFFE0A060, false);

        int y = panelTop + 26;
        int maxY = panelBottom - 8;
        for (FormattedCharSequence line : wrapped) {
            graphics.drawString(this.font, line, panelLeft + 10, y, 0xFFE8EEF4, false);
            y += 12;
            if (y > maxY) {
                break;
            }
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
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
