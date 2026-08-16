package com.effecoria.client.render;

import com.effecoria.entity.WailerBatEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WailerBatRenderer extends UniqueFaunaRenderer<WailerBatEntity> {
    public WailerBatRenderer(EntityRendererProvider.Context context) {
        super(context, "wailer_bat", 0.25f);
    }
}
