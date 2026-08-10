package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.technomagic.TechnomagicCatalog;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicNode;
import com.effecoria.core.technomagic.TechnomagicProgress;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Catalog list of technomagic nodes by era (cosmetic discovery only). */
public class TechnomagicScreen extends Screen {
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 200;
    private static final int ROW_H = 18;

    private final Screen parent;
    private int scroll;
    private int left;
    private int top;
    private final List<TechnomagicNode> rows = new ArrayList<>();

    public TechnomagicScreen(Screen parent) {
        super(Component.translatable("gui.effecoria.technomagic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2 - 8;
        rebuildRows();
        if (minecraft != null && minecraft.getConnection() != null && minecraft.player != null) {
            // Client-side seed from inventory icons; server craft hooks still authoritative.
            TechnomagicProgress progress = minecraft.player.getData(ModAttachments.TECHNOMAGIC.get());
            for (TechnomagicNode node : TechnomagicCatalog.sorted()) {
                if (node.status() != TechnomagicNode.TechnomagicStatus.AVAILABLE) {
                    continue;
                }
                Item icon = BuiltInRegistries.ITEM.get(node.icon());
                if (icon != null
                        && icon != Items.AIR
                        && minecraft.player.getInventory().contains(new ItemStack(icon))) {
                    progress.discover(node.id());
                }
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 40, top + PANEL_H + 12, 80, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-4))
                .bounds(left + PANEL_W - 22, top + 6, 16, 16)
                .build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(4))
                .bounds(left + PANEL_W - 22, top + PANEL_H - 22, 16, 16)
                .build());
    }

    private void rebuildRows() {
        rows.clear();
        rows.addAll(TechnomagicCatalog.sorted());
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
    }

    private int visibleRows() {
        return Math.max(1, (PANEL_H - 28) / ROW_H);
    }

    private void scroll(int delta) {
        scroll = Math.max(0, Math.min(scroll + delta, Math.max(0, rows.size() - visibleRows())));
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 4, top - 4, left + PANEL_W + 4, top + PANEL_H + 4, 0xCC1A2430);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE243848);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 16, 0xFFB8E0FF);

        TechnomagicProgress progress = TechnomagicProgress.createDefault();
        if (minecraft != null && minecraft.player != null) {
            progress = minecraft.player.getData(ModAttachments.TECHNOMAGIC.get());
        }

        int y = top + 8;
        int end = Math.min(rows.size(), scroll + visibleRows());
        TechnomagicEra lastEra = null;
        for (int i = scroll; i < end; i++) {
            TechnomagicNode node = rows.get(i);
            if (node.era() != lastEra) {
                lastEra = node.era();
                graphics.drawString(
                        this.font,
                        Component.translatable("technomagic.effecoria.era." + lastEra.number()),
                        left + 8,
                        y,
                        0xFF7EC8FF,
                        false);
                y += ROW_H;
                if (y > top + PANEL_H - ROW_H) {
                    break;
                }
            }
            boolean planned = node.status() == TechnomagicNode.TechnomagicStatus.PLANNED;
            boolean found = progress.isDiscovered(node);
            int color = planned ? 0xFF6A7080 : (found ? 0xFFE8F4FF : 0xFF8A9AAC);
            Component mark = planned
                    ? Component.translatable("technomagic.effecoria.status.planned")
                    : (found
                            ? Component.translatable("technomagic.effecoria.status.found")
                            : Component.translatable("technomagic.effecoria.status.unknown"));
            Component line = mark.copy().append(" · ").append(Component.translatable(node.translationKey()));
            graphics.drawString(this.font, line, left + 10, y, color, false);

            Item icon = BuiltInRegistries.ITEM.get(node.icon());
            if (icon != null && icon != Items.AIR && !planned) {
                graphics.renderItem(new ItemStack(icon), left + PANEL_W - 44, y - 1);
            }
            if (mouseX >= left + 8
                    && mouseX <= left + PANEL_W - 48
                    && mouseY >= y
                    && mouseY < y + ROW_H) {
                graphics.renderTooltip(this.font, Component.translatable(node.descKey()), mouseX, mouseY);
            }
            y += ROW_H;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
