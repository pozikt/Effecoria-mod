package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.seal.SealScriptLexicon;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Seal reality editor — named glue cells, typed overlay assignments, autocomplete. */
public class SealProgramScreen extends Screen {
    private static final int EXPRESSION_SLOTS = 4;
    private static final int SUGGEST_ROW_H = 12;
    private static final List<String> PROPERTIES = List.of(
            "glow", "hardness", "sound", "hurt", "slow", "push", "calor", "clausura", "umbra", "servare");
    private static final List<String> SPECS = List.of("step", "hit", "use", "break", "approach", "player", "mob");

    private final BlockPos targetPos;
    private final int maxTargets;
    private final List<ModNetworking.SealEditorMember> members;
    private String source;
    private MultiLineEditBox scriptBox;
    private EditBox aliasBox;
    private int selectedMember = -1;
    private int memberScroll;

    public SealProgramScreen(
            BlockPos targetPos,
            int maxTargets,
            String source,
            List<ModNetworking.SealEditorMember> members) {
        super(Component.translatable("gui.effecoria.seal_program"));
        this.targetPos = targetPos.immutable();
        this.maxTargets = Math.max(1, maxTargets);
        this.source = source == null ? "" : source;
        this.members = new ArrayList<>(members);
        SealEditorHighlights.set(this.members, targetPos);
    }

    @Override
    protected void init() {
        int left = 16;
        int editorX = 168;
        int editorW = Math.max(220, this.width - editorX - 16);
        int editorH = Math.max(80, this.height - 120);
        scriptBox = new MultiLineEditBox(
                this.font,
                editorX,
                44,
                editorW,
                editorH,
                Component.translatable("gui.effecoria.seal_program.placeholder"),
                this.title);
        scriptBox.setValue(source);
        scriptBox.setCharacterLimit(8000);
        addRenderableWidget(scriptBox);

        aliasBox = new EditBox(this.font, left, this.height - 72, 140, 18, Component.translatable("gui.effecoria.seal_program.alias"));
        aliasBox.setMaxLength(24);
        aliasBox.setHint(Component.translatable("gui.effecoria.seal_program.alias"));
        addRenderableWidget(aliasBox);

        int bottom = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_program.apply"), b -> apply())
                .bounds(this.width / 2 - 110, bottom, 88, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.effecoria.seal_program.clear_block"), b -> clearBlock())
                .bounds(this.width / 2 - 16, bottom, 72, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 + 62, bottom, 50, 20)
                .build());

        int saveY = bottom - 24;
        int loadY = saveY - 24;
        for (int i = 0; i < EXPRESSION_SLOTS; i++) {
            final int slot = i;
            int x = editorX + i * 52;
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.effecoria.seal_program.load_slot", slot + 1),
                            b -> loadExpression(slot))
                    .bounds(x, loadY, 50, 20)
                    .build());
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.effecoria.seal_program.save_slot", slot + 1),
                            b -> saveExpression(slot))
                    .bounds(x, saveY, 50, 20)
                    .build());
        }
    }

    private PlayerPsiData data() {
        return this.minecraft.player.getData(ModAttachments.PSI.get());
    }

    private String script() {
        return scriptBox == null ? source : scriptBox.getValue();
    }

    private void apply() {
        source = script();
        PacketDistributor.sendToServer(
                new ModNetworking.ApplySealScriptPayload(targetPos, source, List.copyOf(members)));
        onClose();
    }

    private void clearBlock() {
        PacketDistributor.sendToServer(new ModNetworking.ClearSealProgramPayload(targetPos));
        onClose();
    }

    private void saveExpression(int slot) {
        PacketDistributor.sendToServer(new ModNetworking.SaveSealScriptPayload(slot, script()));
    }

    private void loadExpression(int slot) {
        String saved = data().savedSealScript(slot);
        if (saved.isEmpty()) {
            var tokens = data().savedSealExpression(slot);
            if (!tokens.isEmpty()) {
                String symbol = members.isEmpty() ? "block" : members.get(0).typeKey();
                saved = com.effecoria.core.seal.SealScriptPrinter.fromTokens(symbol, tokens);
            }
        }
        if (!saved.isEmpty() && scriptBox != null) {
            scriptBox.setValue(saved);
            source = saved;
        }
    }

    private void recomputeConflicts() {
        Map<String, Integer> counts = new HashMap<>();
        List<String> symbols = new ArrayList<>();
        for (ModNetworking.SealEditorMember member : members) {
            String symbol = member.alias().isBlank() ? member.typeKey() : member.typeKey() + "#" + member.alias();
            symbols.add(symbol);
            counts.merge(symbol, 1, Integer::sum);
        }
        for (int i = 0; i < members.size(); i++) {
            var old = members.get(i);
            members.set(
                    i,
                    new ModNetworking.SealEditorMember(
                            old.pos(), old.typeKey(), old.alias(), counts.getOrDefault(symbols.get(i), 0) > 1));
        }
    }

    private void insertSymbol(ModNetworking.SealEditorMember member) {
        String symbol = member.alias().isBlank() ? member.typeKey() : member.typeKey() + "#" + member.alias();
        if (scriptBox == null) {
            return;
        }
        String current = scriptBox.getValue();
        if (current == null) {
            current = "";
        }
        String insert = current.isEmpty() || current.endsWith("\n") ? symbol + ":\n  " : "\n" + symbol + ":\n  ";
        scriptBox.setValue(current + insert);
    }

    private int inscribedCount() {
        int count = 0;
        for (String line : script().split("\n")) {
            String trimmed = line.trim();
            if (trimmed.endsWith(":") && !trimmed.isEmpty()) {
                int colon = trimmed.indexOf(':');
                String head = trimmed.substring(0, colon).trim();
                if (!head.isEmpty() && !SealScriptLexicon.isWhen(head.split("\\s+")[0])) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<String> suggestions() {
        String text = script();
        int nl = text.lastIndexOf('\n');
        String last = (nl < 0 ? text : text.substring(nl + 1)).trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        boolean whenLine = last.startsWith("when")
                || last.startsWith("когда")
                || last.startsWith("if")
                || last.startsWith("если");
        if (whenLine && !last.endsWith(":")) {
            String rest = last.replaceFirst("^(when|когда|if|если)\\s*", "");
            for (String spec : SPECS) {
                if (spec.startsWith(rest) || rest.isEmpty()) {
                    out.add("when " + spec + ":");
                }
            }
            return cap(out);
        }
        if (last.contains("=")) {
            return out;
        }
        boolean afterColon = last.endsWith(":") || last.isEmpty();
        if (afterColon) {
            addPropertySuggestions(out, "");
            out.add("when step:");
            return cap(out);
        }
        for (ModNetworking.SealEditorMember member : members) {
            if (member.conflict() && member.alias().isBlank()) {
                continue;
            }
            String symbol = member.alias().isBlank() ? member.typeKey() : member.typeKey() + "#" + member.alias();
            if (symbol.startsWith(last)) {
                out.add(symbol + ":");
            }
        }
        addPropertySuggestions(out, last);
        if ("when".startsWith(last) || "когда".startsWith(last)) {
            out.add("when step:");
        }
        return cap(out);
    }

    private static void addPropertySuggestions(List<String> out, String prefix) {
        for (String prop : PROPERTIES) {
            if (prefix.isEmpty() || prop.startsWith(prefix)) {
                out.add(prop);
            }
        }
    }

    private static List<String> cap(List<String> out) {
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private void acceptSuggestion(String token) {
        String text = script();
        int nl = text.lastIndexOf('\n');
        String prefix = nl < 0 ? "" : text.substring(0, nl + 1);
        scriptBox.setValue(prefix + token + (token.endsWith(":") ? "\n  " : " = "));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xE8E0FF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "gui.effecoria.seal_program.target",
                        targetPos.getX(),
                        targetPos.getY(),
                        targetPos.getZ()),
                this.width / 2,
                20,
                0xAAAAAA);
        graphics.drawString(
                this.font,
                Component.translatable("gui.effecoria.seal_program.scheme", inscribedCount(), maxTargets, "—"),
                168,
                32,
                0xD0FFD0,
                false);

        graphics.drawString(this.font, Component.translatable("gui.effecoria.seal_program.symbols"), 16, 32, 0xC8D8F0, false);
        int listY = 44;
        int visible = Math.max(4, (this.height - 140) / 14);
        int from = Math.min(memberScroll, Math.max(0, members.size() - visible));
        int to = Math.min(members.size(), from + visible);
        for (int i = from; i < to; i++) {
            ModNetworking.SealEditorMember member = members.get(i);
            int y = listY + (i - from) * 14;
            boolean hover = mouseX >= 16 && mouseX <= 156 && mouseY >= y && mouseY < y + 14;
            boolean selected = i == selectedMember;
            int color = member.conflict() ? 0xFF553333 : selected ? 0xFF3A4A2A : hover ? 0xFF333348 : 0xFF242430;
            graphics.fill(16, y, 156, y + 13, color);
            String label = member.alias().isBlank() ? member.typeKey() : member.typeKey() + "#" + member.alias();
            if (member.conflict()) {
                label += " ?";
            }
            graphics.drawString(this.font, label, 18, y + 3, member.conflict() ? 0xFFCC8888 : 0xFFEDE6FF, false);
        }

        List<String> suggestions = suggestions();
        if (!suggestions.isEmpty() && scriptBox != null) {
            int sx = 168;
            int sy = this.height - 96;
            graphics.fill(sx, sy, sx + 220, sy + suggestions.size() * SUGGEST_ROW_H + 2, 0xCC101018);
            for (int i = 0; i < suggestions.size(); i++) {
                int rowY = sy + 1 + i * SUGGEST_ROW_H;
                boolean hover = mouseX >= sx && mouseX <= sx + 220 && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H;
                graphics.drawString(this.font, suggestions.get(i), sx + 4, rowY + 1, hover ? 0xFFFFFF : 0xC8D0E8, false);
            }
        }

        graphics.drawString(
                this.font,
                Component.translatable("gui.effecoria.seal_program.hint"),
                16,
                this.height - 48,
                0x8899AA,
                false);
        SealEditorHighlights.setSelected(selectedMember >= 0 && selectedMember < members.size()
                ? members.get(selectedMember).pos()
                : null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listY = 44;
        int visible = Math.max(4, (this.height - 140) / 14);
        int from = Math.min(memberScroll, Math.max(0, members.size() - visible));
        int to = Math.min(members.size(), from + visible);
        for (int i = from; i < to; i++) {
            int y = listY + (i - from) * 14;
            if (mouseX >= 16 && mouseX <= 156 && mouseY >= y && mouseY < y + 14) {
                selectedMember = i;
                ModNetworking.SealEditorMember member = members.get(i);
                aliasBox.setValue(member.alias());
                if (button == 0) {
                    insertSymbol(member);
                }
                return true;
            }
        }
        List<String> suggestions = suggestions();
        if (!suggestions.isEmpty()) {
            int sx = 168;
            int sy = this.height - 96;
            for (int i = 0; i < suggestions.size(); i++) {
                int rowY = sy + 1 + i * SUGGEST_ROW_H;
                if (mouseX >= sx && mouseX <= sx + 220 && mouseY >= rowY && mouseY < rowY + SUGGEST_ROW_H) {
                    acceptSuggestion(suggestions.get(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX <= 160) {
            memberScroll = Math.max(0, memberScroll - (int) Math.signum(scrollY) * 3);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (aliasBox != null && aliasBox.isFocused() && selectedMember >= 0 && selectedMember < members.size()) {
            if (keyCode == 257 || keyCode == 335) {
                String alias = SealScriptLexicon.sanitizeAlias(aliasBox.getValue());
                var old = members.get(selectedMember);
                members.set(
                        selectedMember,
                        new ModNetworking.SealEditorMember(old.pos(), old.typeKey(), alias, false));
                recomputeConflicts();
                SealEditorHighlights.set(members, targetPos);
                return true;
            }
        }
        if (scriptBox != null && scriptBox.isFocused() && keyCode == 258) {
            List<String> suggestions = suggestions();
            if (!suggestions.isEmpty()) {
                acceptSuggestion(suggestions.get(0));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        SealEditorHighlights.clear();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
