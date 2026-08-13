package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PhiSonarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/** Glowing Φ-sonar dish / spire above the emitter block. */
public final class PhiSonarRenderer implements BlockEntityRenderer<PhiSonarBlockEntity> {
    private static final ResourceLocation DISH =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/phi_sonar_dish.png");

    public PhiSonarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            PhiSonarBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        float time = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTick;
        boolean ready = be.ready();
        float pulse = ready ? 0.65f + 0.35f * Mth.sin(time * 0.12f) : 0.35f;
        float spin = ready ? time * 2.5f : time * 0.4f;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.05, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(DISH));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        int overlay = OverlayTexture.NO_OVERLAY;
        int light = Math.max(packedLight, 0x00F000F0);
        int a = Math.round(180 + 60 * pulse);
        int r = Math.round(80 + 100 * pulse);
        int g = Math.round(200 + 40 * pulse);
        int b = 255;

        // Dish (flat octagon-ish quad stack)
        float half = 0.55f;
        float y = 0.02f;
        quad(
                consumer,
                pose,
                mat,
                -half,
                y,
                -half,
                half,
                y,
                -half,
                half,
                y,
                half,
                -half,
                y,
                half,
                0,
                1,
                0,
                light,
                overlay,
                r,
                g,
                b,
                a);
        // Underside
        quad(
                consumer,
                pose,
                mat,
                -half,
                y - 0.04f,
                half,
                half,
                y - 0.04f,
                half,
                half,
                y - 0.04f,
                -half,
                -half,
                y - 0.04f,
                -half,
                0,
                -1,
                0,
                light,
                overlay,
                r / 2,
                g / 2,
                b / 2,
                a);

        // Spire mast
        float mast = 0.06f;
        float top = 0.55f;
        box(
                consumer,
                pose,
                mat,
                -mast,
                0f,
                -mast,
                mast,
                top,
                mast,
                light,
                overlay,
                r,
                g,
                b,
                Math.min(255, a + 20));

        // Tip glow
        float tip = 0.12f;
        box(
                consumer,
                pose,
                mat,
                -tip,
                top,
                -tip,
                tip,
                top + 0.18f,
                tip,
                light,
                overlay,
                220,
                255,
                255,
                Math.round(120 + 100 * pulse));

        poseStack.popPose();
    }

    private static void box(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int light,
            int overlay,
            int r,
            int g,
            int b,
            int a) {
        quad(consumer, pose, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, light, overlay, r, g, b, a);
        quad(consumer, pose, mat, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, light, overlay, r, g, b, a);
        quad(consumer, pose, mat, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, light, overlay, r, g, b, a);
        quad(consumer, pose, mat, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, light, overlay, r, g, b, a);
        quad(consumer, pose, mat, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, light, overlay, r, g, b, a);
        quad(consumer, pose, mat, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, light, overlay, r, g, b, a);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz,
            int light,
            int overlay,
            int r,
            int g,
            int b,
            int a) {
        vert(consumer, pose, mat, x0, y0, z0, 0f, 1f, nx, ny, nz, light, overlay, r, g, b, a);
        vert(consumer, pose, mat, x1, y1, z1, 0f, 0f, nx, ny, nz, light, overlay, r, g, b, a);
        vert(consumer, pose, mat, x2, y2, z2, 1f, 0f, nx, ny, nz, light, overlay, r, g, b, a);
        vert(consumer, pose, mat, x3, y3, z3, 1f, 1f, nx, ny, nz, light, overlay, r, g, b, a);
    }

    private static void vert(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int light,
            int overlay,
            int r,
            int g,
            int b,
            int a) {
        consumer.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(PhiSonarBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(PhiSonarBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.75, 1.5, 0.75);
    }
}
