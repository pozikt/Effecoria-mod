package com.effecoria.client.gui;

import java.util.Optional;

import org.joml.Matrix4f;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
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

/** Hold-X radial spell picker. Non-pausing overlay. */
public class SpellRadialScreen extends Screen {
    private SpellRadialLayout.Layout layout;
    private SpellRadialLayout.Sector hovered;
    private static final int ICON_DRAW_SIZE = 26;

    public SpellRadialScreen() {
        super(Component.translatable("gui.effecoria.radial"));
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {
        if (minecraft == null || minecraft.player == null) {
            layout = new SpellRadialLayout.Layout(SpellRadialLayout.DEAD_ZONE, java.util.List.of());
            return;
        }
        PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
        layout = SpellRadialLayout.build(data);
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
        double mouseX = minecraft.mouseHandler.xpos()
                * (double) minecraft.getWindow().getGuiScaledWidth()
                / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos()
                * (double) minecraft.getWindow().getGuiScaledHeight()
                / (double) minecraft.getWindow().getScreenHeight();
        float dx = (float) (mouseX - this.width / 2.0);
        float dy = (float) (mouseY - this.height / 2.0);
        hovered = SpellRadialLayout.pick(layout, dx, dy).orElse(null);
    }

    /** Called when X is released while this screen is open. */
    public void completeSelection() {
        if (hovered != null) {
            PacketDistributor.sendToServer(new ModNetworking.SelectSpellPayload(hovered.knownIndex()));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float dx = mouseX - this.width / 2f;
        float dy = mouseY - this.height / 2f;
        hovered = SpellRadialLayout.pick(layout, dx, dy).orElse(null);

        int cx = this.width / 2;
        int cy = this.height / 2;

        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        for (SpellRadialLayout.Ring ring : layout.rings()) {
            for (SpellRadialLayout.Sector sector : ring.sectors()) {
                boolean highlight = hovered != null
                        && hovered.spellId().equals(sector.spellId())
                        && hovered.innerRadius() == sector.innerRadius();
                int color = highlight ? brighten(ring.baseColorArgb()) : ring.baseColorArgb();
                drawAnnularSector(
                        graphics,
                        cx,
                        cy,
                        sector.innerRadius(),
                        sector.outerRadius(),
                        sector.startAngleRad(),
                        sector.endAngleRad(),
                        color);
            }
        }

        // Center dead-zone disc
        drawDisk(graphics, cx, cy, layout.deadZone(), 0xAA1A1A1A);

        for (SpellRadialLayout.Ring ring : layout.rings()) {
            for (SpellRadialLayout.Sector sector : ring.sectors()) {
                float mid = sector.midAngleRad();
                float r = (sector.innerRadius() + sector.outerRadius()) * 0.5f;
                int lx = cx + Math.round((float) Math.cos(mid) * r);
                int ly = cy + Math.round((float) Math.sin(mid) * r);
                ResourceLocation icon = SpellIcons.forSpell(sector.spellId());
                int iconX = lx - ICON_DRAW_SIZE / 2;
                int iconY = ly - ICON_DRAW_SIZE / 2;
                graphics.blit(
                        icon,
                        iconX,
                        iconY,
                        0,
                        0,
                        ICON_DRAW_SIZE,
                        ICON_DRAW_SIZE,
                        SpellIcons.TEXTURE_SIZE,
                        SpellIcons.TEXTURE_SIZE);
            }
        }

        graphics.drawCenteredString(this.font, this.title, cx, cy - 8, 0xFFFFFF);

        if (hovered != null) {
            Component spellName = Component.translatable("spell.effecoria." + hovered.spellId().getPath());
            Component schoolLabel = schoolLabel(hovered.spellId());
            String ringLabel = ringLabelFor(hovered);
            graphics.drawCenteredString(this.font, spellName, cx, this.height - 40, 0xFFFFFF);
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(ringLabel).append(" · ").append(schoolLabel),
                    cx,
                    this.height - 28,
                    0xAAAAAA);
        } else {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("gui.effecoria.radial.hint"),
                    cx,
                    this.height - 28,
                    0xAAAAAA);
        }
    }

    private String ringLabelFor(SpellRadialLayout.Sector sector) {
        for (SpellRadialLayout.Ring ring : layout.rings()) {
            if (sector.innerRadius() == ring.innerRadius()) {
                return Component.translatable(ring.labelKey()).getString();
            }
        }
        return "";
    }

    private static Component schoolLabel(ResourceLocation spellId) {
        Optional<SpellDefinition> def = SpellRegistry.get(spellId);
        if (def.isEmpty()) {
            return Component.empty();
        }
        MagicSchool school = def.get().requiredSchool();
        return Component.translatable("school.effecoria." + school.getSerializedName());
    }

    private static int brighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + 40);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + 40);
        int b = Math.min(255, (argb & 0xFF) + 40);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawDisk(GuiGraphics graphics, int cx, int cy, float radius, int argb) {
        drawAnnularSector(graphics, cx, cy, 0f, radius, 0f, (float) (Math.PI * 2.0), argb);
    }

    private void drawAnnularSector(
            GuiGraphics graphics,
            int cx,
            int cy,
            float inner,
            float outer,
            float startAngle,
            float endAngle,
            int argb) {
        float span = endAngle - startAngle;
        if (span <= 0f) {
            span += (float) (Math.PI * 2.0);
        }
        int segments = Math.max(4, Math.round(span / 0.08f));

        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float t = startAngle + span * (i / (float) segments);
            float cos = (float) Math.cos(t);
            float sin = (float) Math.sin(t);
            buffer.addVertex(matrix, cx + cos * outer, cy + sin * outer, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + cos * inner, cy + sin * inner, 0).setColor(r, g, b, a);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
