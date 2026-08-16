package com.effecoria.client.render;

import com.effecoria.entity.PhiEntEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class PhiEntRenderer extends UniqueFaunaRenderer<PhiEntEntity> {
    public PhiEntRenderer(EntityRendererProvider.Context context) {
        super(context, "phi_ent", 0.7f);
    }
}
