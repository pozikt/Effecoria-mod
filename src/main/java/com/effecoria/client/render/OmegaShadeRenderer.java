package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.OmegaShadeEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vex;

public class OmegaShadeRenderer extends VexRenderer {
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/omega_shade.png");

    public OmegaShadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2f;
    }

    @Override
    public ResourceLocation getTextureLocation(Vex entity) {
        return TEXTURE;
    }
}
