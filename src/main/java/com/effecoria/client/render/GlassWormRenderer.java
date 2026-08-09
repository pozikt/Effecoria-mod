package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Silverfish;

public class GlassWormRenderer extends SilverfishRenderer {
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/glass_worm.png");

    public GlassWormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Silverfish entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.15f, 1.15f, 1.15f);
    }

    @Override
    public ResourceLocation getTextureLocation(Silverfish entity) {
        return TEXTURE;
    }
}
