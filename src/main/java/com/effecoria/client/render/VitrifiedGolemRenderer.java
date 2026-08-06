package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.VitrifiedGolemEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VitrifiedGolemRenderer extends GeoEntityRenderer<VitrifiedGolemEntity> {
    public VitrifiedGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.65f;
    }

    private static final class Model extends GeoModel<VitrifiedGolemEntity> {
        private static final ResourceLocation GEO = EffecoriaMod.id("geo/vitrified_golem.geo.json");
        private static final ResourceLocation TEX = EffecoriaMod.id("textures/entity/vitrified_golem.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/vitrified_golem.animation.json");

        @Override
        public ResourceLocation getModelResource(VitrifiedGolemEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(VitrifiedGolemEntity animatable) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(VitrifiedGolemEntity animatable) {
            return ANIM;
        }
    }
}
