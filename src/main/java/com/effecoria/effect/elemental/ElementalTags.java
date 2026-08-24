package com.effecoria.effect.elemental;

/** NBT keys for elemental projectiles and temporary blocks. */
public final class ElementalTags {
    public static final String PROJECTILE = "effecoria_elemental";
    public static final String KIND = "effecoria_kind";
    public static final String POWER = "effecoria_power";

    public static final String KIND_WEAK_FIRE = "weak_fire";
    public static final String KIND_ICE_SHARD = "ice_shard";
    public static final String KIND_PLASMA = "plasma";
    public static final String KIND_GREAT_FIRE = "great_fire";
    public static final String KIND_MATTER_WATER = "matter_water";
    public static final String KIND_MATTER_ICE = "matter_ice";
    public static final String KIND_MATTER_LAVA = "matter_lava";
    public static final String KIND_MATTER_DUST = "matter_dust";

    /** Remaining fire mass units for shedding fireballs. */
    public static final String FIRE_MASS = "effecoria_fire_mass";
    public static final String IGNITE_RADIUS = "effecoria_ignite_radius";
    public static final String GROUND_IGNITE_COUNT = "effecoria_ground_ignite";

    private ElementalTags() {}
}
