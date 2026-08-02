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
import net.neoforged.neoforge.network.PacketDistributor;

/** Organic gene-engineering editor — pick up to {@link GeneMod#MAX_SLOTS} grafts. */
public class GeneEditorScreen extends Screen {
    private final int targetEntityId;
    private final String targetName;
    private final List<String> unlocked;
    private final Set<String> selected = new LinkedHashSet<>();
    private String status = "";

    public GeneEditorScreen(int targetEntityId, String targetName, List<String> current, List<String> unlocked) {
        super(Component.translatable("gui.effecoria.gene_editor"));
        this.targetEntityId = targetEntityId;
        this.targetName = targetName == null ? "?" : targetName;
        this.unlocked = List.copyOf(unlocked);
        this.selected.addAll(current);
        while (this.selected.size() > GeneMod.MAX_SLOTS) {
            this.selected.remove(this.selected.iterator().next());
        }
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 150;
        int y = 48;
        for (String id : unlocked) {
            GeneMod.byId(id).ifPresent(mod -> {
                // captured for button
            });
            String modId = id;
            addRenderableWidget(Button.builder(buttonLabel(modId), b -> toggle(modId))
                    .bounds(left, y, 180, 18)
                    .build());
            y += 20;
        }
        int bottom = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.gene_editor.apply"), b -> apply())
                .bounds(this.width / 2 - 100, bottom, 90, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.gene_editor.clear"), b -> clear())
                .bounds(this.width / 2 - 5, bottom, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 + 70, bottom, 50, 20)
                .build());
    }

    private Component buttonLabel(String id) {
        return GeneMod.byId(id)
                .map(mod -> {
                    String mark = selected.contains(id) ? "[x] " : "[ ] ";
                    return Component.literal(mark).append(mod.title());
                })
                .orElse(Component.literal(id));
    }

    private void toggle(String id) {
        if (selected.contains(id)) {
            selected.remove(id);
            status = "";
            rebuildButtons();
            return;
        }
        if (selected.size() >= GeneMod.MAX_SLOTS) {
            status = Component.translatable("gui.effecoria.gene_editor.slots_full", GeneMod.MAX_SLOTS).getString();
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
        graphics.drawString(
                this.font,
                Component.translatable("gui.effecoria.gene_editor.hint", GeneMod.MAX_SLOTS),
                this.width / 2 + 40,
                50,
                0xFFA8C898,
                false);

        int infoX = this.width / 2 + 40;
        int infoY = 70;
        for (String id : selected) {
            GeneMod.byId(id).ifPresent(mod -> {
                // draw in loop with mutable y — use local
            });
        }
        int y = infoY;
        for (String id : selected) {
            var opt = GeneMod.byId(id);
            if (opt.isEmpty()) {
                continue;
            }
            GeneMod mod = opt.get();
            graphics.drawString(this.font, mod.title(), infoX, y, 0xFFE8FFD0, false);
            y += 12;
            graphics.drawString(this.font, mod.benefit(), infoX, y, 0xFF88CC88, false);
            y += 11;
            graphics.drawString(this.font, mod.cost(), infoX, y, 0xFFCC8888, false);
            y += 16;
        }
        if (!status.isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 48, 0xFFFFAA66);
        }
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
