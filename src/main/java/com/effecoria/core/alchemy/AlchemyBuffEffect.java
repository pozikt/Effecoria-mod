package com.effecoria.core.alchemy;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class AlchemyBuffEffect extends MobEffect {
    private final AlchemyCrashKind crashKind;

    public AlchemyBuffEffect(MobEffectCategory category, int color, AlchemyCrashKind crashKind) {
        super(category, color);
        this.crashKind = crashKind;
    }

    public AlchemyCrashKind crashKind() {
        return crashKind;
    }
}
