package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.EidosEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EidosRenderer extends GeoEntityRenderer<EidosEntity> {
    public EidosRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.35f;
    }

    @Override
    public RenderType getRenderType(
            EidosEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
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
