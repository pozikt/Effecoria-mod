package com.effecoria.client.gui;

import com.effecoria.core.progression.PlayerRace;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Permanent race choice — Origins-style cards, then school select. */
public class RaceSelectScreen extends Screen {
    private static final PlayerRace[] RACES = PlayerRace.values();

    private static final int CARD_W = 304;
    private static final int CARD_GAP = 10;
    private static final int HEADER_H = 22;
    private static final int PAD = 8;
    private static final int BODY_LINE = 10;

    private final boolean mandatory;
    private final boolean openSchoolAfter;
    private double scrollY;
    private int contentHeight;
    private PlayerRace hoveredRace;

    public RaceSelectScreen() {
        this(false, true);
    }

    public RaceSelectScreen(boolean mandatory) {
        this(mandatory, true);
    }

    public RaceSelectScreen(boolean mandatory, boolean openSchoolAfter) {
        super(Component.translatable("gui.effecoria.race_select"));
        this.mandatory = mandatory;
        this.openSchoolAfter = openSchoolAfter;
    }

    @Override
    protected void init() {
        scrollY = 0;
    }

    private void choose(PlayerRace race) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
        PacketDistributor.sendToServer(new ModNetworking.SelectRacePayload(race.getSerializedName(), false, openSchoolAfter));
        onClose();
    }

    private int columns() {
        if (width >= CARD_W * 2 + CARD_GAP + 48) {
            return 2;
        }
        return 1;
    }

    private int gridWidth() {
        int cols = columns();
        return cols * CARD_W + (cols - 1) * CARD_GAP;
    }

    private int cardHeight(PlayerRace race) {
        int textW = CARD_W - PAD * 2;
        List<FormattedCharSequence> overview = font.split(race.overview(), textW);
        List<FormattedCharSequence> traits = font.split(race.traits(), textW);
        int body = PAD + BODY_LINE + 2 + overview.size() * BODY_LINE + 6 + BODY_LINE + 2 + traits.size() * BODY_LINE + PAD;
        return HEADER_H + body;
    }

    private int layoutTop() {
        return 52;
    }

    private int maxScroll() {
        int viewH = height - layoutTop() - 40;
        return Math.max(0, contentHeight - viewH);
    }

    private void layoutContentHeight() {
        int cols = columns();
        int rows = (RACES.length + cols - 1) / cols;
        int maxRowH = 0;
        for (int r = 0; r < rows; r++) {
            int rowH = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= RACES.length) {
                    break;
                }
                rowH = Math.max(rowH, cardHeight(RACES[idx]));
            }
            maxRowH += rowH + CARD_GAP;
        }
        contentHeight = Math.max(0, maxRowH - CARD_GAP);
    }

    private void cardPosition(int index, int[] out) {
        int cols = columns();
        int row = index / cols;
        int col = index % cols;
        int gridW = gridWidth();
        int left = (width - gridW) / 2;
        int y = layoutTop() - (int) scrollY;
        for (int r = 0; r < row; r++) {
            int rowH = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx < RACES.length) {
                    rowH = Math.max(rowH, cardHeight(RACES[idx]));
                }
            }
            y += rowH + CARD_GAP;
        }
        out[0] = left + col * (CARD_W + CARD_GAP);
        out[1] = y;
    }

    private PlayerRace raceAt(int mouseX, int mouseY) {
        layoutContentHeight();
        int[] pos = new int[2];
        for (int i = 0; i < RACES.length; i++) {
            cardPosition(i, pos);
            int h = cardHeight(RACES[i]);
            if (mouseX >= pos[0] && mouseX < pos[0] + CARD_W && mouseY >= pos[1] && mouseY < pos[1] + h) {
                return RACES[i];
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollY = Mth.clamp(this.scrollY - scrollY * 18, 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            PlayerRace hit = raceAt((int) mouseX, (int) mouseY);
            if (hit != null) {
                choose(hit);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xE0121218);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        layoutContentHeight();
        hoveredRace = raceAt(mouseX, mouseY);

        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.race_select.subtitle"), width / 2, 28, 0xAAAAAA);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.race_select.pick_hint"), width / 2, 40, 0x888888);

        int clipTop = layoutTop();
        int clipBottom = height - 28;
        graphics.enableScissor(0, clipTop, width, clipBottom);

        int[] pos = new int[2];
        for (int i = 0; i < RACES.length; i++) {
            PlayerRace race = RACES[i];
            cardPosition(i, pos);
            int h = cardHeight(race);
            if (pos[1] + h < clipTop || pos[1] > clipBottom) {
                continue;
            }
            drawCard(graphics, pos[0], pos[1], race, race == hoveredRace);
        }

        graphics.disableScissor();

        if (hoveredRace != null) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.race_select.click_choose", hoveredRace.title()),
                    width / 2,
                    height - 14,
                    0xFFE8C060);
        } else {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.race_select.scroll_hint"),
                    width / 2,
                    height - 14,
                    0x666666);
        }

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawCard(GuiGraphics graphics, int x, int y, PlayerRace race, boolean hover) {
        int h = cardHeight(race);
        int border = hover ? 0xFFE8C060 : 0xFF1A1A1A;
        graphics.fill(x - 1, y - 1, x + CARD_W + 1, y + h + 1, border);
        graphics.fill(x, y, x + CARD_W, y + h, 0xFF2B2B2B);

        graphics.fill(x, y, x + CARD_W, y + HEADER_H, race.headerColor());
        graphics.fill(x, y + HEADER_H - 1, x + CARD_W, y + HEADER_H, 0xFF1A1A1A);

        graphics.drawString(font, race.title(), x + 8, y + 7, 0xFFFFFF, false);
        drawDifficulty(graphics, x + CARD_W - 8, y + 7, race.difficulty());

        int textX = x + PAD;
        int cy = y + HEADER_H + PAD;
        int textW = CARD_W - PAD * 2;

        drawSectionHeader(graphics, textX, cy, Component.translatable("gui.effecoria.race_select.section.overview"));
        cy += BODY_LINE + 2;
        for (FormattedCharSequence line : font.split(race.overview(), textW)) {
            graphics.drawString(font, line, textX, cy, 0xDDDDDD, false);
            cy += BODY_LINE;
        }
        cy += 4;
        drawSectionHeader(graphics, textX, cy, Component.translatable("gui.effecoria.race_select.section.traits"));
        cy += BODY_LINE + 2;
        for (FormattedCharSequence line : font.split(race.traits(), textW)) {
            graphics.drawString(font, line, textX, cy, 0xBBBBBB, false);
            cy += BODY_LINE;
        }
    }

    private void drawSectionHeader(GuiGraphics graphics, int x, int y, Component text) {
        graphics.drawString(font, text, x, y, 0xFFFFFF, false);
        int w = font.width(text);
        graphics.fill(x, y + BODY_LINE, x + w, y + BODY_LINE + 1, 0xFF888888);
    }

    private void drawDifficulty(GuiGraphics graphics, int rightX, int y, int level) {
        int max = 3;
        for (int i = 0; i < max; i++) {
            int cx = rightX - (max - i) * 7;
            int color = i < level ? 0xFFCC3333 : 0xFF552222;
            graphics.fill(cx - 2, y, cx + 2, y + 4, color);
            graphics.fill(cx - 1, y - 1, cx + 1, y + 5, 0xFF1A1A1A);
            if (i < level) {
                graphics.fill(cx - 1, y, cx + 1, y + 4, color);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !mandatory;
    }
}
