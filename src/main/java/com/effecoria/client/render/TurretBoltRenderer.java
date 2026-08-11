package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.TurretBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;

/** Elongated hypervelocity Φ-bolt. */
public final class TurretBoltRenderer extends EntityRenderer<TurretBoltEntity> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "textures/block/turret_bolt.png");

    public TurretBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            TurretBoltEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        boolean omega = entity.isOmega();
        int r = omega ? 180 : 200;
        int g = omega ? 80 : 230;
        int b = omega ? 220 : 255;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEX));
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        float half = 0.06f;
        float len = 1.35f;
        // Two crossed billboards along +X (after -90 Y from flight yaw)
        quad(consumer, pose, mat, 0, -half, 0, 0, half, 0, len, half, 0, len, -half, 0, r, g, b, packedLight);
        quad(consumer, pose, mat, 0, 0, -half, 0, 0, half, len, 0, half, len, 0, -half, r, g, b, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
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
            int r,
            int g,
            int b,
            int light) {
        consumer.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, 255)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
        consumer.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, 255)
                .setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
        consumer.addVertex(mat, x2, y2, z2)
                .setColor(255, 255, 255, 255)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
        consumer.addVertex(mat, x3, y3, z3)
                .setColor(255, 255, 255, 255)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(TurretBoltEntity entity) {
        return TEX;
    }
}
