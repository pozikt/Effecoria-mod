package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.client.ClientGuiHooks;
import com.effecoria.core.progression.PrimerChapters;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/** Open leather-bound Magic Primer — expandable pages with NEW markers. */
public class MagicGuideScreen extends Screen {
    private static final int BOOK_W = 300;
    private static final int BOOK_H = 188;
    private static final int PAGE_PAD = 14;
    private static final int LINE_H = 11;
    private static final int BODY_LINES = 13;

    private enum LeafKind {
        COVER,
        TOC,
        CHAPTER
    }

    private record Leaf(
            LeafKind kind,
            PrimerChapters.Chapter chapter,
            int partIndex,
            int partCount,
            List<FormattedCharSequence> lines) {}

    private final Screen parent;
    private final PrimerChapters.Chapter startChapter;
    private final List<Leaf> leaves = new ArrayList<>();
    private int leafIndex;
    private int bookLeft;
    private int bookTop;
    private float turnPulse;
    private boolean openedSoundPlayed;

    public MagicGuideScreen() {
        this(null, (PrimerChapters.Chapter) null);
    }

    public MagicGuideScreen(Screen parent) {
        this(parent, (PrimerChapters.Chapter) null);
    }

    public MagicGuideScreen(PrimerChapters.Chapter start) {
        this(null, start);
    }

    public MagicGuideScreen(Screen parent, PrimerChapters.Chapter start) {
        super(Component.translatable("guide.effecoria.title"));
        this.parent = parent;
        this.startChapter = start;
    }

    @Override
    protected void init() {
        rebuildLeaves();
        bookLeft = (this.width - BOOK_W) / 2;
        bookTop = (this.height - BOOK_H) / 2 - 8;

        addRenderableWidget(Button.builder(Component.literal("◀"), b -> turn(-1))
                .bounds(bookLeft - 28, bookTop + BOOK_H / 2 - 10, 22, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> turn(1))
                .bounds(bookLeft + BOOK_W + 6, bookTop + BOOK_H / 2 - 10, 22, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 40, bookTop + BOOK_H + 14, 80, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.technomagic"), b -> {
                    ClientGuiHooks.openTechnomagic(this);
                })
                .bounds(this.width / 2 + 48, bookTop + BOOK_H + 14, 100, 20)
                .build());

        if (!openedSoundPlayed && minecraft != null) {
            openedSoundPlayed = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.85f, 0.9f));
        }
        markCurrentSeen();
    }

    private PlayerPsiData psi() {
        if (minecraft == null || minecraft.player == null) {
            return PlayerPsiData.createDefault();
        }
        return minecraft.player.getData(ModAttachments.PSI.get());
    }

    private void rebuildLeaves() {
        leaves.clear();
        PlayerPsiData data = psi();
        List<PrimerChapters.Chapter> chapters = PrimerChapters.visible(data);
        int textW = BOOK_W / 2 - PAGE_PAD * 2;

        leaves.add(new Leaf(
                LeafKind.COVER,
                null,
                0,
                1,
                this.font.split(Component.translatable("guide.effecoria.cover_body"), textW)));

        leaves.add(new Leaf(
                LeafKind.TOC,
                null,
                0,
                1,
                this.font.split(Component.translatable("guide.effecoria.toc_body"), textW)));

        for (PrimerChapters.Chapter ch : chapters) {
            List<FormattedCharSequence> body = this.font.split(ch.body(), textW);
            int parts = Math.max(1, (body.size() + BODY_LINES - 1) / BODY_LINES);
            for (int p = 0; p < parts; p++) {
                int from = p * BODY_LINES;
                int to = Math.min(body.size(), from + BODY_LINES);
                leaves.add(new Leaf(
                        LeafKind.CHAPTER,
                        ch,
                        p,
                        parts,
                        body.isEmpty() ? List.of() : body.subList(from, to)));
            }
        }

        if (startChapter != null) {
            for (int i = 0; i < leaves.size(); i++) {
                Leaf leaf = leaves.get(i);
                if (leaf.kind() == LeafKind.CHAPTER && leaf.chapter() == startChapter && leaf.partIndex() == 0) {
                    leafIndex = i;
                    return;
                }
            }
        }
        // Prefer first unseen chapter page, else cover.
        for (int i = 0; i < leaves.size(); i++) {
            Leaf leaf = leaves.get(i);
            if (leaf.kind() == LeafKind.CHAPTER
                    && leaf.partIndex() == 0
                    && leaf.chapter() != null
                    && !PrimerChapters.isSeen(data, leaf.chapter())) {
                leafIndex = i;
                return;
            }
        }
        leafIndex = Mth.clamp(leafIndex, 0, Math.max(0, leaves.size() - 1));
    }

    private void turn(int delta) {
        if (leaves.isEmpty()) {
            return;
        }
        int next = leafIndex + delta;
        if (next < 0 || next >= leaves.size()) {
            return;
        }
        leafIndex = next;
        turnPulse = 1f;
        if (minecraft != null) {
            float pitch = 0.9f + (minecraft.player != null
                    ? minecraft.player.getRandom().nextFloat() * 0.2f
                    : 0.1f);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, pitch));
        }
        markCurrentSeen();
    }

    private void markCurrentSeen() {
        if (minecraft == null || minecraft.player == null || leaves.isEmpty()) {
            return;
        }
        Leaf leaf = leaves.get(leafIndex);
        if (leaf.kind() != LeafKind.CHAPTER || leaf.chapter() == null) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(minecraft.player);
        if (PrimerChapters.isSeen(data, leaf.chapter())) {
            return;
        }
        data.setPrimerSeenMask(data.primerSeenMask() | leaf.chapter().mask());
        PsiHelper.set(minecraft.player, data);
        PacketDistributor.sendToServer(new ModNetworking.MarkPrimerChapterSeenPayload(leaf.chapter().bitIndex()));
        int stay = leafIndex;
        rebuildLeaves();
        leafIndex = Mth.clamp(stay, 0, Math.max(0, leaves.size() - 1));
    }

    private void jumpToChapter(PrimerChapters.Chapter chapter) {
        for (int i = 0; i < leaves.size(); i++) {
            Leaf leaf = leaves.get(i);
            if (leaf.kind() == LeafKind.CHAPTER && leaf.chapter() == chapter && leaf.partIndex() == 0) {
                if (i != leafIndex && minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.05f));
                }
                leafIndex = i;
                turnPulse = 1f;
                markCurrentSeen();
                return;
            }
        }
    }

    @Override
    public void tick() {
        if (turnPulse > 0f) {
            turnPulse = Math.max(0f, turnPulse - 0.12f);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Soft desk dim only — no menu blur / heavy veil over parchment.
        graphics.fill(0, 0, this.width, this.height, 0x66080810);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBook(graphics, mouseX, mouseY);
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawBook(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = bookLeft;
        int top = bookTop;
        int mid = left + BOOK_W / 2;
        float pulse = turnPulse;

        // Desk shadow
        graphics.fill(left - 10, top + BOOK_H - 4, left + BOOK_W + 10, top + BOOK_H + 10, 0x44000000);

        // Leather cover
        graphics.fill(left - 8, top - 8, left + BOOK_W + 8, top + BOOK_H + 8, 0xFF3A2418);
        graphics.fill(left - 6, top - 6, left + BOOK_W + 6, top + BOOK_H + 6, 0xFF5C3A24);
        graphics.fill(left - 5, top - 5, left + BOOK_W + 5, top + BOOK_H + 5, 0xFF7A5230);

        // Gold corner fillets
        int gold = 0xFFD4B24A;
        graphics.fill(left - 5, top - 5, left + 10, top - 3, gold);
        graphics.fill(left - 5, top - 5, left - 3, top + 10, gold);
        graphics.fill(left + BOOK_W - 10, top - 5, left + BOOK_W + 5, top - 3, gold);
        graphics.fill(left + BOOK_W + 3, top - 5, left + BOOK_W + 5, top + 10, gold);
        graphics.fill(left - 5, top + BOOK_H + 3, left + 10, top + BOOK_H + 5, gold);
        graphics.fill(left - 5, top + BOOK_H - 10, left - 3, top + BOOK_H + 5, gold);
        graphics.fill(left + BOOK_W - 10, top + BOOK_H + 3, left + BOOK_W + 5, top + BOOK_H + 5, gold);
        graphics.fill(left + BOOK_W + 3, top + BOOK_H - 10, left + BOOK_W + 5, top + BOOK_H + 5, gold);

        // Bright parchment spread
        int pageShift = Math.round(pulse * 4);
        graphics.fill(left, top, mid - 1, top + BOOK_H, 0xFFFFF6E4);
        graphics.fill(mid + 1 + pageShift, top, left + BOOK_W, top + BOOK_H, 0xFFFFF1D8);
        // Soft page edge only
        graphics.fill(left, top, left + 2, top + BOOK_H, 0x18C9A06A);
        graphics.fill(left + BOOK_W - 2, top, left + BOOK_W, top + BOOK_H, 0x18C9A06A);
        // Spine gutter
        graphics.fill(mid - 3, top, mid + 3, top + BOOK_H, 0x33281810);
        graphics.fill(mid - 1, top + 6, mid + 1, top + BOOK_H - 6, 0x55201008);

        // Very faint rules
        for (int y = top + 28; y < top + BOOK_H - 16; y += LINE_H) {
            graphics.fill(left + PAGE_PAD, y, mid - PAGE_PAD, y + 1, 0x10B09060);
            graphics.fill(mid + PAGE_PAD, y, left + BOOK_W - PAGE_PAD, y + 1, 0x10B09060);
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 18, 0xFFFFF0D0);

        if (leaves.isEmpty()) {
            return;
        }
        Leaf leaf = leaves.get(Mth.clamp(leafIndex, 0, leaves.size() - 1));
        drawLeftPage(graphics, left, top, mid, leaf, mouseX, mouseY);
        drawRightPage(graphics, mid, top, left + BOOK_W, leaf);

        Component footer = Component.translatable("guide.effecoria.page", leafIndex + 1, leaves.size());
        graphics.drawCenteredString(this.font, footer, this.width / 2, top + BOOK_H + 2, 0xFFE8D8B8);

        if (PrimerChapters.hasUnseen(psi())) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("guide.effecoria.unread_hint"),
                    this.width / 2,
                    top - 30,
                    0xFFFFC078);
        }
    }

    private void drawLeftPage(GuiGraphics graphics, int left, int top, int mid, Leaf leaf, int mouseX, int mouseY) {
        int x = left + PAGE_PAD;
        int y = top + PAGE_PAD;
        if (leaf.kind() == LeafKind.COVER) {
            graphics.drawString(this.font, Component.translatable("guide.effecoria.cover_title"), x, y, 0xFF5A3A20, false);
            y += LINE_H + 6;
            for (FormattedCharSequence line : leaf.lines()) {
                graphics.drawString(this.font, line, x, y, 0xFF3A2A18, false);
                y += LINE_H;
            }
            return;
        }
        if (leaf.kind() == LeafKind.TOC) {
            PlayerPsiData data = psi();
            List<PrimerChapters.Chapter> chapters = PrimerChapters.visible(data);
            graphics.drawString(this.font, Component.translatable("guide.effecoria.toc"), x, y, 0xFF6B4428, false);
            y += LINE_H + 4;
            for (int i = 0; i < chapters.size(); i++) {
                PrimerChapters.Chapter ch = chapters.get(i);
                boolean unseen = !PrimerChapters.isSeen(data, ch);
                int color = unseen ? 0xFF8B4518 : 0xFF3A2A18;
                Component row = Component.literal((i + 1) + ". ").append(ch.title());
                int rowW = this.font.width(row);
                boolean hover = mouseX >= x && mouseX <= x + rowW + 36 && mouseY >= y && mouseY < y + LINE_H;
                if (hover) {
                    color = 0xFFA05A20;
                }
                graphics.drawString(this.font, row, x, y, color, false);
                if (unseen) {
                    graphics.drawString(
                            this.font,
                            Component.translatable("guide.effecoria.new"),
                            x + rowW + 6,
                            y,
                            0xFFC94A2A,
                            false);
                }
                y += LINE_H + 1;
            }
            return;
        }
        // Chapter continuation: show short TOC reminder on left
        graphics.drawString(this.font, Component.translatable("guide.effecoria.toc"), x, y, 0xFF8A6A48, false);
        y += LINE_H + 4;
        PlayerPsiData data = psi();
        for (PrimerChapters.Chapter ch : PrimerChapters.visible(data)) {
            boolean current = leaf.chapter() == ch;
            boolean unseen = !PrimerChapters.isSeen(data, ch);
            int color = current ? 0xFF6B4428 : (unseen ? 0xFF8B4518 : 0xFF5A4A38);
            Component row = ch.title();
            if (current) {
                row = Component.literal("› ").append(row);
            }
            int rowW = this.font.width(row);
            boolean hover = mouseX >= x && mouseX <= x + Math.max(rowW, 80) && mouseY >= y && mouseY < y + LINE_H;
            if (hover && !current) {
                color = 0xFFA05A20;
            }
            graphics.drawString(this.font, row, x, y, color, false);
            if (unseen) {
                graphics.fill(x + rowW + 4, y + 2, x + rowW + 8, y + 6, 0xFFE05030);
            }
            y += LINE_H + 1;
        }
    }

    private void drawRightPage(GuiGraphics graphics, int mid, int top, int right, Leaf leaf) {
        int x = mid + PAGE_PAD;
        int y = top + PAGE_PAD;
        if (leaf.kind() == LeafKind.COVER) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("guide.effecoria.cover_flourish"),
                    mid + (right - mid) / 2,
                    top + BOOK_H / 2 - 4,
                    0xFF8A6A40);
            return;
        }
        if (leaf.kind() == LeafKind.TOC) {
            for (FormattedCharSequence line : leaf.lines()) {
                graphics.drawString(this.font, line, x, y, 0xFF3A2A18, false);
                y += LINE_H;
            }
            return;
        }
        Component title = leaf.chapter().title();
        if (leaf.partCount() > 1) {
            title = title.copy()
                    .append(Component.literal(" (" + (leaf.partIndex() + 1) + "/" + leaf.partCount() + ")"));
        }
        graphics.drawString(this.font, title, x, y, 0xFF6B4428, false);
        y += LINE_H + 6;
        for (FormattedCharSequence line : leaf.lines()) {
            graphics.drawString(this.font, line, x, y, 0xFF2E2218, false);
            y += LINE_H;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !leaves.isEmpty()) {
            Leaf leaf = leaves.get(Mth.clamp(leafIndex, 0, leaves.size() - 1));
            int left = bookLeft;
            int top = bookTop;
            int mid = left + BOOK_W / 2;
            int x = left + PAGE_PAD;
            // TOC / sidebar chapter clicks
            if (leaf.kind() == LeafKind.TOC || leaf.kind() == LeafKind.CHAPTER) {
                int y = top + PAGE_PAD + (leaf.kind() == LeafKind.TOC ? LINE_H + 4 : LINE_H + 4);
                if (leaf.kind() == LeafKind.TOC) {
                    y = top + PAGE_PAD + LINE_H + 4;
                }
                for (PrimerChapters.Chapter ch : PrimerChapters.visible(psi())) {
                    if (mouseX >= x && mouseX <= mid - PAGE_PAD && mouseY >= y && mouseY < y + LINE_H + 1) {
                        jumpToChapter(ch);
                        return true;
                    }
                    y += LINE_H + 1;
                }
            }
            // Click parchment halves to turn
            if (mouseY >= top && mouseY <= top + BOOK_H) {
                if (mouseX >= mid && mouseX <= left + BOOK_W) {
                    turn(1);
                    return true;
                }
                if (mouseX >= left && mouseX < mid && leaf.kind() == LeafKind.COVER) {
                    turn(1);
                    return true;
                }
                if (mouseX >= left && mouseX < mid) {
                    turn(-1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            turn(-1);
            return true;
        }
        if (scrollY < 0) {
            turn(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 262 || keyCode == 68) { // right / D
            turn(1);
            return true;
        }
        if (keyCode == 263 || keyCode == 65) { // left / A
            turn(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.7f, 0.75f));
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
