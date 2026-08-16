package com.effecoria.client.render;

import com.effecoria.entity.OmegaWormEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class OmegaWormRenderer extends UniqueFaunaRenderer<OmegaWormEntity> {
    public OmegaWormRenderer(EntityRendererProvider.Context context) {
        super(context, "omega_worm", 0.25f);
    }
}
