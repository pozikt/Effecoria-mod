package com.effecoria.client.gui;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Phase 2 — pick active spell from known list. */
public class SpellBookScreen extends Screen {
    public SpellBookScreen() {
        super(Component.translatable("gui.effecoria.spell_book"));
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        int centerX = this.width / 2;
        int y = this.height / 2 - 40;

        for (int i = 0; i < data.knownSpells().size(); i++) {
            ResourceLocation spellId = data.knownSpells().get(i);
            boolean selected = i == data.selectedSpellIndex();
            Component label = selected
                    ? Component.translatable("gui.effecoria.spell_book.selected", Component.translatable("spell.effecoria." + spellId.getPath()))
                    : Component.translatable("spell.effecoria." + spellId.getPath());
            int index = i;
            addRenderableWidget(Button.builder(label, button -> select(index))
                    .bounds(centerX - 110, y, 220, 20)
                    .build());
            y += 24;
        }
    }

    private void select(int index) {
        PacketDistributor.sendToServer(new ModNetworking.SelectSpellPayload(index));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.effecoria.spell_book.hint"),
                this.width / 2,
                this.height / 2 - 48,
                0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
