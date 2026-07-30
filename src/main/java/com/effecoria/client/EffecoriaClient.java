package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.hud.PsiHudOverlay;
import com.effecoria.client.particle.SchoolParticles;
import com.effecoria.content.ModParticleTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class EffecoriaClient {
    private EffecoriaClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.CAST_SPELL);
        event.register(KeyBindings.OPEN_SPELL_BOOK);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, EffecoriaMod.id("psi_hud"), PsiHudOverlay::render);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Elemental
        event.registerSpriteSet(ModParticleTypes.WATER_DROP.get(), SchoolParticles.DropParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.WATER_SPLASH.get(), SchoolParticles.SplashParticle.Provider::new);
        event.registerSpriteSet(
                ModParticleTypes.WATER_WAVE.get(), sprites -> new SchoolParticles.StreakParticle.Provider(sprites, 0.12F));
        event.registerSpriteSet(
                ModParticleTypes.STEAM_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 1.4F, 0.03F, 32, 28, 0.5F, 0.3F));
        event.registerSpriteSet(ModParticleTypes.ICE_CRYSTAL.get(), SchoolParticles.SparkParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.PHI_FLAME.get(), SchoolParticles.FlameParticle.Provider::new);
        event.registerSpriteSet(
                ModParticleTypes.PHI_GUST.get(), sprites -> new SchoolParticles.StreakParticle.Provider(sprites, 0.1F));

        // Mental — brief fog veil
        event.registerSpriteSet(
                ModParticleTypes.MENTAL_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 0.55F, 0.018F));

        // Organic
        event.registerSpriteSet(ModParticleTypes.ORGANIC_LEAF.get(), SchoolParticles.LeafParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_ROOT.get(), SchoolParticles.RootParticle.Provider::new);
        event.registerSpriteSet(
                ModParticleTypes.ORGANIC_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 0.65F, 0.02F));

        // Necromancy
        event.registerSpriteSet(ModParticleTypes.NECRO_SHADOW.get(), SchoolParticles.ShadowParticle.Provider::new);
        event.registerSpriteSet(
                ModParticleTypes.NECRO_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 0.75F, 0.012F));

        // Spatial — wobble / warp
        event.registerSpriteSet(
                ModParticleTypes.SPATIAL_RIFT.get(),
                sprites -> new SchoolParticles.WarpParticle.Provider(sprites, 0.14F, 0.4F, 0.1F));
        event.registerSpriteSet(
                ModParticleTypes.SPATIAL_WARP.get(),
                sprites -> new SchoolParticles.WarpParticle.Provider(sprites, 0.12F, 0.55F, 0.14F));

        // Corruption
        event.registerSpriteSet(ModParticleTypes.CORRUPTION_POISON.get(), SchoolParticles.DropParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CORRUPTION_BLOOD.get(), SchoolParticles.BloodParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.CORRUPTION_RUNE.get(), SchoolParticles.GlyphParticle.Provider::new);

        // Seals
        event.registerSpriteSet(ModParticleTypes.SEAL_GLYPH.get(), SchoolParticles.GlyphParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.SEAL_SPARK.get(), SchoolParticles.SparkParticle.Provider::new);

        event.registerSpriteSet(ModParticleTypes.PHI_SPARK.get(), SchoolParticles.SparkParticle.Provider::new);
    }
}
