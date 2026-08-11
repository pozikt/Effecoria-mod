package com.effecoria.content;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, EffecoriaMod.MOD_ID);

    /** Hypervelocity kinetic turret shot — synthetic gauss crack. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KINETIC_GAUSS_SHOT = SOUND_EVENTS.register(
            "kinetic_gauss_shot", () -> SoundEvent.createVariableRangeEvent(EffecoriaMod.id("kinetic_gauss_shot")));

    private ModSounds() {}
}
