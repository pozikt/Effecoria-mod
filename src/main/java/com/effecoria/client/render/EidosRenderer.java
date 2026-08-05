package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.EidosEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EidosRenderer extends GeoEntityRenderer<EidosEntity> {
    public EidosRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.4f;
    }

    @Override
    public void actuallyRender(
            PoseStack poseStack,
            EidosEntity animatable,
            BakedGeoModel model,
            net.minecraft.client.renderer.RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour) {
        int tinted = (Math.round(0.85f * 255) << 24) | (colour & 0x00FFFFFF);
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

    private static final class Model extends GeoModel<EidosEntity> {
        private static final ResourceLocation GEO = EffecoriaMod.id("geo/eidos.geo.json");
        private static final ResourceLocation TEX = EffecoriaMod.id("textures/entity/eidos.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/eidos.animation.json");

        @Override
        public ResourceLocation getModelResource(EidosEntity a) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(EidosEntity a) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(EidosEntity a) {
            return ANIM;
        }
    }
}
