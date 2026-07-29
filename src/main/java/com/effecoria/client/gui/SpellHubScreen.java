package com.effecoria.client.gui;

import java.util.Optional;

import org.joml.Matrix4f;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.SpellDefinition;
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

/** Hold-X spell constellation — core hub with threaded spell nodes. */
public class SpellHubScreen extends Screen {
    private SpellHubLayout.Layout layout;
    private SpellHubLayout.SpellNode hovered;

    public SpellHubScreen() {
        super(Component.translatable("gui.effecoria.hub"));
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {
        if (minecraft == null || minecraft.player == null) {
            layout = new SpellHubLayout.Layout(0.7f, 24f, 14f, java.util.List.of());
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
            return;
        }
        hovered = pickAtMouse().orElse(null);
    }

    private Optional<SpellHubLayout.SpellNode> pickAtMouse() {
        double mouseX = minecraft.mouseHandler.xpos()
                * (double) minecraft.getWindow().getGuiScaledWidth()
                / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos()
                * (double) minecraft.getWindow().getGuiScaledHeight()
                / (double) minecraft.getWindow().getScreenHeight();
        float dx = (float) (mouseX - this.width / 2.0);
        float dy = (float) (mouseY - this.height / 2.0);
        return SpellHubLayout.pick(layout, dx, dy);
    }

    public void completeSelection() {
        if (hovered != null) {
            PacketDistributor.sendToServer(new ModNetworking.SelectSpellPayload(hovered.knownIndex()));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hovered = pickAtMouse().orElse(null);

        int cx = this.width / 2;
        int cy = this.height / 2;
        float scale = layout.scale();

        graphics.fill(0, 0, this.width, this.height, 0x88000812);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (SpellHubLayout.SpellNode node : layout.nodes()) {
            drawThread(graphics, cx, cy, node, isHovered(node));
        }

        drawCore(graphics, cx, cy, layout.coreRadius(), scale);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        for (SpellHubLayout.SpellNode node : layout.nodes()) {
            drawSpellNode(graphics, cx, cy, node, layout.nodeRadius(), isHovered(node));
        }

        RenderSystem.disableBlend();
        renderHoverPanel(graphics, cx, cy, scale);
    }

    private void drawCore(GuiGraphics graphics, int cx, int cy, float radius, float scale) {
        int r = Math.round(radius);
        int outer = 0xFF3A4868;
        int fill = 0xF0141828;
        int glow = 0x5533AAFF;

        drawFilledCircle(graphics, cx, cy, r + 4, glow);
        drawFilledCircle(graphics, cx, cy, r + 1, outer);
        drawFilledCircle(graphics, cx, cy, r, fill);

        String coreMark = "Ψ";
        graphics.drawCenteredString(this.font, coreMark, cx, cy - 5, 0xE8F0FF);
        Component coreLabel = Component.translatable("gui.effecoria.hub.core");
        graphics.drawCenteredString(this.font, coreLabel, cx, cy + 6, 0x99AABBCC);
    }

    private void drawThread(GuiGraphics graphics, int cx, int cy, SpellHubLayout.SpellNode node, boolean highlight) {
        float nx = cx + node.offsetX();
        float ny = cy + node.offsetY();
        float dist = (float) Math.hypot(node.offsetX(), node.offsetY());
        if (dist < 1f) {
            return;
        }
        float ux = node.offsetX() / dist;
        float uy = node.offsetY() / dist;
        float coreEdge = layout.coreRadius() + 2f;
        float nodeEdge = layout.nodeRadius() + 1f;
        float x1 = cx + ux * coreEdge;
        float y1 = cy + uy * coreEdge;
        float x2 = cx + node.offsetX() - ux * nodeEdge;
        float y2 = cy + node.offsetY() - uy * nodeEdge;

        int color = highlight ? 0xCCFFE890 : categoryThreadColor(node.category());
        drawLine(graphics, x1, y1, x2, y2, 1.5f + (highlight ? 0.5f : 0f), color);
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

        int border = highlight ? 0xFFFFE080 : 0xFF9090A8;
        int fill = highlight ? 0xFF282838 : 0xFF141420;
        drawFilledCircle(graphics, px, py, r + 2, border);
        drawFilledCircle(graphics, px, py, r, fill);

        drawSpellIcon(graphics, node.spellId(), px, py, iconSize);
    }

    private void renderHoverPanel(GuiGraphics graphics, int cx, int cy, float scale) {
        int panelY = cy + Math.round(118 * scale);
        if (hovered != null && minecraft != null && minecraft.player != null) {
            ResourceLocation spellId = hovered.spellId();
            Component spellName = Component.translatable("spell.effecoria." + spellId.getPath());
            Component school = schoolLabel(spellId);
            Component category = Component.translatable(SpellHubLayout.categoryLabelKey(hovered.category()));
            float cost = SpellRadialCosts.previewCost(minecraft.player, spellId);
            Component costLine = formatCost(cost);

            int padX = 12;
            int lineH = 10;
            int panelH = lineH * 3 + 16;
            int maxW = Math.max(
                    this.font.width(spellName),
                    Math.max(this.font.width(school), this.font.width(category.copy().append(" · ").append(costLine))));
            int panelW = maxW + padX * 2;
            int left = cx - panelW / 2;
            int top = panelY - 4;

            graphics.fill(left - 1, top - 1, left + panelW + 1, top + panelH + 1, 0xFF505068);
            graphics.fill(left, top, left + panelW, top + panelH, 0xF0181828);

            int y = top + 6;
            graphics.drawCenteredString(this.font, spellName, cx, y, 0xFFFFFF);
            y += lineH + 2;
            graphics.drawCenteredString(this.font, school, cx, y, 0xCCE8E8FF);
            y += lineH + 2;
            graphics.drawCenteredString(
                    this.font,
                    Component.literal("").append(category).append(" · ").append(costLine),
                    cx,
                    y,
                    canAffordCost(cost) ? 0x99CCFF : 0xFF8888);
        } else {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.effecoria.hub.hint"),
                    cx,
                    panelY,
                    0xAAAAAA);
        }
    }

    private boolean isHovered(SpellHubLayout.SpellNode node) {
        return hovered != null && hovered.spellId().equals(node.spellId());
    }

    private Component formatCost(float cost) {
        if (CreativeGodMode.isActive(minecraft != null ? minecraft.player : null)) {
            return Component.translatable("gui.effecoria.radial.cost_free");
        }
        int rounded = Math.max(1, Math.round(cost));
        return Component.translatable("gui.effecoria.radial.cost", rounded);
    }

    private boolean canAffordCost(float cost) {
        return minecraft != null
                && minecraft.player != null
                && SpellRadialCosts.canAfford(minecraft.player, cost);
    }

    private static void drawSpellIcon(GuiGraphics graphics, ResourceLocation spellId, int cx, int cy, int size) {
        ResourceLocation icon = SpellIcons.forSpell(spellId);
        int x = cx - size / 2;
        int y = cy - size / 2;
        graphics.blitSprite(icon, x, y, size, size);
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
