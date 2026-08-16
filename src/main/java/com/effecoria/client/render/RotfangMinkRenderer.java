package com.effecoria.client.render;

import com.effecoria.entity.RotfangMinkEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class RotfangMinkRenderer extends UniqueFaunaRenderer<RotfangMinkEntity> {
    public RotfangMinkRenderer(EntityRendererProvider.Context context) {
        super(context, "rotfang_mink", 0.4f);
    }
}
