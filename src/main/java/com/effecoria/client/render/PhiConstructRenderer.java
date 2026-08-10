package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.PhiConstructEntity;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Friendly construct — same geo as vitrified golem, tinted via cutout texture reuse. */
public class PhiConstructRenderer extends GeoEntityRenderer<PhiConstructEntity> {
    public PhiConstructRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.55f;
    }

    @Override
    public RenderType getRenderType(
            PhiConstructEntity animatable,
            ResourceLocation texture,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    private static final class Model extends GeoModel<PhiConstructEntity> {
        private static final ResourceLocation MODEL = EffecoriaMod.id("geo/vitrified_golem.geo.json");
        private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/vitrified_golem.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/vitrified_golem.animation.json");

        @Override
        public ResourceLocation getModelResource(PhiConstructEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(PhiConstructEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(PhiConstructEntity animatable) {
            return ANIM;
        }
    }
}
