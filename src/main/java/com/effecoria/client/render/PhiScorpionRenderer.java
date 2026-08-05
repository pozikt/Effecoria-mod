package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.PhiScorpionEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Placeholder: reuses crystal-crab geo with gold/teal Φ-scorpion texture. */
public class PhiScorpionRenderer extends GeoEntityRenderer<PhiScorpionEntity> {
    public PhiScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.5f;
    }

    private static final class Model extends GeoModel<PhiScorpionEntity> {
        private static final ResourceLocation GEO = EffecoriaMod.id("geo/crystal_crab.geo.json");
        private static final ResourceLocation TEX = EffecoriaMod.id("textures/entity/phi_scorpion.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/crystal_crab.animation.json");

        @Override
        public ResourceLocation getModelResource(PhiScorpionEntity a) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(PhiScorpionEntity a) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(PhiScorpionEntity a) {
            return ANIM;
        }
    }
}
