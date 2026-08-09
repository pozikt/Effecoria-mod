package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

public class PhiEntRenderer extends IronGolemRenderer {
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/phi_ent.png");

    public PhiEntRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolem entity) {
        return TEXTURE;
    }
}
