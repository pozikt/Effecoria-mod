package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.RootCageEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class RootCageModel extends GeoModel<RootCageEntity> {
    private static final ResourceLocation MODEL = EffecoriaMod.id("geo/root_cage.geo.json");
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/root_cage.png");
    private static final ResourceLocation ANIM = EffecoriaMod.id("animations/root_cage.animation.json");

    @Override
    public ResourceLocation getModelResource(RootCageEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RootCageEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RootCageEntity animatable) {
        return ANIM;
    }
}
