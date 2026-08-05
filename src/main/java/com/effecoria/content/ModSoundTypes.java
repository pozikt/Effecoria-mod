package com.effecoria.content;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

/**
 * Φ-field block acoustics — glass-chime steps / crystalline break without custom audio files.
 */
public final class ModSoundTypes {
    private ModSoundTypes() {}

    /** Soft soil with micro-essonite chime underfoot. */
    public static final SoundType PHI_EARTH = new SoundType(
            1.0f,
            1.25f,
            SoundEvents.ROOTED_DIRT_BREAK,
            SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundEvents.ROOTED_DIRT_PLACE,
            SoundEvents.AMETHYST_BLOCK_HIT,
            SoundEvents.ROOTED_DIRT_FALL);

    /** Turf over Φ-earth — grass place/break, chime steps. */
    public static final SoundType PHI_GRASS = new SoundType(
            1.0f,
            1.15f,
            SoundEvents.GRASS_BREAK,
            SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundEvents.GRASS_PLACE,
            SoundEvents.AMETHYST_BLOCK_HIT,
            SoundEvents.GRASS_FALL);

    /** Graphite stone with crystalline shatter + lower pitch (Φ-hum feel). */
    public static final SoundType PHI_STONE = new SoundType(
            1.0f,
            0.75f,
            SoundEvents.AMETHYST_BLOCK_BREAK,
            SoundEvents.AMETHYST_BLOCK_STEP,
            SoundEvents.AMETHYST_BLOCK_PLACE,
            SoundEvents.AMETHYST_BLOCK_HIT,
            SoundEvents.AMETHYST_BLOCK_FALL);
}
