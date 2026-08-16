package com.effecoria.client.render;

import com.effecoria.entity.OmegaShadeEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class OmegaShadeRenderer extends UniqueFaunaRenderer<OmegaShadeEntity> {
    public OmegaShadeRenderer(EntityRendererProvider.Context context) {
        super(context, "omega_shade", 0.35f);
    }
}
