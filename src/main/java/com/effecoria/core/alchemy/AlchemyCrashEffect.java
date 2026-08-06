package com.effecoria.core.alchemy;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Harmful hangover after a Φ-alchemy buff expires. Amplifier encodes {@link AlchemyCrashKind}. */
public final class AlchemyCrashEffect extends MobEffect {
    public AlchemyCrashEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
