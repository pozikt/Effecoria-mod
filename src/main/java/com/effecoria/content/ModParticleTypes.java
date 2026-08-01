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
    /** Fire cinder / ember ash. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELEMENTAL_EMBER =
            PARTICLE_TYPES.register("elemental_ember", () -> new SimpleParticleType(false));
    /** Hot plasma mote — violet-white. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELEMENTAL_PLASMA =
            PARTICLE_TYPES.register("elemental_plasma", () -> new SimpleParticleType(false));
    /** Lightning / ionization spark. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELEMENTAL_SPARK =
            PARTICLE_TYPES.register("elemental_spark", () -> new SimpleParticleType(false));
    /** Vacuum / void swirl. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELEMENTAL_VACUUM =
            PARTICLE_TYPES.register("elemental_vacuum", () -> new SimpleParticleType(false));

    // Mental
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_FOG =
            PARTICLE_TYPES.register("mental_fog", () -> new SimpleParticleType(false));
    /** Psychic needle / mind shard. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_SHARD =
            PARTICLE_TYPES.register("mental_shard", () -> new SimpleParticleType(false));
    /** Telekinetic force ripple. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_FORCE =
            PARTICLE_TYPES.register("mental_force", () -> new SimpleParticleType(false));
    /** Synapse flash — neural lock / overload / scream. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_SYNAPSE =
            PARTICLE_TYPES.register("mental_synapse", () -> new SimpleParticleType(false));
    /** Ward / fortress hex mote. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_WARD =
            PARTICLE_TYPES.register("mental_ward", () -> new SimpleParticleType(false));
    /** Compulsion / terror mote. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_FEAR =
            PARTICLE_TYPES.register("mental_fear", () -> new SimpleParticleType(false));
    /** Sense / probe / focus iris spark. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MENTAL_SENSE =
            PARTICLE_TYPES.register("mental_sense", () -> new SimpleParticleType(false));

    // Organic
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_LEAF =
            PARTICLE_TYPES.register("organic_leaf", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_ROOT =
            PARTICLE_TYPES.register("organic_root", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_FOG =
            PARTICLE_TYPES.register("organic_fog", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_SPORE =
            PARTICLE_TYPES.register("organic_spore", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_THORN =
            PARTICLE_TYPES.register("organic_thorn", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_SAP =
            PARTICLE_TYPES.register("organic_sap", () -> new SimpleParticleType(false));
    /** Erythrocyte — heal / vital FX. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_BLOOD_CELL =
            PARTICLE_TYPES.register("organic_blood_cell", () -> new SimpleParticleType(false));
    /** Leukocyte / stabilizer cell — immune + tissue repair FX. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_WHITE_CELL =
            PARTICLE_TYPES.register("organic_white_cell", () -> new SimpleParticleType(false));
    /** Virion / foreign agent. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_VIRUS =
            PARTICLE_TYPES.register("organic_virus", () -> new SimpleParticleType(false));
    /** Parasite worm / larva. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_PARASITE =
            PARTICLE_TYPES.register("organic_parasite", () -> new SimpleParticleType(false));
    /** Bone shard. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_BONE =
            PARTICLE_TYPES.register("organic_bone", () -> new SimpleParticleType(false));
    /** Chitin plate flake. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_CHITIN =
            PARTICLE_TYPES.register("organic_chitin", () -> new SimpleParticleType(false));
    /** Muscle fiber streak. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_MUSCLE =
            PARTICLE_TYPES.register("organic_muscle", () -> new SimpleParticleType(false));
    /** Nerve / synapse spark. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_NERVE =
            PARTICLE_TYPES.register("organic_nerve", () -> new SimpleParticleType(false));
    /** DNA / genetic fragment. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORGANIC_DNA =
            PARTICLE_TYPES.register("organic_dna", () -> new SimpleParticleType(false));

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
    /** Rot / decay flake. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_ROT =
            PARTICLE_TYPES.register("corruption_rot", () -> new SimpleParticleType(false));
    /** Thick miasma fog. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_MIASMA =
            PARTICLE_TYPES.register("corruption_miasma", () -> new SimpleParticleType(false));
    /** Entropy ash / crumbling mote. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_ENTROPY =
            PARTICLE_TYPES.register("corruption_entropy", () -> new SimpleParticleType(false));
    /** Binding chain link. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTION_BIND =
            PARTICLE_TYPES.register("corruption_bind", () -> new SimpleParticleType(false));

    // Seals
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SEAL_GLYPH =
            PARTICLE_TYPES.register("seal_glyph", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SEAL_SPARK =
            PARTICLE_TYPES.register("seal_spark", () -> new SimpleParticleType(false));

    // General Φ motes
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHI_SPARK =
            PARTICLE_TYPES.register("phi_spark", () -> new SimpleParticleType(false));
}
