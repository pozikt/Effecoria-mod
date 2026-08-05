package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.PhiLarvaEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PhiLarvaRenderer extends GeoEntityRenderer<PhiLarvaEntity> {
    public PhiLarvaRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.25f;
    }

    private static final class Model extends GeoModel<PhiLarvaEntity> {
        private static final ResourceLocation GEO = EffecoriaMod.id("geo/phi_larva.geo.json");
        private static final ResourceLocation TEX = EffecoriaMod.id("textures/entity/phi_larva.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/phi_larva.animation.json");

        @Override
        public ResourceLocation getModelResource(PhiLarvaEntity a) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(PhiLarvaEntity a) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(PhiLarvaEntity a) {
            return ANIM;
        }
    }
}
