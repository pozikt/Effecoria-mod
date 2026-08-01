package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** School-themed particles — each type has unique sprites and client behaviour. */
public final class ModParticleTypes {
    private ModParticleTypes() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EffecoriaMod.MOD_ID);

    // Elemental — water / fire / wind
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATER_DROP =
            PARTICLE_TYPES.register("water_drop", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATER_SPLASH =
            PARTICLE_TYPES.register("water_splash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WATER_WAVE =
            PARTICLE_TYPES.register("water_wave", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STEAM_FOG =
            PARTICLE_TYPES.register("steam_fog", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_CRYSTAL =
            PARTICLE_TYPES.register("ice_crystal", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHI_FLAME =
            PARTICLE_TYPES.register("phi_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHI_GUST =
            PARTICLE_TYPES.register("phi_gust", () -> new SimpleParticleType(false));

    // Mental
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_FOG =
            PARTICLE_TYPES.register("mental_fog", () -> new SimpleParticleType(false));

    // Organic
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_LEAF =
            PARTICLE_TYPES.register("organic_leaf", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_ROOT =
            PARTICLE_TYPES.register("organic_root", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_FOG =
            PARTICLE_TYPES.register("organic_fog", () -> new SimpleParticleType(false));

    // Necromancy
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NECRO_SHADOW =
            PARTICLE_TYPES.register("necro_shadow", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NECRO_FOG =
            PARTICLE_TYPES.register("necro_fog", () -> new SimpleParticleType(false));

    // Corruption
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_POISON =
            PARTICLE_TYPES.register("corruption_poison", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_BLOOD =
            PARTICLE_TYPES.register("corruption_blood", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_RUNE =
            PARTICLE_TYPES.register("corruption_rune", () -> new SimpleParticleType(false));

    // Seals
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SEAL_GLYPH =
            PARTICLE_TYPES.register("seal_glyph", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SEAL_SPARK =
            PARTICLE_TYPES.register("seal_spark", () -> new SimpleParticleType(false));

    // General Φ motes
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHI_SPARK =
            PARTICLE_TYPES.register("phi_spark", () -> new SimpleParticleType(false));
}
