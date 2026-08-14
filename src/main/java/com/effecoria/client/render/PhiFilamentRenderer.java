package com.effecoria.client.render;

import com.effecoria.entity.PhiFilamentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/** Thin mithril nerve between two terminals. */
public final class PhiFilamentRenderer extends EntityRenderer<PhiFilamentEntity> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("effecoria", "textures/block/mithril_block.png");

    public PhiFilamentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            PhiFilamentEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        Vec3 a = Vec3.atCenterOf(entity.endA());
        Vec3 b = Vec3.atCenterOf(entity.endB());
        Vec3 origin = entity.getPosition(partialTick);
        Vec3 relA = a.subtract(origin);
        Vec3 relB = b.subtract(origin);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX));
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        float w = 0.035f;
        int r = 180;
        int g = 220;
        int bl = 230;
        quad(consumer, pose, mat,
                (float) relA.x, (float) relA.y - w, (float) relA.z,
                (float) relA.x, (float) relA.y + w, (float) relA.z,
                (float) relB.x, (float) relB.y + w, (float) relB.z,
                (float) relB.x, (float) relB.y - w, (float) relB.z,
                r, g, bl, packedLight);
        quad(consumer, pose, mat,
                (float) relA.x - w, (float) relA.y, (float) relA.z,
                (float) relA.x + w, (float) relA.y, (float) relA.z,
                (float) relB.x + w, (float) relB.y, (float) relB.z,
                (float) relB.x - w, (float) relB.y, (float) relB.z,
                r, g, bl, packedLight);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int r, int g, int b,
            int light) {
        vertex(consumer, pose, mat, x0, y0, z0, r, g, b, 0, 0, light);
        vertex(consumer, pose, mat, x1, y1, z1, r, g, b, 0, 1, light);
        vertex(consumer, pose, mat, x2, y2, z2, r, g, b, 1, 1, light);
        vertex(consumer, pose, mat, x3, y3, z3, r, g, b, 1, 0, light);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Matrix4f mat,
            float x, float y, float z,
            int r, int g, int b,
            float u, float v,
            int light) {
        consumer.addVertex(mat, x, y, z)
                .setColor(r, g, b, 230)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(PhiFilamentEntity entity) {
        return TEX;
    }
}
