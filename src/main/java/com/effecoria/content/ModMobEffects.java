package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.alchemy.AlchemyBuffEffect;
import com.effecoria.core.alchemy.AlchemyCrashEffect;
import com.effecoria.core.alchemy.AlchemyCrashKind;

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
}
