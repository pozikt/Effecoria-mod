package com.effecoria.client.gui;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

/** Permanent school choice at first join — Origins-style description cards. */
public class SchoolSelectScreen extends Screen {
    private static final MagicSchool[] PLAYABLE = Arrays.stream(MagicSchool.values())
            .filter(MagicSchool::isPlayable)
            .toArray(MagicSchool[]::new);

    private static final int CARD_W = 304;
    private static final int CARD_GAP = 10;
    private static final int HEADER_H = 22;
    private static final int PAD = 8;
    private static final int BODY_LINE = 10;

    private final boolean mandatory;
    private double scrollY;
    private int contentHeight;
    private MagicSchool hoveredSchool;

    public SchoolSelectScreen() {
        this(false);
    }

    public SchoolSelectScreen(boolean mandatory) {
        super(Component.translatable("gui.effecoria.school_select"));
        this.mandatory = mandatory;
    }

    @Override
    protected void init() {
        scrollY = 0;
    }

    private void choose(MagicSchool school) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
        PacketDistributor.sendToServer(new ModNetworking.InitiateSchoolPayload(school.getSerializedName()));
        onClose();
    }

    private void deferSchool() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
        PacketDistributor.sendToServer(new ModNetworking.DeferSchoolPayload());
        onClose();
    }

    private int deferButtonY() {
        return height - 42;
    }

    private boolean hitDeferButton(int mouseX, int mouseY) {
        int bw = 160;
        int bh = 18;
        int bx = (width - bw) / 2;
        int by = deferButtonY();
        return mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
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

    private int cardHeight(MagicSchool school) {
        int textW = CARD_W - PAD * 2;
        List<FormattedCharSequence> overview = font.split(schoolOverview(school), textW);
        List<FormattedCharSequence> starters = font.split(starterLine(school), textW);
        int body = PAD + BODY_LINE + 2 + overview.size() * BODY_LINE + 6 + BODY_LINE + 2 + starters.size() * BODY_LINE + PAD;
        return HEADER_H + body;
    }

    private int layoutTop() {
        return 52;
    }

    private int maxScroll() {
        int viewH = height - layoutTop() - 56;
        return Math.max(0, contentHeight - viewH);
    }

    private void layoutContentHeight() {
        int cols = columns();
        int rows = (PLAYABLE.length + cols - 1) / cols;
        int maxRowH = 0;
        for (int r = 0; r < rows; r++) {
            int rowH = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= PLAYABLE.length) {
                    break;
                }
                rowH = Math.max(rowH, cardHeight(PLAYABLE[idx]));
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
                if (idx < PLAYABLE.length) {
                    rowH = Math.max(rowH, cardHeight(PLAYABLE[idx]));
                }
            }
            y += rowH + CARD_GAP;
        }
        out[0] = left + col * (CARD_W + CARD_GAP);
        out[1] = y;
    }

    private MagicSchool schoolAt(int mouseX, int mouseY) {
        layoutContentHeight();
        int[] pos = new int[2];
        for (int i = 0; i < PLAYABLE.length; i++) {
            cardPosition(i, pos);
            int h = cardHeight(PLAYABLE[i]);
            if (mouseX >= pos[0] && mouseX < pos[0] + CARD_W && mouseY >= pos[1] && mouseY < pos[1] + h) {
                return PLAYABLE[i];
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
            if (hitDeferButton((int) mouseX, (int) mouseY)) {
                deferSchool();
                return true;
            }
            MagicSchool hit = schoolAt((int) mouseX, (int) mouseY);
            if (hit != null) {
                choose(hit);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No menu blur — vanilla blur washes out custom cards (same approach as MagicGuideScreen / SpellHub).
        graphics.fill(0, 0, this.width, this.height, 0xE0121218);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        layoutContentHeight();
        hoveredSchool = schoolAt(mouseX, mouseY);

        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.school_select.subtitle"), width / 2, 28, 0xAAAAAA);
        graphics.drawCenteredString(
                font, Component.translatable("gui.effecoria.school_select.pick_hint"), width / 2, 40, 0x888888);

        int clipTop = layoutTop();
        int clipBottom = height - 48;
        graphics.enableScissor(0, clipTop, width, clipBottom);

        int[] pos = new int[2];
        for (int i = 0; i < PLAYABLE.length; i++) {
            MagicSchool school = PLAYABLE[i];
            cardPosition(i, pos);
            int h = cardHeight(school);
            if (pos[1] + h < clipTop || pos[1] > clipBottom) {
                continue;
            }
            boolean hover = school == hoveredSchool;
            drawCard(graphics, pos[0], pos[1], school, hover);
        }

        graphics.disableScissor();

        int deferBw = 160;
        int deferBh = 18;
        int deferBx = (width - deferBw) / 2;
        int deferBy = deferButtonY();
        boolean deferHover = hitDeferButton(mouseX, mouseY);
        graphics.fill(deferBx - 1, deferBy - 1, deferBx + deferBw + 1, deferBy + deferBh + 1,
                deferHover ? 0xFFE8C060 : 0xFF1A1A1A);
        graphics.fill(deferBx, deferBy, deferBx + deferBw, deferBy + deferBh, 0xFF2B2B2B);
        graphics.drawCenteredString(
                font,
                Component.translatable("gui.effecoria.school_select.defer"),
                width / 2,
                deferBy + 5,
                deferHover ? 0xFFE8C060 : 0xCCCCCC);

        if (hoveredSchool != null) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.school_select.click_choose", schoolTitle(hoveredSchool)),
                    width / 2,
                    height - 18,
                    0xFFE8C060);
        } else {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.effecoria.school_select.defer_hint"),
                    width / 2,
                    height - 18,
                    0x666666);
        }

        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawCard(GuiGraphics graphics, int x, int y, MagicSchool school, boolean hover) {
        int h = cardHeight(school);
        int border = hover ? 0xFFE8C060 : 0xFF1A1A1A;
        graphics.fill(x - 1, y - 1, x + CARD_W + 1, y + h + 1, border);
        graphics.fill(x, y, x + CARD_W, y + h, 0xFF2B2B2B);

        int headerCol = headerColor(school);
        graphics.fill(x, y, x + CARD_W, y + HEADER_H, headerCol);
        graphics.fill(x, y + HEADER_H - 1, x + CARD_W, y + HEADER_H, 0xFF1A1A1A);

        int iconSlot = 18;
        graphics.fill(x + 2, y + 2, x + 2 + iconSlot, y + HEADER_H - 2, 0xFF1A1A1A);
        drawSchoolIcon(graphics, school, x + 3, y + 3, iconSlot - 2);

        Component title = schoolTitle(school);
        graphics.drawString(font, title, x + 24, y + 7, 0xFFFFFF, false);

        drawDifficulty(graphics, x + CARD_W - 8, y + 7, difficulty(school));

        int textX = x + PAD;
        int cy = y + HEADER_H + PAD;
        int textW = CARD_W - PAD * 2;

        drawSectionHeader(graphics, textX, cy, Component.translatable("gui.effecoria.school_select.section.overview"));
        cy += BODY_LINE + 2;
        for (FormattedCharSequence line : font.split(schoolOverview(school), textW)) {
            graphics.drawString(font, line, textX, cy, 0xDDDDDD, false);
            cy += BODY_LINE;
        }
        cy += 4;
        drawSectionHeader(graphics, textX, cy, Component.translatable("gui.effecoria.school_select.section.starters"));
        cy += BODY_LINE + 2;
        for (FormattedCharSequence line : font.split(starterLine(school), textW)) {
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

    private static void drawSchoolIcon(GuiGraphics graphics, MagicSchool school, int x, int y, int size) {
        List<ResourceLocation> starters = SpellProgression.starterSpells(school);
        if (starters.isEmpty()) {
            return;
        }
        ResourceLocation icon = SpellIcons.forSpell(starters.getFirst());
        graphics.blitSprite(icon, x, y, size, size);
    }

    private static Component schoolTitle(MagicSchool school) {
        return Component.translatable("school.effecoria." + school.getSerializedName());
    }

    private static Component schoolOverview(MagicSchool school) {
        return Component.translatable("school.effecoria." + school.getSerializedName() + ".overview");
    }

    private Component starterLine(MagicSchool school) {
        List<ResourceLocation> starters = SpellProgression.starterSpells(school);
        if (starters.isEmpty()) {
            return Component.empty();
        }
        MutableComponent joined = Component.empty();
        for (int i = 0; i < starters.size(); i++) {
            if (i > 0) {
                joined.append(" · ");
            }
            joined.append(Component.translatable("spell.effecoria." + starters.get(i).getPath()));
        }
        return joined;
    }

    private static int headerColor(MagicSchool school) {
        return switch (school) {
            case ELEMENTAL -> 0xFF7A4520;
            case MENTAL -> 0xFF2A4488;
            case ORGANIC -> 0xFF2A6630;
            case NECROMANCY -> 0xFF4A4A48;
            case SPATIAL -> 0xFF5A5A70;
            case CORRUPTION -> 0xFF2A5A28;
            case SEALS -> 0xFF6A5020;
            default -> 0xFF6A5020;
        };
    }

    /** 1 = gentle, 3 = demanding (Origins-style impact dots). */
    private static int difficulty(MagicSchool school) {
        return switch (school) {
            case ELEMENTAL, ORGANIC, CORRUPTION -> 2;
            case SEALS -> 3;
            case MENTAL, NECROMANCY, SPATIAL -> 3;
            default -> 2;
        };
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
