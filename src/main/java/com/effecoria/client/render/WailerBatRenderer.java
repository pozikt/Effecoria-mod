package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;

public class WailerBatRenderer extends BatRenderer {
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/wailer_bat.png");

    public WailerBatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Bat entity) {
        return TEXTURE;
    }
}
