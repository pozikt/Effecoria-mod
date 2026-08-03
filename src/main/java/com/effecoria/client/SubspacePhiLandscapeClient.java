package com.effecoria.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.ModDimensions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Φ-vision hyperspace skybox: ultramarine ocean, star hills, planet motes,
 * black-hole whirlpools, TSE knots, and Ω glitch snow.
 *
 * <p>Rendered as a classic skybox (camera rotation only, no player translation) so turning
 * the head looks around a fixed celestial sphere; animation is time-driven only.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class SubspacePhiLandscapeClient {
    private static final List<Landmark> LANDMARKS = new ArrayList<>();
    private static boolean seeded;

    private SubspacePhiLandscapeClient() {}

    private enum Kind {
        STAR_HILL,
        PLANET,
        BLACK_HOLE,
        TSE,
        OMEGA
    }

    private record Landmark(Kind kind, float yaw, float pitch, float scale, float hue) {}

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ModDimensions.isSubspace(mc.level)) {
            return;
        }
        event.setNearPlaneDistance(8f);
        event.setFarPlaneDistance(96f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ModDimensions.isSubspace(mc.level)) {
            return;
        }
        float pulse = 0.5f + 0.5f * Mth.sin((mc.level.getGameTime() + (float) event.getPartialTick()) * 0.012f);
        event.setRed(0.02f + 0.01f * pulse);
        event.setGreen(0.05f + 0.02f * pulse);
        event.setBlue(0.24f + 0.05f * pulse);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ModDimensions.isSubspace(mc.level)) {
            return;
        }
        ensureSeeded();

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = mc.level.getGameTime() + partial;
        Camera camera = event.getCamera();
        PoseStack pose = event.getPoseStack();

        pose.pushPose();
        // Infinite skybox frame: drop world translation, keep only look orientation.
        pose.last().pose().identity();
        pose.last().normal().identity();
        pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        pose.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0f));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        drawSkyDome(pose, time);
        for (Landmark landmark : LANDMARKS) {
            drawLandmark(pose, landmark, time);
        }
        drawPhiCurrents(pose, time);
        drawOmegaSnow(pose, time);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void ensureSeeded() {
        if (seeded) {
            return;
        }
        seeded = true;
        Random random = new Random(0x0F1E55EAL);
        for (int i = 0; i < 14; i++) {
            LANDMARKS.add(new Landmark(
                    Kind.STAR_HILL,
                    random.nextFloat() * Mth.TWO_PI,
                    (random.nextFloat() - 0.12f) * 0.5f,
                    14f + random.nextFloat() * 28f,
                    random.nextFloat()));
        }
        for (int i = 0; i < 10; i++) {
            LANDMARKS.add(new Landmark(
                    Kind.PLANET,
                    random.nextFloat() * Mth.TWO_PI,
                    (random.nextFloat() - 0.2f) * 0.35f,
                    2.5f + random.nextFloat() * 4.5f,
                    0.55f + random.nextFloat() * 0.35f));
        }
        LANDMARKS.add(new Landmark(Kind.BLACK_HOLE, 1.1f, -0.05f, 9f, 0.75f));
        LANDMARKS.add(new Landmark(Kind.BLACK_HOLE, 4.2f, 0.12f, 6.5f, 0.8f));
        LANDMARKS.add(new Landmark(Kind.TSE, 2.6f, 0.08f, 8f, 0f));
        LANDMARKS.add(new Landmark(Kind.TSE, 5.4f, -0.1f, 6f, 0f));
        for (int i = 0; i < 6; i++) {
            LANDMARKS.add(new Landmark(
                    Kind.OMEGA,
                    random.nextFloat() * Mth.TWO_PI,
                    (random.nextFloat() - 0.5f) * 0.65f,
                    3f + random.nextFloat() * 4f,
                    random.nextFloat()));
        }
    }

    /** Slow-breathing ultramarine dome — the compressed Φ ocean itself. */
    private static void drawSkyDome(PoseStack pose, float time) {
        float breath = 0.5f + 0.5f * Mth.sin(time * 0.018f);
        for (int band = 0; band < 8; band++) {
            float t = band / 7f;
            float y0 = -40f + t * 100f;
            float y1 = y0 + 16f;
            float radius = 110f + Mth.sin(time * 0.01f + band) * 4f;
            float a = 0.12f + 0.06f * (1f - t) + 0.03f * breath;
            float r = 0.02f + 0.02f * t;
            float g = 0.05f + 0.04f * t;
            float b = 0.18f + 0.22f * t;
            drawRing(pose, radius, y0, y1, r, g, b, a, 24);
        }
    }

    /** Distant Φ currents drifting across the celestial sphere (not player-locked). */
    private static void drawPhiCurrents(PoseStack pose, float time) {
        for (int i = 0; i < 5; i++) {
            float drift = time * 0.004f + i * 1.25f;
            float yaw = drift;
            float pitch = -0.15f + 0.08f * Mth.sin(drift * 0.7f + i);
            float dist = 95f;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotation(yaw));
            pose.mulPose(Axis.XP.rotation(-pitch));
            pose.translate(0, 0, -dist);
            float w = 28f + 8f * Mth.sin(drift);
            float a = 0.05f + 0.03f * Mth.sin(drift * 1.3f);
            drawBillboardQuad(pose, w, -2f, 3f, 0.08f, 0.2f, 0.65f, a);
            pose.popPose();
        }
    }

    private static void drawLandmark(PoseStack pose, Landmark landmark, float time) {
        float dist = 100f;
        pose.pushPose();
        // World-fixed spherical placement on the skybox.
        pose.mulPose(Axis.YP.rotation(landmark.yaw()));
        pose.mulPose(Axis.XP.rotation(-landmark.pitch()));
        pose.translate(0, 0, -dist);
        switch (landmark.kind()) {
            case STAR_HILL -> drawStarHill(pose, landmark, time);
            case PLANET -> drawPlanet(pose, landmark, time);
            case BLACK_HOLE -> drawBlackHole(pose, landmark, time);
            case TSE -> drawTseKnot(pose, landmark, time);
            case OMEGA -> drawOmegaRift(pose, landmark, time);
        }
        pose.popPose();
    }

    private static void drawStarHill(PoseStack pose, Landmark landmark, float time) {
        float h = landmark.scale();
        float warm = landmark.hue();
        float r = 0.75f + 0.25f * warm;
        float g = 0.55f + 0.35f * (1f - warm * 0.4f);
        float b = 0.35f + 0.55f * (1f - warm);
        float pulse = 0.75f + 0.25f * Mth.sin(time * 0.035f + landmark.yaw());
        for (int layer = 0; layer < 5; layer++) {
            float t = layer / 4f;
            float radius = h * (0.55f - t * 0.45f);
            float y0 = t * h * 0.85f - h * 0.2f;
            float y1 = y0 + h * 0.22f;
            float a = (0.2f + 0.12f * (1f - t)) * pulse;
            drawBillboardQuad(pose, radius, y0, y1, r, g, b, a);
        }
        drawBillboardQuad(pose, h * 0.08f, h * 0.55f, h * 0.85f, 1f, 1f, 0.95f, 0.6f * pulse);
    }

    private static void drawPlanet(PoseStack pose, Landmark landmark, float time) {
        float s = landmark.scale();
        float glow = 0.5f + 0.2f * Mth.sin(time * 0.045f + landmark.yaw());
        float blue = 0.45f + landmark.hue() * 0.45f;
        drawBillboardQuad(pose, s, -s * 0.2f, s * 0.55f, 0.25f, 0.45f, blue, 0.38f * glow);
        drawBillboardQuad(pose, s * 0.45f, 0f, s * 0.3f, 0.4f, 0.7f, 1f, 0.6f * glow);
    }

    private static void drawBlackHole(PoseStack pose, Landmark landmark, float time) {
        float s = landmark.scale();
        // Soft accretion glow (violet) — rings, not sticks.
        float spin = time * 0.05f;
        for (int ring = 0; ring < 4; ring++) {
            float t = ring / 3f;
            float radius = s * (0.7f + t * 0.85f);
            float a = 0.22f * (1f - t * 0.55f);
            drawDisc(pose, radius, radius * 1.12f, 0.45f, 0.12f, 0.9f, a, 28, spin + ring * 0.4f);
        }
        // Absolute black core — circular, not a rectangle.
        drawFilledDisc(pose, s * 0.55f, 0f, 0f, 0f, 0.98f, 24);
        // Inner violet rim
        drawDisc(pose, s * 0.52f, s * 0.62f, 0.55f, 0.15f, 0.95f, 0.45f, 24, spin * 1.3f);
    }

    /** Annulus in the local XY plane (faces the viewer after skybox placement). */
    private static void drawDisc(
            PoseStack pose,
            float inner,
            float outer,
            float r,
            float g,
            float b,
            float a,
            int segments,
            float phase) {
        var matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float a0 = phase + (i / (float) segments) * Mth.TWO_PI;
            float a1 = phase + ((i + 1) / (float) segments) * Mth.TWO_PI;
            // Slight spiral thickness modulation.
            float wobble = 1f + 0.08f * Mth.sin(a0 * 3f);
            float i0 = inner * wobble;
            float o0 = outer * wobble;
            float x0i = Mth.cos(a0) * i0;
            float y0i = Mth.sin(a0) * i0;
            float x1i = Mth.cos(a1) * i0;
            float y1i = Mth.sin(a1) * i0;
            float x0o = Mth.cos(a0) * o0;
            float y0o = Mth.sin(a0) * o0;
            float x1o = Mth.cos(a1) * o0;
            float y1o = Mth.sin(a1) * o0;
            buffer.addVertex(matrix, x0i, y0i, 0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x0o, y0o, 0).setColor(r, g, b, a * 0.55f);
            buffer.addVertex(matrix, x1o, y1o, 0).setColor(r, g, b, a * 0.55f);
            buffer.addVertex(matrix, x1i, y1i, 0).setColor(r, g, b, a);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawFilledDisc(PoseStack pose, float radius, float r, float g, float b, float a, int segments) {
        var matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, a);
        for (int i = 0; i <= segments; i++) {
            float ang = (i / (float) segments) * Mth.TWO_PI;
            buffer.addVertex(matrix, Mth.cos(ang) * radius, Mth.sin(ang) * radius, 0).setColor(r, g, b, a);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawTseKnot(PoseStack pose, Landmark landmark, float time) {
        float s = landmark.scale();
        // Slow morph — still skybox-anchored, not look-anchored.
        Random jitter = new Random((long) (landmark.yaw() * 1000) ^ ((long) (time / 8)));
        for (int i = 0; i < 8; i++) {
            float ox = (jitter.nextFloat() - 0.5f) * s;
            float oy = (jitter.nextFloat() - 0.5f) * s * 0.7f;
            float oz = (jitter.nextFloat() - 0.5f) * s * 0.3f;
            pose.pushPose();
            pose.translate(ox, oy, oz);
            float frag = s * (0.12f + jitter.nextFloat() * 0.2f);
            drawBillboardQuad(pose, frag, -frag * 0.3f, frag * 0.5f, 0.02f, 0.01f, 0.04f, 0.8f);
            pose.popPose();
        }
    }

    private static void drawOmegaRift(PoseStack pose, Landmark landmark, float time) {
        // Flicker in time only — position stays fixed on the sphere.
        if (((int) (time / 2) + (int) (landmark.yaw() * 40)) % 5 == 0) {
            return;
        }
        float s = landmark.scale();
        Random random = new Random((long) (landmark.yaw() * 997) ^ 0x0E6A55L);
        for (int i = 0; i < 10; i++) {
            float ox = (random.nextFloat() - 0.5f) * s * 2f;
            float oy = (random.nextFloat() - 0.5f) * s;
            pose.pushPose();
            pose.translate(ox, oy, (random.nextFloat() - 0.5f) * s * 0.2f);
            float w = 0.35f + random.nextFloat() * 1.1f;
            float cold = 0.55f + random.nextFloat() * 0.35f;
            float flicker = 0.35f + 0.35f * Mth.sin(time * 0.4f + i);
            drawBillboardQuad(pose, w, 0f, w * 0.8f, cold, cold, 1f, flicker);
            pose.popPose();
        }
    }

    private static void drawOmegaSnow(PoseStack pose, float time) {
        // Fixed flake directions on the skybox; only opacity animates.
        Random random = new Random(0x51E55EAL);
        for (int i = 0; i < 22; i++) {
            float yaw = random.nextFloat() * Mth.TWO_PI;
            float pitch = (random.nextFloat() - 0.5f) * 1.1f;
            float blink = 0.2f + 0.5f * Math.max(0f, Mth.sin(time * 0.25f + i * 1.7f));
            if (blink < 0.25f) {
                continue;
            }
            pose.pushPose();
            pose.mulPose(Axis.YP.rotation(yaw));
            pose.mulPose(Axis.XP.rotation(-pitch));
            pose.translate(0, 0, -88f);
            float w = 0.3f + random.nextFloat() * 0.8f;
            drawBillboardQuad(pose, w, 0f, w, 0.7f, 0.78f, 1f, blink * 0.55f);
            pose.popPose();
        }
    }

    private static void drawRing(
            PoseStack pose, float radius, float y0, float y1, float r, float g, float b, float a, int segments) {
        var matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float a0 = (i / (float) segments) * Mth.TWO_PI;
            float a1 = ((i + 1) / (float) segments) * Mth.TWO_PI;
            float x0 = Mth.sin(a0) * radius;
            float z0 = Mth.cos(a0) * radius;
            float x1 = Mth.sin(a1) * radius;
            float z1 = Mth.cos(a1) * radius;
            buffer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y0, z1).setColor(r, g, b, a);
            buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a * 0.75f);
            buffer.addVertex(matrix, x0, y1, z0).setColor(r, g, b, a * 0.75f);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void drawBillboardQuad(
            PoseStack pose, float halfWidth, float y0, float y1, float r, float g, float b, float a) {
        var matrix = pose.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float w = halfWidth;
        buffer.addVertex(matrix, -w, y0, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, w, y0, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, w, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, -w, y1, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, 0, y0, -w).setColor(r, g, b, a * 0.85f);
        buffer.addVertex(matrix, 0, y0, w).setColor(r, g, b, a * 0.85f);
        buffer.addVertex(matrix, 0, y1, w).setColor(r, g, b, a * 0.85f);
        buffer.addVertex(matrix, 0, y1, -w).setColor(r, g, b, a * 0.85f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
