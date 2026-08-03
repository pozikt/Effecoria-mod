package com.effecoria.client;

import com.effecoria.EffecoriaMod;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class SubspaceClientEffects {
    private SubspaceClientEffects() {}

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                EffecoriaMod.id("subspace"),
                new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, false) {
                    @Override
                    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
                        // Compressed Φ ocean — deep ultramarine / indigo.
                        return new Vec3(0.03, 0.06, 0.28);
                    }

                    @Override
                    public boolean isFoggyAt(int x, int y) {
                        return true;
                    }
                });
    }
}
