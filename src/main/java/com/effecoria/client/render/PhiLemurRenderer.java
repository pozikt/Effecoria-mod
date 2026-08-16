package com.effecoria.client.render;

import com.effecoria.entity.PhiLemurEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class PhiLemurRenderer extends UniqueFaunaRenderer<PhiLemurEntity> {
    public PhiLemurRenderer(EntityRendererProvider.Context context) {
        super(context, "phi_lemur", 0.35f);
    }
}
