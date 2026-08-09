package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.alchemy.AlchemyBuffEffect;
import com.effecoria.core.alchemy.AlchemyCrashEffect;
import com.effecoria.core.alchemy.AlchemyCrashKind;
import com.effecoria.effect.OmegaWoundEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    private ModMobEffects() {}

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> PHI_TONIC = MOB_EFFECTS.register(
            "phi_tonic",
            () -> new AlchemyBuffEffect(MobEffectCategory.BENEFICIAL, 0x5AD4FF, AlchemyCrashKind.TONIC));

    public static final DeferredHolder<MobEffect, MobEffect> PHI_RESONANCE = MOB_EFFECTS.register(
            "phi_resonance",
            () -> new AlchemyBuffEffect(MobEffectCategory.BENEFICIAL, 0x7B8CFF, AlchemyCrashKind.RESONANCE));

    public static final DeferredHolder<MobEffect, MobEffect> PHI_STIMULANT = MOB_EFFECTS.register(
            "phi_stimulant",
            () -> new AlchemyBuffEffect(MobEffectCategory.BENEFICIAL, 0xFFE066, AlchemyCrashKind.STIMULANT));

    public static final DeferredHolder<MobEffect, MobEffect> ALCHEMY_CRASH = MOB_EFFECTS.register(
            "alchemy_crash",
            () -> new AlchemyCrashEffect(MobEffectCategory.HARMFUL, 0x4A3A55));

    /** Temporary Φ-radiation resistance (potion / draught). */
    public static final DeferredHolder<MobEffect, MobEffect> PHI_RESISTANCE = MOB_EFFECTS.register(
            "phi_resistance",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xC9A84C) {});

    /** Skin clay crust — short weak Φ insulation. */
    public static final DeferredHolder<MobEffect, MobEffect> PHI_CLAY_SALVE = MOB_EFFECTS.register(
            "phi_clay_salve",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x6B8CAE) {});

    /** Internal lead screening — strong but toxic. */
    public static final DeferredHolder<MobEffect, MobEffect> LEAD_SATURATION = MOB_EFFECTS.register(
            "lead_saturation",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x5A5A62) {});

    /** Rotfang Ω-bite — wounds refuse to close. */
    public static final DeferredHolder<MobEffect, MobEffect> OMEGA_WOUND = MOB_EFFECTS.register(
            "omega_wound", OmegaWoundEffect::new);

    public static Holder<MobEffect> tonic() {
        return PHI_TONIC;
    }

    public static Holder<MobEffect> resonance() {
        return PHI_RESONANCE;
    }

    public static Holder<MobEffect> stimulant() {
        return PHI_STIMULANT;
    }

    public static Holder<MobEffect> crash() {
        return ALCHEMY_CRASH;
    }

    public static Holder<MobEffect> phiResistance() {
        return PHI_RESISTANCE;
    }

    public static Holder<MobEffect> claySalve() {
        return PHI_CLAY_SALVE;
    }

    public static Holder<MobEffect> leadSaturation() {
        return LEAD_SATURATION;
    }

    public static Holder<MobEffect> omegaWound() {
        return OMEGA_WOUND;
    }
}
