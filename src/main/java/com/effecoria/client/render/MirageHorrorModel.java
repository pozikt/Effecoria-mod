package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.MirageHorrorEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class MirageHorrorModel extends GeoModel<MirageHorrorEntity> {
    private static final ResourceLocation MODEL = EffecoriaMod.id("geo/mirage_horror.geo.json");
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/mirage_horror.png");
    private static final ResourceLocation ANIM = EffecoriaMod.id("animations/mirage_horror.animation.json");

    @Override
    public ResourceLocation getModelResource(MirageHorrorEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MirageHorrorEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MirageHorrorEntity animatable) {
        return ANIM;
    }
}
