package com.effecoria.core.seal;

import com.effecoria.EffecoriaMod;

import net.minecraft.resources.ResourceLocation;

/** Built-in seal effect types placed on blocks. */
public final class SealTypes {
    private SealTypes() {}

    /** Player-authored word program (replaces legacy fixed types). */
    public static final ResourceLocation PROGRAM = EffecoriaMod.id("program");

    /** Legacy; kept for world migration / particles. */
    @Deprecated
    public static final ResourceLocation DAMAGE_TRAP = EffecoriaMod.id("damage_trap");
    /** Legacy. */
    @Deprecated
    public static final ResourceLocation FORTIFY = EffecoriaMod.id("fortify");
    /** Legacy. */
    @Deprecated
    public static final ResourceLocation GLOW = EffecoriaMod.id("glow");
    /** Legacy. */
    @Deprecated
    public static final ResourceLocation SNARE = EffecoriaMod.id("snare");
    /** Legacy. */
    @Deprecated
    public static final ResourceLocation REPULSE = EffecoriaMod.id("repulse");
}
