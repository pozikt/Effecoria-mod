package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.ForgeReactorBlock;
import com.effecoria.block.ForgeReactorBlockEntity;
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
 * Assembled Forge as one solid 3×4×3 prism.
 * Side faces use a 3:4 hull sheet; top/bottom use a square roof sheet (Heart-style megablock).
 */
public final class ForgeReactorRenderer implements BlockEntityRenderer<ForgeReactorBlockEntity> {
    private static final ResourceLocation HULL_SIDE =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/forge_reactor_hull.png");
    private static final ResourceLocation HULL_SIDE_ON =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/forge_reactor_hull_on.png");
    private static final ResourceLocation HULL_TOP =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/forge_reactor_hull_top.png");
    private static final ResourceLocation HULL_TOP_ON =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/forge_reactor_hull_top_on.png");

    public ForgeReactorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            ForgeReactorBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (!be.getBlockState().getValue(ForgeReactorBlock.FORMED) && !be.isFormed()) {
            return;
        }
        boolean lit = be.getBlockState().getValue(ForgeReactorBlock.LIT) || be.supplying();

        poseStack.pushPose();
        // Core at (0,0,0); hull spans x/z [-1,2], y [-1,3]
        poseStack.translate(-1.0, -1.0, -1.0);
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        float min = 0.001f;
        float maxX = 2.999f;
        float maxY = 3.999f;
        float maxZ = 2.999f;
        int overlay = OverlayTexture.NO_OVERLAY;

        // One buffer at a time — getting another RenderType ends the previous builder.
        VertexConsumer tops = buffer.getBuffer(RenderType.entitySolid(lit ? HULL_TOP_ON : HULL_TOP));
        quad(tops, pose, mat, min, min, min, maxX, min, min, maxX, min, maxZ, min, min, maxZ, 0, -1, 0, packedLight, overlay);
        quad(tops, pose, mat, min, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, min, min, maxY, min, 0, 1, 0, packedLight, overlay);

        VertexConsumer sides = buffer.getBuffer(RenderType.entitySolid(lit ? HULL_SIDE_ON : HULL_SIDE));
        quad(sides, pose, mat, min, min, min, min, maxY, min, maxX, maxY, min, maxX, min, min, 0, 0, -1, packedLight, overlay);
        quad(sides, pose, mat, maxX, min, maxZ, maxX, maxY, maxZ, min, maxY, maxZ, min, min, maxZ, 0, 0, 1, packedLight, overlay);
        quad(sides, pose, mat, min, min, maxZ, min, maxY, maxZ, min, maxY, min, min, min, min, -1, 0, 0, packedLight, overlay);
        quad(sides, pose, mat, maxX, min, min, maxX, maxY, min, maxX, maxY, maxZ, maxX, min, maxZ, 1, 0, 0, packedLight, overlay);

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
    public boolean shouldRenderOffScreen(ForgeReactorBlockEntity be) {
        return be.isFormed() || be.getBlockState().getValue(ForgeReactorBlock.FORMED);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(ForgeReactorBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.2).expandTowards(0, 2, 0);
    }
}
