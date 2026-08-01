package com.effecoria.client.gui;

import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.seal.SealProgramCompiler;
import com.effecoria.core.seal.SealProgramService;
import com.effecoria.core.seal.SealWordDefinition;
import com.effecoria.core.seal.SealWordRegistry;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Word-programming editor for Seals school inscriptions. */
public class SealProgramScreen extends Screen {
    private final BlockPos targetPos;
    private final List<ResourceLocation> program = new ArrayList<>();
    private int scroll;

    public SealProgramScreen(BlockPos targetPos) {
        super(Component.translatable("gui.effecoria.seal_program"));
        this.targetPos = targetPos.immutable();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 160;
        int right = this.width / 2 + 20;
        int bottom = this.height - 40;

        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_program.apply"), b -> apply())
                .bounds(right, bottom, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_program.clear_block"), b -> clearBlock())
                .bounds(right + 110, bottom, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_program.pop"), b -> pop())
                .bounds(left, bottom, 80, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_program.reset"), b -> {
                    program.clear();
                })
                .bounds(left + 90, bottom, 80, 20)
                .build());
    }

    private PlayerPsiData data() {
        return this.minecraft.player.getData(ModAttachments.PSI.get());
    }

    private int maxTokens() {
        return SealProgramCompiler.maxTokens(BreathingService.referenceRatio(data().breathingMastery()));
    }

    private List<SealWordDefinition> knownWords() {
        PlayerPsiData data = data();
        List<SealWordDefinition> words = new ArrayList<>();
        for (ResourceLocation id : data.knownSealWords()) {
            SealWordRegistry.get(id).ifPresent(words::add);
        }
        words.sort(Comparator
                .comparing((SealWordDefinition w) -> w.kind().ordinal())
                .thenComparing(w -> w.id().getPath()));
        return words;
    }

    private void push(ResourceLocation id) {
        if (program.size() >= maxTokens()) {
            return;
        }
        if (!data().knowsSealWord(id)) {
            return;
        }
        program.add(id);
    }

    private void pop() {
        if (!program.isEmpty()) {
            program.remove(program.size() - 1);
        }
    }

    private void apply() {
        PacketDistributor.sendToServer(new ModNetworking.ApplySealProgramPayload(targetPos, List.copyOf(program)));
        onClose();
    }

    private void clearBlock() {
        PacketDistributor.sendToServer(new ModNetworking.ClearSealProgramPayload(targetPos));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xAA101018);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xE8E0FF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "gui.effecoria.seal_program.target",
                        targetPos.getX(),
                        targetPos.getY(),
                        targetPos.getZ()),
                this.width / 2,
                26,
                0xAAAAAA);

        float cost = SealProgramService.previewCost(data(), program);
        graphics.drawString(
                this.font,
                Component.translatable(
                        "gui.effecoria.seal_program.scheme",
                        program.size(),
                        maxTokens(),
                        String.format("%.1f", cost)),
                this.width / 2 - 160,
                44,
                0xD0FFD0,
                false);

        int schemeX = this.width / 2 - 160;
        int schemeY = 60;
        for (int i = 0; i < program.size(); i++) {
            ResourceLocation id = program.get(i);
            String label = Component.translatable("seal_word.effecoria." + id.getPath()).getString();
            graphics.fill(schemeX + i * 54, schemeY, schemeX + i * 54 + 50, schemeY + 18, 0xFF3A2A55);
            graphics.drawCenteredString(this.font, label, schemeX + i * 54 + 25, schemeY + 5, 0xFFFFFF);
        }

        List<SealWordDefinition> words = knownWords();
        int cols = 4;
        int startY = 95;
        int visible = 16;
        int from = Math.min(scroll, Math.max(0, words.size() - visible));
        int to = Math.min(words.size(), from + visible);
        for (int i = from; i < to; i++) {
            SealWordDefinition word = words.get(i);
            int idx = i - from;
            int col = idx % cols;
            int row = idx / cols;
            int x = this.width / 2 - 160 + col * 85;
            int y = startY + row * 24;
            boolean hover = mouseX >= x && mouseX <= x + 80 && mouseY >= y && mouseY <= y + 20;
            graphics.fill(x, y, x + 80, y + 20, hover ? 0xFF554488 : 0xFF2A2438);
            String label = Component.translatable("seal_word.effecoria." + word.id().getPath()).getString();
            graphics.drawCenteredString(this.font, label, x + 40, y + 6, 0xEDE6FF);
        }

        graphics.drawString(
                this.font,
                Component.translatable("gui.effecoria.seal_program.hint"),
                this.width / 2 - 160,
                this.height - 58,
                0x8899AA,
                false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<SealWordDefinition> words = knownWords();
            int cols = 4;
            int startY = 95;
            int visible = 16;
            int from = Math.min(scroll, Math.max(0, words.size() - visible));
            int to = Math.min(words.size(), from + visible);
            for (int i = from; i < to; i++) {
                int idx = i - from;
                int col = idx % cols;
                int row = idx / cols;
                int x = this.width / 2 - 160 + col * 85;
                int y = startY + row * 24;
                if (mouseX >= x && mouseX <= x + 80 && mouseY >= y && mouseY <= y + 20) {
                    push(words.get(i).id());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 4);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
