package com.effecoria.effect;

import java.util.Set;

import javax.annotation.Nullable;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.RadialCategory;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.effect.spatial.SpatialVfx;
import com.effecoria.magic.CastDelivery;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared cast readability: school wind-up at the caster, impact burst, and a horizontal AoE ring.
 * Spell-specific FX still run in school effect classes; this layer makes every cast identifiable.
 * Spatial school is sound + Veil distortion only (no particles).
 */
public final class CastPresentation {
    private CastPresentation() {}

    /** Organic heals use cellular FX; plant/combat organic keep leaf/fog. */
    private static final Set<ResourceLocation> ORGANIC_HEAL_SPELLS = Set.of(
            ResourceLocation.fromNamespaceAndPath("effecoria", "blood_stasis"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "vitality_pulse"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "verdant_mend"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "biological_field"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "super_regeneration"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "absolute_regeneration"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "symbiotic_graft"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "limb_regeneration"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "vital_infusion"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "soothing_sap"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "vital_ward"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "adrenal_gift"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "life_creation"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "biological_immortality"),
            ResourceLocation.fromNamespaceAndPath("effecoria", "biological_singularity"));

    /** Soft school cue at the caster before spell logic runs. */
    public static void playWindUp(ServerPlayer caster, SpellDefinition spell) {
        MagicSchool school = spell.requiredSchool();
        if (school == MagicSchool.NONE) {
            school = MagicSchool.MENTAL;
        }
        playWindUp(caster, themeFor(school, spell));
    }

    /** Impact / ring on full delivery, or a quiet fizzle on whiff. */
    public static void playResolve(ServerPlayer caster, SpellDefinition spell, float power, CastDelivery delivery) {
        MagicSchool school = spell.requiredSchool();
        if (school == MagicSchool.NONE) {
            school = MagicSchool.MENTAL;
        }
        SchoolTheme theme = themeFor(school, spell);
        if (delivery == CastDelivery.WHIFF_NO_TARGET || delivery == CastDelivery.WHIFF_NO_BLOCK) {
            playWhiff(caster, theme);
            return;
        }
        Vec3 focus = resolveFocus(caster, 12.0);
        if (theme.usesParticles()) {
            playImpact(caster.serverLevel(), focus, theme, power);
            playRing(caster.serverLevel(), focus, theme, ringRadius(spell, power));
        }
        playImpactSound(caster.serverLevel(), focus, theme);
        if (school == MagicSchool.SPATIAL) {
            switch (SpatialVfx.bucket(spell.id())) {
                case SINGULARITY -> SpatialVfx.playSingularity(caster, focus, power);
                case RIPPLE -> SpatialVfx.playRipple(caster, caster.position().add(0, 1, 0), power);
                default -> {
                    // Cuts owned by hit methods; NONE is sound-only
                }
            }
        }
    }

    private static void playWindUp(ServerPlayer caster, SchoolTheme theme) {
        ServerLevel level = caster.serverLevel();
        Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.55)).add(0, -0.25, 0);
        if (theme.usesParticles()) {
            level.sendParticles(theme.primary(), hand.x, hand.y, hand.z, 10, 0.18, 0.18, 0.18, 0.02);
            level.sendParticles(theme.secondary(), hand.x, hand.y, hand.z, 6, 0.12, 0.14, 0.12, 0.01);
        }
        level.playSound(
                null,
                caster.blockPosition(),
                theme.windUp(),
                SoundSource.PLAYERS,
                theme.windUpVolume(),
                theme.windUpPitch());
    }

    private static void playWhiff(ServerPlayer caster, SchoolTheme theme) {
        ServerLevel level = caster.serverLevel();
        Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.4));
        if (theme.usesParticles()) {
            level.sendParticles(theme.secondary(), hand.x, hand.y, hand.z, 4, 0.1, 0.1, 0.1, 0.005);
        }
        level.playSound(
                null,
                caster.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                0.25f,
                1.6f);
    }

    private static void playImpact(ServerLevel level, Vec3 focus, SchoolTheme theme, float power) {
        int burst = 10 + Mth.clamp(Math.round(power / 8f), 0, 18);
        level.sendParticles(theme.primary(), focus.x, focus.y, focus.z, burst, 0.28, 0.35, 0.28, 0.04);
        level.sendParticles(theme.secondary(), focus.x, focus.y + 0.15, focus.z, burst / 2, 0.2, 0.25, 0.2, 0.02);
    }

    private static void playRing(ServerLevel level, Vec3 center, SchoolTheme theme, float radius) {
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2.0 * i) / segments;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(theme.primary(), x, center.y, z, 1, 0.02, 0.04, 0.02, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(theme.secondary(), x, center.y + 0.12, z, 1, 0.02, 0.06, 0.02, 0.0);
            }
        }
    }

    private static void playImpactSound(ServerLevel level, Vec3 focus, SchoolTheme theme) {
        level.playSound(
                null,
                focus.x,
                focus.y,
                focus.z,
                theme.impact(),
                SoundSource.PLAYERS,
                theme.impactVolume(),
                theme.impactPitch());
    }

    private static float ringRadius(SpellDefinition spell, float power) {
        float base = spell.radialCategory() == RadialCategory.COMBAT ? 1.7f : 1.15f;
        return Mth.clamp(base + power / 90f, 1.0f, 3.2f);
    }

    private static Vec3 resolveFocus(ServerPlayer caster, double range) {
        HitResult hit = caster.pick(range, 0f, false);
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        return caster.getEyePosition().add(caster.getLookAngle().scale(Math.min(range, 3.5)));
    }

    private static SchoolTheme themeFor(MagicSchool school, SpellDefinition spell) {
        return switch (school) {
            case MENTAL -> new SchoolTheme(
                    ModParticleTypes.MENTAL_FOG.get(),
                    ModParticleTypes.PHI_SPARK.get(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundEvents.ILLUSIONER_CAST_SPELL,
                    0.45f,
                    1.35f,
                    0.55f,
                    1.25f);
            case ELEMENTAL -> new SchoolTheme(
                    ModParticleTypes.PHI_FLAME.get(),
                    ModParticleTypes.PHI_GUST.get(),
                    SoundEvents.BLAZE_SHOOT,
                    SoundEvents.FIRECHARGE_USE,
                    0.4f,
                    1.25f,
                    0.55f,
                    1.05f);
            case ORGANIC -> ORGANIC_HEAL_SPELLS.contains(spell.id())
                    ? new SchoolTheme(
                            ModParticleTypes.ORGANIC_BLOOD_CELL.get(),
                            ModParticleTypes.ORGANIC_WHITE_CELL.get(),
                            SoundEvents.HONEY_DRINK,
                            SoundEvents.AMETHYST_BLOCK_CHIME,
                            0.4f,
                            1.35f,
                            0.5f,
                            1.45f)
                    // Generic organic cast: sound only — no leaf burst around the caster.
                    // Spell-specific FX (virus, thorns, DNA, …) come from OrganicEffects.
                    : new SchoolTheme(
                            null,
                            null,
                            SoundEvents.MOSS_PLACE,
                            SoundEvents.AZALEA_LEAVES_PLACE,
                            0.45f,
                            1.1f,
                            0.5f,
                            0.95f);
            case NECROMANCY -> new SchoolTheme(
                    ModParticleTypes.NECRO_SHADOW.get(),
                    ModParticleTypes.NECRO_FOG.get(),
                    SoundEvents.SOUL_ESCAPE.value(),
                    SoundEvents.WITHER_AMBIENT,
                    0.45f,
                    0.85f,
                    0.5f,
                    0.75f);
            case SPATIAL -> new SchoolTheme(
                    null,
                    null,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundEvents.CHORUS_FRUIT_TELEPORT,
                    0.45f,
                    1.2f,
                    0.6f,
                    0.95f);
            case CORRUPTION -> new SchoolTheme(
                    ModParticleTypes.CORRUPTION_POISON.get(),
                    ModParticleTypes.CORRUPTION_RUNE.get(),
                    SoundEvents.SCULK_CLICKING,
                    SoundEvents.SCULK_SHRIEKER_SHRIEK,
                    0.45f,
                    0.8f,
                    0.45f,
                    1.15f);
            case SEALS -> new SchoolTheme(
                    ModParticleTypes.SEAL_GLYPH.get(),
                    ModParticleTypes.SEAL_SPARK.get(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                    0.5f,
                    1.2f,
                    0.55f,
                    1.1f);
            case NONE -> new SchoolTheme(
                    ModParticleTypes.PHI_SPARK.get(),
                    ModParticleTypes.PHI_SPARK.get(),
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(),
                    0.3f,
                    1.0f,
                    0.3f,
                    1.0f);
        };
    }

    private record SchoolTheme(
            @Nullable SimpleParticleType primary,
            @Nullable SimpleParticleType secondary,
            SoundEvent windUp,
            SoundEvent impact,
            float windUpVolume,
            float windUpPitch,
            float impactVolume,
            float impactPitch) {
        boolean usesParticles() {
            return primary != null && secondary != null;
        }
    }
}
