package com.effecoria.client.render;

import com.effecoria.entity.GlassWormEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GlassWormRenderer extends UniqueFaunaRenderer<GlassWormEntity> {
    public GlassWormRenderer(EntityRendererProvider.Context context) {
        super(context, "glass_worm", 0.2f);
    }
}
