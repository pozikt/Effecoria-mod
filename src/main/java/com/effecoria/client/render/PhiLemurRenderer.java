package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Fox;

public class PhiLemurRenderer extends FoxRenderer {
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/phi_lemur.png");

    public PhiLemurRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Fox entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.72f, 0.72f, 0.72f);
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(Fox entity) {
        return TEXTURE;
    }
}
