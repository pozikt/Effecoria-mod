package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.hud.PsiHudOverlay;
import com.effecoria.client.particle.SchoolParticles;
import com.effecoria.client.render.RootCageRenderer;
import com.effecoria.content.ModEntities;
import com.effecoria.content.ModParticleTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
        event.register(KeyBindings.CYCLE_SPELL_MODIFIER);
        event.register(KeyBindings.OPEN_SEAL_EDITOR);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ROOT_CAGE.get(), RootCageRenderer::new);
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
        event.registerSpriteSet(ModParticleTypes.ELEMENTAL_EMBER.get(), SchoolParticles.EmberParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ELEMENTAL_PLASMA.get(), SchoolParticles.PlasmaParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ELEMENTAL_SPARK.get(), SchoolParticles.LightningSparkParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ELEMENTAL_VACUUM.get(), SchoolParticles.VacuumParticle.Provider::new);

        // Mental — fog veil + association particles
        event.registerSpriteSet(
                ModParticleTypes.MENTAL_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 0.55F, 0.018F));
        event.registerSpriteSet(ModParticleTypes.MENTAL_SHARD.get(), SchoolParticles.MentalShardParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.MENTAL_FORCE.get(), SchoolParticles.MentalForceParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.MENTAL_SYNAPSE.get(), SchoolParticles.MentalSynapseParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.MENTAL_WARD.get(), SchoolParticles.MentalWardParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.MENTAL_FEAR.get(), SchoolParticles.MentalFearParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.MENTAL_SENSE.get(), SchoolParticles.MentalSenseParticle.Provider::new);

        // Organic
        event.registerSpriteSet(ModParticleTypes.ORGANIC_LEAF.get(), SchoolParticles.LeafParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_ROOT.get(), SchoolParticles.RootParticle.Provider::new);
        event.registerSpriteSet(
                ModParticleTypes.ORGANIC_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 0.65F, 0.02F));
        event.registerSpriteSet(ModParticleTypes.ORGANIC_SPORE.get(), SchoolParticles.SporeParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_THORN.get(), SchoolParticles.ThornParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_SAP.get(), SchoolParticles.SapParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_BLOOD_CELL.get(), SchoolParticles.BloodCellParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_WHITE_CELL.get(), SchoolParticles.WhiteCellParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_VIRUS.get(), SchoolParticles.VirusParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_PARASITE.get(), SchoolParticles.ParasiteParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_BONE.get(), SchoolParticles.BoneShardParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_CHITIN.get(), SchoolParticles.ChitinParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_MUSCLE.get(), SchoolParticles.MuscleParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_NERVE.get(), SchoolParticles.NerveParticle.Provider::new);
        event.registerSpriteSet(ModParticleTypes.ORGANIC_DNA.get(), SchoolParticles.DnaParticle.Provider::new);

        // Necromancy
        event.registerSpriteSet(ModParticleTypes.NECRO_SHADOW.get(), SchoolParticles.ShadowParticle.Provider::new);
        event.registerSpriteSet(
                ModParticleTypes.NECRO_FOG.get(),
                sprites -> new SchoolParticles.FogParticle.Provider(sprites, 0.75F, 0.012F));

        // Spatial uses Veil distortion only — no particle providers
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
