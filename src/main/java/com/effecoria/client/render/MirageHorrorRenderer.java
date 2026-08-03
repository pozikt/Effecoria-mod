package com.effecoria.client.render;

import com.effecoria.client.MirageClient;
import com.effecoria.entity.MirageHorrorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Visible only to the mirage victim — others see nothing. */
public class MirageHorrorRenderer extends GeoEntityRenderer<MirageHorrorEntity> {
    public MirageHorrorRenderer(EntityRendererProvider.Context context) {
        super(context, new MirageHorrorModel());
        this.shadowRadius = 2.6f;
    }

    @Override
    public void render(
            MirageHorrorEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !entity.isBoundTo(mc.player.getUUID()) || !MirageClient.isActive()) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(1.15f, 1.15f, 1.15f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public void actuallyRender(
            PoseStack poseStack,
            MirageHorrorEntity animatable,
            BakedGeoModel model,
            net.minecraft.client.renderer.RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour) {
        // Slightly translucent silhouette.
        int tinted = (Math.round(0.88f * 255) << 24) | (colour & 0x00FFFFFF);
        super.actuallyRender(
                poseStack,
                animatable,
                model,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                tinted);
    }
}
