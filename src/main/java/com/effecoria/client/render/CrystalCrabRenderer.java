package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.CrystalCrabEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CrystalCrabRenderer extends GeoEntityRenderer<CrystalCrabEntity> {
    public CrystalCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.65f;
    }

    @Override
    public RenderType getRenderType(
            CrystalCrabEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    private static final class Model extends GeoModel<CrystalCrabEntity> {
        private static final ResourceLocation GEO = EffecoriaMod.id("geo/crystal_crab.geo.json");
        private static final ResourceLocation TEX = EffecoriaMod.id("textures/entity/crystal_crab.png");
        private static final ResourceLocation ANIM = EffecoriaMod.id("animations/crystal_crab.animation.json");

        @Override
        public ResourceLocation getModelResource(CrystalCrabEntity a) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(CrystalCrabEntity a) {
            return TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(CrystalCrabEntity a) {
            return ANIM;
        }
    }
}
