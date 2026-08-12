package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.RegenChamberBlock;
import com.effecoria.block.RegenChamberBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * Assembled regen capsule: open-top 4×3×4 hull centered on the floor core,
 * plus a translucent Φ-fluid column that rises with fillAmount.
 */
public final class RegenChamberRenderer implements BlockEntityRenderer<RegenChamberBlockEntity> {
    private static final ResourceLocation HULL =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/regen_capsule_hull.png");
    private static final ResourceLocation FLUID =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/regen_capsule_fluid.png");

    public RegenChamberRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            RegenChamberBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (!be.getBlockState().getValue(RegenChamberBlock.FORMED) && !be.isFormed()) {
            return;
        }

        int overlay = OverlayTexture.NO_OVERLAY;

        // Hull: AABB relative to core [-1..3]×[0..3]×[-1..3] in block space.
        poseStack.pushPose();
        poseStack.translate(-1.0, 0.0, -1.0);
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer hull = buffer.getBuffer(RenderType.entityTranslucent(HULL));

        float min = 0.02f;
        float maxX = 3.98f;
        float maxY = 2.98f;
        float maxZ = 3.98f;
        // Floor
        quad(hull, pose, mat, min, min, min, maxX, min, min, maxX, min, maxZ, min, min, maxZ, 0, -1, 0, packedLight, overlay, 200);
        // Walls (open top — no +Y face)
        quad(hull, pose, mat, min, min, min, min, maxY, min, maxX, maxY, min, maxX, min, min, 0, 0, -1, packedLight, overlay, 180);
        quad(hull, pose, mat, maxX, min, maxZ, maxX, maxY, maxZ, min, maxY, maxZ, min, min, maxZ, 0, 0, 1, packedLight, overlay, 180);
        quad(hull, pose, mat, min, min, maxZ, min, maxY, maxZ, min, maxY, min, min, min, min, -1, 0, 0, packedLight, overlay, 180);
        quad(hull, pose, mat, maxX, min, min, maxX, maxY, min, maxX, maxY, maxZ, maxX, min, maxZ, 1, 0, 0, packedLight, overlay, 180);
        // Inner rim around open top
        float ix0 = 1.02f;
        float ix1 = 2.98f;
        float iz0 = 1.02f;
        float iz1 = 2.98f;
        float top = maxY;
        quad(hull, pose, mat, min, top, min, maxX, top, min, maxX, top, iz0, min, top, iz0, 0, 1, 0, packedLight, overlay, 200);
        quad(hull, pose, mat, min, top, iz1, maxX, top, iz1, maxX, top, maxZ, min, top, maxZ, 0, 1, 0, packedLight, overlay, 200);
        quad(hull, pose, mat, min, top, iz0, min, top, iz1, ix0, top, iz1, ix0, top, iz0, 0, 1, 0, packedLight, overlay, 200);
        quad(hull, pose, mat, ix1, top, iz0, ix1, top, iz1, maxX, top, iz1, maxX, top, iz0, 0, 1, 0, packedLight, overlay, 200);
        poseStack.popPose();

        float ratio = Math.max(0f, Math.min(1f, be.fillRatio()));
        if (ratio <= 0.001f) {
            return;
        }

        // Fluid column above cavity floor (local to core): [0..2]×[1..1+3r]×[0..2]
        poseStack.pushPose();
        mat = poseStack.last().pose();
        pose = poseStack.last();
        VertexConsumer fluid = buffer.getBuffer(RenderType.entityTranslucent(FLUID));
        float fy0 = 1.01f;
        float fy1 = 1.01f + 2.96f * ratio;
        float fx0 = 0.08f;
        float fx1 = 1.92f;
        float fz0 = 0.08f;
        float fz1 = 1.92f;
        int a = 140;
        // sides
        quad(fluid, pose, mat, fx0, fy0, fz0, fx0, fy1, fz0, fx1, fy1, fz0, fx1, fy0, fz0, 0, 0, -1, packedLight, overlay, a);
        quad(fluid, pose, mat, fx1, fy0, fz1, fx1, fy1, fz1, fx0, fy1, fz1, fx0, fy0, fz1, 0, 0, 1, packedLight, overlay, a);
        quad(fluid, pose, mat, fx0, fy0, fz1, fx0, fy1, fz1, fx0, fy1, fz0, fx0, fy0, fz0, -1, 0, 0, packedLight, overlay, a);
        quad(fluid, pose, mat, fx1, fy0, fz0, fx1, fy1, fz0, fx1, fy1, fz1, fx1, fy0, fz1, 1, 0, 0, packedLight, overlay, a);
        // surface
        quad(fluid, pose, mat, fx0, fy1, fz1, fx1, fy1, fz1, fx1, fy1, fz0, fx0, fy1, fz0, 0, 1, 0, packedLight, overlay, 180);
        poseStack.popPose();
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
            int alpha) {
        vert(consumer, pose, mat, x0, y0, z0, 0f, 1f, nx, ny, nz, light, overlay, alpha);
        vert(consumer, pose, mat, x1, y1, z1, 0f, 0f, nx, ny, nz, light, overlay, alpha);
        vert(consumer, pose, mat, x2, y2, z2, 1f, 0f, nx, ny, nz, light, overlay, alpha);
        vert(consumer, pose, mat, x3, y3, z3, 1f, 1f, nx, ny, nz, light, overlay, alpha);
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
            int alpha) {
        consumer.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(RegenChamberBlockEntity be) {
        return be.isFormed() || be.getBlockState().getValue(RegenChamberBlock.FORMED);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(RegenChamberBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2.5, 2.0, 2.5);
    }
}
