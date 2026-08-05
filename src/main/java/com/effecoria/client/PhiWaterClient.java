package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModFluids;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import org.joml.Vector3f;

/** Aquamarine client look for cave Φ-hydrolat. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class PhiWaterClient {
    private static final int PHI_WATER_TINT = 0xFF2A8B9A;

    private PhiWaterClient() {}

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(
                new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return ResourceLocation.withDefaultNamespace("block/water_still");
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return ResourceLocation.withDefaultNamespace("block/water_flow");
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return ResourceLocation.withDefaultNamespace("block/water_overlay");
                    }

                    @Override
                    public int getTintColor() {
                        return PHI_WATER_TINT;
                    }

                    @Override
                    public int getTintColor(
                            net.minecraft.world.level.material.FluidState state,
                            net.minecraft.world.level.BlockAndTintGetter getter,
                            net.minecraft.core.BlockPos pos) {
                        return PHI_WATER_TINT;
                    }

                    @Override
                    public Vector3f modifyFogColor(
                            net.minecraft.client.Camera camera,
                            float partialTick,
                            net.minecraft.client.multiplayer.ClientLevel level,
                            int renderDistance,
                            float darkenWorldAmount,
                            Vector3f fluidFogColor) {
                        return new Vector3f(0.12f, 0.42f, 0.55f);
                    }
                },
                ModFluids.PHI_WATER_TYPE.value());
    }
}
