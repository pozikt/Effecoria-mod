package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.EssenceWyvernEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EssenceWyvernRenderer extends GeoEntityRenderer<EssenceWyvernEntity> {
    public EssenceWyvernRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 2.4f;
    }

    @Override
    public RenderType getRenderType(
            EssenceWyvernEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public float getMotionAnimThreshold(EssenceWyvernEntity animatable) {
        return 0.001f;
    }

    private static final class Model extends GeoModel<EssenceWyvernEntity> {
        private static final ResourceLocation GEO = EffecoriaMod.id("geo/essence_wyvern.geo.json");
        private static final ResourceLocation TEX = EffecoriaMod.id("textures/entity/essence_wyvern.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/essence_wyvern.animation.json");

        @Override
        public ResourceLocation getModelResource(EssenceWyvernEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(EssenceWyvernEntity animatable) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(EssenceWyvernEntity animatable) {
            return ANIM;
        }
    }
}
