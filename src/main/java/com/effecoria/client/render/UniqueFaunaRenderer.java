package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Shared cutout Geo renderer for unique fauna (opaque atlases). */
public class UniqueFaunaRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T> {
    public UniqueFaunaRenderer(EntityRendererProvider.Context context, String id, float shadow) {
        super(context, new FaunaModel<>(id));
        this.shadowRadius = shadow;
    }

    @Override
    public RenderType getRenderType(
            T animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    private static final class FaunaModel<T extends LivingEntity & GeoEntity> extends GeoModel<T> {
        private final ResourceLocation geo;
        private final ResourceLocation tex;
        private final ResourceLocation anim;

        FaunaModel(String id) {
            this.geo = EffecoriaMod.id("geo/" + id + ".geo.json");
            this.tex = EffecoriaMod.id("textures/entity/" + id + ".png");
            this.anim = EffecoriaMod.id("animations/" + id + ".animation.json");
        }

        @Override
        public ResourceLocation getModelResource(T a) {
            return geo;
        }

        @Override
        public ResourceLocation getTextureResource(T a) {
            return tex;
        }

        @Override
        public ResourceLocation getAnimationResource(T a) {
            return anim;
        }
    }
}
