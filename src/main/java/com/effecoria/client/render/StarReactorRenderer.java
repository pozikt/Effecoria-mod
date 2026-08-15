package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.StarReactorBlock;
import com.effecoria.block.StarReactorBlockEntity;
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

/** Draws the assembled Star Reactor as one textured 5×5×5 cube centered on the core. */
public final class StarReactorRenderer implements BlockEntityRenderer<StarReactorBlockEntity> {
    private static final ResourceLocation HULL =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/star_reactor_hull.png");
    private static final ResourceLocation HULL_ON =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/star_reactor_hull_on.png");

    public StarReactorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            StarReactorBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (!be.getBlockState().getValue(StarReactorBlock.FORMED) && !be.isFormed()) {
            return;
        }

        boolean lit = be.getBlockState().getValue(StarReactorBlock.LIT) || be.supplying();
        ResourceLocation tex = lit ? HULL_ON : HULL;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entitySolid(tex));

        poseStack.pushPose();
        poseStack.translate(-2.0, -2.0, -2.0);
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();

        float min = 0.001f;
        float max = 4.999f;
        int overlay = OverlayTexture.NO_OVERLAY;

        quad(consumer, pose, mat, min, min, min, max, min, min, max, min, max, min, min, max, 0, -1, 0, packedLight, overlay);
        quad(consumer, pose, mat, min, max, max, max, max, max, max, max, min, min, max, min, 0, 1, 0, packedLight, overlay);
        quad(consumer, pose, mat, min, min, min, min, max, min, max, max, min, max, min, min, 0, 0, -1, packedLight, overlay);
        quad(consumer, pose, mat, max, min, max, max, max, max, min, max, max, min, min, max, 0, 0, 1, packedLight, overlay);
        quad(consumer, pose, mat, min, min, max, min, max, max, min, max, min, min, min, min, -1, 0, 0, packedLight, overlay);
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
    public boolean shouldRenderOffScreen(StarReactorBlockEntity be) {
        return be.isFormed() || be.getBlockState().getValue(StarReactorBlock.FORMED);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(StarReactorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2.05);
    }
}
