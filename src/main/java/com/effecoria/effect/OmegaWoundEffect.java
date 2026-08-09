package com.effecoria.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Ω-bite wound — living heal is cancelled while active. */
public final class OmegaWoundEffect extends MobEffect {
    public OmegaWoundEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A1A7A);
    }
}
