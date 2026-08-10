package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.HeartReactorBlock;
import com.effecoria.block.HeartReactorBlockEntity;
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

/** Draws the assembled Heart as one textured 3×3×3 cube centered on the core. */
public final class HeartReactorRenderer implements BlockEntityRenderer<HeartReactorBlockEntity> {
    private static final ResourceLocation HULL =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/heart_reactor_hull.png");
    private static final ResourceLocation HULL_ON =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/heart_reactor_hull_on.png");

    public HeartReactorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            HeartReactorBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (!be.getBlockState().getValue(HeartReactorBlock.FORMED) && !be.isFormed()) {
            return;
        }

        boolean lit = be.getBlockState().getValue(HeartReactorBlock.LIT) || be.supplying();
        ResourceLocation tex = lit ? HULL_ON : HULL;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entitySolid(tex));

        poseStack.pushPose();
        // Core is the center cell; hull spans [-1, 2] in block space.
        poseStack.translate(-1.0, -1.0, -1.0);
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();

        float min = 0.001f;
        float max = 2.999f;
        int overlay = OverlayTexture.NO_OVERLAY;

        // -Y
        quad(consumer, pose, mat, min, min, min, max, min, min, max, min, max, min, min, max, 0, -1, 0, packedLight, overlay);
        // +Y
        quad(consumer, pose, mat, min, max, max, max, max, max, max, max, min, min, max, min, 0, 1, 0, packedLight, overlay);
        // -Z
        quad(consumer, pose, mat, min, min, min, min, max, min, max, max, min, max, min, min, 0, 0, -1, packedLight, overlay);
        // +Z
        quad(consumer, pose, mat, max, min, max, max, max, max, min, max, max, min, min, max, 0, 0, 1, packedLight, overlay);
        // -X
        quad(consumer, pose, mat, min, min, max, min, max, max, min, max, min, min, min, min, -1, 0, 0, packedLight, overlay);
        // +X
        quad(consumer, pose, mat, max, min, min, max, max, min, max, max, max, max, min, max, 1, 0, 0, packedLight, overlay);

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
            int overlay) {
        vert(consumer, pose, mat, x0, y0, z0, 0f, 1f, nx, ny, nz, light, overlay);
        vert(consumer, pose, mat, x1, y1, z1, 0f, 0f, nx, ny, nz, light, overlay);
        vert(consumer, pose, mat, x2, y2, z2, 1f, 0f, nx, ny, nz, light, overlay);
        vert(consumer, pose, mat, x3, y3, z3, 1f, 1f, nx, ny, nz, light, overlay);
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
            int overlay) {
        consumer.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(HeartReactorBlockEntity be) {
        return be.isFormed() || be.getBlockState().getValue(HeartReactorBlock.FORMED);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(HeartReactorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.05);
    }
}
