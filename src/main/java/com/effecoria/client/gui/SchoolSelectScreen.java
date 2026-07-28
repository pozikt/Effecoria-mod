package com.effecoria.client.gui;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Phase 2 — permanent school choice at initiation. */
public class SchoolSelectScreen extends Screen {
    private static final MagicSchool[] PLAYABLE = {
            MagicSchool.ELEMENTAL,
            MagicSchool.MENTAL,
            MagicSchool.ORGANIC
    };

    public SchoolSelectScreen() {
        super(Component.translatable("gui.effecoria.school_select"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 20;

        for (MagicSchool school : PLAYABLE) {
            addRenderableWidget(Button.builder(
                            Component.translatable("school.effecoria." + school.getSerializedName()),
                            button -> choose(school))
                    .bounds(centerX - 110, y, 220, 20)
                    .build());
            y += 28;
        }
    }

    private void choose(MagicSchool school) {
        PacketDistributor.sendToServer(new ModNetworking.InitiateSchoolPayload(school.getSerializedName()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.school_select.subtitle"),
                this.width / 2,
                this.height / 2 - 56,
                0xAAAAAA);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.school_select.hint"),
                this.width / 2,
                this.height / 2 + 70,
                0x88AA88);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
