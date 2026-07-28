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
    public SchoolSelectScreen() {
        super(Component.translatable("gui.effecoria.school_select"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Button.builder(
                        Component.translatable("school.effecoria.elemental"),
                        button -> choose(MagicSchool.ELEMENTAL))
                .bounds(centerX - 110, centerY - 10, 220, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("school.effecoria.mental"),
                        button -> choose(MagicSchool.MENTAL))
                .bounds(centerX - 110, centerY + 20, 220, 20)
                .build());
    }

    private void choose(MagicSchool school) {
        PacketDistributor.sendToServer(new ModNetworking.InitiateSchoolPayload(school.getSerializedName()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 55, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.school_select.subtitle"),
                this.width / 2,
                this.height / 2 - 40,
                0xAAAAAA);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.school_select.elemental.desc"),
                this.width / 2,
                this.height / 2 - 28,
                0xFFAA66);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.school_select.mental.desc"),
                this.width / 2,
                this.height / 2 + 48,
                0xAA88FF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
