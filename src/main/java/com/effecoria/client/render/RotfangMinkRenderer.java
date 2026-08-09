package com.effecoria.client.render;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.RotfangMinkEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Fox;

/** Fox renderer retargeted to Rotfang texture / scale. */
public class RotfangMinkRenderer extends FoxRenderer {
    private static final ResourceLocation TEXTURE = EffecoriaMod.id("textures/entity/rotfang_mink.png");

    public RotfangMinkRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(Fox entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.85f, 0.85f, 0.85f);
        super.scale(entity, poseStack, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(Fox entity) {
        return TEXTURE;
    }
}
