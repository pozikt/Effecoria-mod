package com.effecoria.client.gui;

import java.util.Optional;

import org.joml.Matrix4f;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.progression.SpellUnlockService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.magic.SpellRegistry;
import com.effecoria.network.ModNetworking;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Hold-X spell constellation — core hub with threaded spell nodes + breathing train. */
public class SpellHubScreen extends Screen {
    private SpellHubLayout.Layout layout;
    private SpellHubLayout.SpellNode hovered;
    private boolean trainHovered;

    public SpellHubScreen() {
        super(Component.translatable("gui.effecoria.hub"));
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {
        if (minecraft == null || minecraft.player == null) {
            layout = new SpellHubLayout.Layout(
                    0.7f, 24f, 14f, java.util.List.of(), new SpellHubLayout.TrainNode(-64f, 0f, 16f));
            return;
        }
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        layout = SpellHubLayout.build(data, this.width, this.height);
    }

    @Override
    public void tick() {
        rebuildLayout();
        updateHover();
    }

    private void updateHover() {
        if (minecraft == null) {
            hovered = null;
            trainHovered = false;
            return;
        }
        float dx = mouseDx();
        float dy = mouseDy();
        trainHovered = SpellHubLayout.pickTrain(layout, dx, dy).isPresent();
        hovered = trainHovered ? null : SpellHubLayout.pick(layout, dx, dy).orElse(null);
    }

    private float mouseDx() {
        double mouseX = minecraft.mouseHandler.xpos()
                * (double) minecraft.getWindow().getGuiScaledWidth()
                / (double) minecraft.getWindow().getScreenWidth();
        return (float) (mouseX - this.width / 2.0);
    }

    private float mouseDy() {
        double mouseY = minecraft.mouseHandler.ypos()
                * (double) minecraft.getWindow().getGuiScaledHeight()
                / (double) minecraft.getWindow().getScreenHeight();
        return (float) (mouseY - this.height / 2.0);
    }

    public void completeSelection() {
        if (trainHovered) {
            onClose();
            if (minecraft != null) {
                minecraft.setScreen(new BreathingTrainScreen());
            }
            return;
        }
        if (hovered != null && !hovered.locked()) {
            PacketDistributor.sendToServer(new ModNetworking.SelectSpellPayload(hovered.knownIndex()));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float dx = mouseX - this.width / 2f;
        float dy = mouseY - this.height / 2f;
        trainHovered = SpellHubLayout.pickTrain(layout, dx, dy).isPresent();
        hovered = trainHovered ? null : SpellHubLayout.pick(layout, dx, dy).orElse(null);

        int cx = this.width / 2;
        int cy = this.height / 2;
        float scale = layout.scale();

        graphics.fill(0, 0, this.width, this.height, 0x88000812);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawTrainThread(graphics, cx, cy, trainHovered);
        for (SpellHubLayout.SpellNode node : layout.nodes()) {
            drawThread(graphics, cx, cy, node, isHovered(node));
        }

        drawCore(graphics, cx, cy, layout.coreRadius(), scale);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        drawTrainNode(graphics, cx, cy, trainHovered);
        for (SpellHubLayout.SpellNode node : layout.nodes()) {
            drawSpellNode(graphics, cx, cy, node, layout.nodeRadius(), isHovered(node));
        }

        RenderSystem.disableBlend();
        renderHoverPanel(graphics, cx, cy, scale);
    }

    private void drawCore(GuiGraphics graphics, int cx, int cy, float radius, float scale) {
        int r = Math.round(radius);
        drawFilledCircle(graphics, cx, cy, r + 4, 0x5533AAFF);
        drawFilledCircle(graphics, cx, cy, r + 1, 0xFF3A4868);
        drawFilledCircle(graphics, cx, cy, r, 0xF0141828);
        graphics.drawCenteredString(this.font, "Ψ", cx, cy - 5, 0xE8F0FF);
        graphics.drawCenteredString(
                this.font, Component.translatable("gui.effecoria.hub.core"), cx, cy + 6, 0x99AABBCC);
    }

    private void drawTrainThread(GuiGraphics graphics, int cx, int cy, boolean highlight) {
        SpellHubLayout.TrainNode train = layout.trainNode();
        if (train == null) {
            return;
        }
        float dist = (float) Math.hypot(train.offsetX(), train.offsetY());
        if (dist < 1f) {
            return;
        }
        float ux = train.offsetX() / dist;
        float uy = train.offsetY() / dist;
        float x1 = cx + ux * (layout.coreRadius() + 2f);
        float y1 = cy + uy * (layout.coreRadius() + 2f);
        float x2 = cx + train.offsetX() - ux * (train.radius() + 1f);
        float y2 = cy + train.offsetY() - uy * (train.radius() + 1f);
        int color = highlight ? 0xEEFFE08A : 0xCCD4A84A;
        drawDashedLine(graphics, x1, y1, x2, y2, 2.2f, color);
    }

    private void drawTrainNode(GuiGraphics graphics, int cx, int cy, boolean highlight) {
        SpellHubLayout.TrainNode train = layout.trainNode();
        if (train == null) {
            return;
        }
        int px = cx + Math.round(train.offsetX());
        int py = cy + Math.round(train.offsetY());
        int r = Math.round(train.radius());
        drawFilledCircle(graphics, px, py, r + 3, highlight ? 0xFFFFE080 : 0xFFC9A227);
        drawFilledCircle(graphics, px, py, r, highlight ? 0xFF3A3420 : 0xFF221E12);
        graphics.drawCenteredString(this.font, "◎", px, py - 4, highlight ? 0xFFFFF0A0 : 0xFFE8C860);
    }

    private void drawThread(GuiGraphics graphics, int cx, int cy, SpellHubLayout.SpellNode node, boolean highlight) {
        float dist = (float) Math.hypot(node.offsetX(), node.offsetY());
        if (dist < 1f) {
            return;
        }
        float ux = node.offsetX() / dist;
        float uy = node.offsetY() / dist;
        float x1 = cx + ux * (layout.coreRadius() + 2f);
        float y1 = cy + uy * (layout.coreRadius() + 2f);
        float x2 = cx + node.offsetX() - ux * (layout.nodeRadius() + 1f);
        float y2 = cy + node.offsetY() - uy * (layout.nodeRadius() + 1f);
        int color = highlight
                ? (node.locked() ? 0xAA886644 : 0xCCFFE890)
                : (node.locked() ? 0x55444455 : categoryThreadColor(node.category()));
        if (node.locked()) {
            drawDashedLine(graphics, x1, y1, x2, y2, 1.4f + (highlight ? 0.4f : 0f), color);
        } else {
            drawLine(graphics, x1, y1, x2, y2, 1.5f + (highlight ? 0.5f : 0f), color);
        }
    }

    private static int categoryThreadColor(com.effecoria.core.magic.RadialCategory category) {
        return switch (category) {
            case MOVEMENT -> 0x884488CC;
            case COMBAT -> 0x88CC5555;
            case UTILITY -> 0x8855BB77;
            case SEALS -> 0x88AA77CC;
        };
    }

    private void drawSpellNode(
            GuiGraphics graphics, int cx, int cy, SpellHubLayout.SpellNode node, float nodeRadius, boolean highlight) {
        int px = cx + Math.round(node.offsetX());
        int py = cy + Math.round(node.offsetY());
        int r = Math.round(nodeRadius);
        int iconSize = Math.round(nodeRadius * 1.55f);
        if (node.locked()) {
            drawFilledCircle(graphics, px, py, r + 2, highlight ? 0xFF886644 : 0xFF4A4458);
            drawFilledCircle(graphics, px, py, r, highlight ? 0xFF1A1520 : 0xFF0C0C14);
            RenderSystem.setShaderColor(0.45f, 0.42f, 0.5f, 0.85f);
            drawSpellIcon(graphics, node.spellId(), px, py, iconSize);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            graphics.drawCenteredString(this.font, "?", px, py - 4, highlight ? 0xFFE8C080 : 0xAA9988AA);
        } else {
            drawFilledCircle(graphics, px, py, r + 2, highlight ? 0xFFFFE080 : 0xFF9090A8);
            drawFilledCircle(graphics, px, py, r, highlight ? 0xFF282838 : 0xFF141420);
            drawSpellIcon(graphics, node.spellId(), px, py, iconSize);
        }
    }

    private void renderHoverPanel(GuiGraphics graphics, int cx, int cy, float scale) {
        int panelY = cy + Math.round(118 * scale);
        if (trainHovered) {
            Component title = Component.translatable("gui.effecoria.breath_train.node");
            Component sub = Component.translatable("gui.effecoria.breath_train.node_hint");
            int panelW = Math.max(this.font.width(title), this.font.width(sub)) + 24;
            int left = cx - panelW / 2;
            graphics.fill(left - 1, panelY - 5, left + panelW + 1, panelY + 28, 0xFFC9A227);
            graphics.fill(left, panelY - 4, left + panelW, panelY + 27, 0xF01E1A10);
            graphics.drawCenteredString(this.font, title, cx, panelY, 0xFFFFE8A0);
            graphics.drawCenteredString(this.font, sub, cx, panelY + 12, 0xCCD8C878);
            return;
        }
        if (hovered != null && minecraft != null && minecraft.player != null) {
            ResourceLocation spellId = hovered.spellId();
            Component spellName = Component.translatable("spell.effecoria." + spellId.getPath());
            if (hovered.locked()) {
                renderLockedHoverPanel(graphics, cx, panelY, scale, spellId, spellName);
                return;
            }
            Component school = schoolLabel(spellId);
            Component category = Component.translatable(SpellHubLayout.categoryLabelKey(hovered.category()));
            float cost = SpellRadialCosts.previewCost(minecraft.player, spellId);
            Component costLine = formatCost(cost);
            Component desc = Component.translatable("spell.effecoria." + spellId.getPath() + ".desc");
            boolean deathMark = isDeathMarkSpell(spellId);
            Component reserveHint = deathMark
                    ? Component.translatable("gui.effecoria.hub.death_mark_reserve")
                    : null;
            int padX = 12;
            int lineH = 10;
            int lines = 4 + (deathMark ? 1 : 0);
            int panelH = lineH * lines + 18;
            int maxW = Math.max(
                    this.font.width(spellName),
                    Math.max(
                            this.font.width(school),
                            Math.max(
                                    this.font.width(category.copy().append(" · ").append(costLine)),
                                    Math.max(
                                            this.font.width(desc),
                                            reserveHint == null ? 0 : this.font.width(reserveHint)))));
            int panelW = Math.min(this.width - 24, maxW + padX * 2);
            int left = cx - panelW / 2;
            graphics.fill(left - 1, panelY - 5, left + panelW + 1, panelY + panelH - 3, 0xFF505068);
            graphics.fill(left, panelY - 4, left + panelW, panelY + panelH - 4, 0xF0181828);
            int y = panelY;
            graphics.drawCenteredString(this.font, spellName, cx, y, 0xFFFFFF);
            y += lineH + 2;
            graphics.drawCenteredString(this.font, school, cx, y, 0xCCE8E8FF);
            y += lineH + 2;
            graphics.drawCenteredString(
                    this.font,
                    Component.literal("").append(category).append(" · ").append(costLine),
                    cx,
                    y,
                    canAffordCost(cost) ? 0x99CCFF : 0xFFAA55);
            y += lineH + 2;
            graphics.drawCenteredString(this.font, desc, cx, y, 0xAABBBBCC);
            if (reserveHint != null) {
                y += lineH + 2;
                graphics.drawCenteredString(this.font, reserveHint, cx, y, 0xFFCC99AA);
            }
        } else {
            graphics.drawCenteredString(
                    this.font, Component.translatable("gui.effecoria.hub.hint"), cx, panelY, 0xAAAAAA);
        }
    }

    private static boolean isDeathMarkSpell(ResourceLocation spellId) {
        return spellId.getPath().equals("death_mark");
    }

    private void renderLockedHoverPanel(
            GuiGraphics graphics, int cx, int panelY, float scale, ResourceLocation spellId, Component spellName) {
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        SpellUnlockService.UnlockHint hint = SpellUnlockService.hintFor(data, spellId);
        Component locked = Component.translatable("gui.effecoria.hub.locked");
        Component line2;
        Component line3;
        if (!hint.nextInLine()) {
            line2 = Component.translatable("gui.effecoria.hub.locked_queue");
            line3 = Component.translatable("gui.effecoria.hub.locked_queue_hint");
        } else {
            String needBreath = BreathingService.formatTotalPercent(hint.needMastery());
            String haveBreath = BreathingService.formatTotalPercent(hint.haveMastery());
            line2 = Component.translatable(
                    "gui.effecoria.hub.locked_mastery", needBreath, haveBreath);
            if (hint.needEssence() > 0) {
                line3 = Component.translatable(
                        "gui.effecoria.hub.locked_essence", hint.needEssence(), hint.haveEssence());
            } else {
                line3 = Component.translatable("gui.effecoria.hub.locked_essence_free");
            }
        }
        int padX = 12;
        int lineH = 10;
        int panelH = lineH * 4 + 18;
        int maxW = Math.max(
                this.font.width(spellName),
                Math.max(this.font.width(locked), Math.max(this.font.width(line2), this.font.width(line3))));
        int panelW = maxW + padX * 2;
        int left = cx - panelW / 2;
        graphics.fill(left - 1, panelY - 5, left + panelW + 1, panelY + panelH - 3, 0xFF6A5538);
        graphics.fill(left, panelY - 4, left + panelW, panelY + panelH - 4, 0xF01A1410);
        int y = panelY;
        graphics.drawCenteredString(this.font, spellName, cx, y, 0xCCBBAA);
        y += lineH + 2;
        graphics.drawCenteredString(this.font, locked, cx, y, 0xFFE0A060);
        y += lineH + 2;
        graphics.drawCenteredString(
                this.font, line2, cx, y, hint.nextInLine() && hint.masteryMet() ? 0x88FFAA : 0xFFAA8888);
        y += lineH + 2;
        graphics.drawCenteredString(
                this.font, line3, cx, y, hint.nextInLine() && hint.essenceMet() ? 0x88FFAA : 0xFFAA8888);
    }

    private boolean isHovered(SpellHubLayout.SpellNode node) {
        return hovered != null && hovered.spellId().equals(node.spellId());
    }

    private Component formatCost(float cost) {
        if (CreativeGodMode.isActive(minecraft != null ? minecraft.player : null)) {
            return Component.translatable("gui.effecoria.radial.cost_free");
        }
        return Component.translatable("gui.effecoria.radial.cost", Math.max(1, Math.round(cost)));
    }

    private boolean canAffordCost(float cost) {
        return minecraft != null
                && minecraft.player != null
                && SpellRadialCosts.canAfford(minecraft.player, cost);
    }

    private static void drawSpellIcon(GuiGraphics graphics, ResourceLocation spellId, int cx, int cy, int size) {
        ResourceLocation icon = SpellIcons.forSpell(spellId);
        graphics.blitSprite(icon, cx - size / 2, cy - size / 2, size, size);
    }

    private static Component schoolLabel(ResourceLocation spellId) {
        Optional<SpellDefinition> def = SpellRegistry.get(spellId);
        if (def.isEmpty()) {
            return Component.empty();
        }
        MagicSchool school = def.get().requiredSchool();
        return Component.translatable("school.effecoria." + school.getSerializedName());
    }

    private void drawFilledCircle(GuiGraphics graphics, int cx, int cy, int radius, int argb) {
        if (radius <= 0) {
            return;
        }
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, cx, cy, 0).setColor(r, g, b, a);
        int segments = Math.max(12, radius * 2);
        for (int i = 0; i <= segments; i++) {
            float t = (float) (i / (double) segments * Math.PI * 2.0);
            buffer.addVertex(matrix, cx + (float) Math.cos(t) * radius, cy + (float) Math.sin(t) * radius, 0)
                    .setColor(r, g, b, a);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float width, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.5f) {
            return;
        }
        float nx = -dy / len * width * 0.5f;
        float ny = dx / len * width * 0.5f;
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, x1 + nx, y1 + ny, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1 - nx, y1 - ny, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2 + nx, y2 + ny, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2 - nx, y2 - ny, 0).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void drawDashedLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float width, int argb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 1f) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        float dash = 6f;
        float gap = 4f;
        float t = 0f;
        while (t < len) {
            float t2 = Math.min(len, t + dash);
            drawLine(graphics, x1 + ux * t, y1 + uy * t, x1 + ux * t2, y1 + uy * t2, width, argb);
            t += dash + gap;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
