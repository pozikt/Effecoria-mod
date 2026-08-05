package com.effecoria.world;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.SpellDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * High-Φ Glass Plain (Φ-Пустыня) — ambient Φ boost and school-biased casting.
 */
public final class PhiGlassPlainService {
    private PhiGlassPlainService() {}

    private static final Set<String> WATERISH = Set.of(
            "water_stream",
            "hydro_slice",
            "steam_jet",
            "steam_flight",
            "steam_veil",
            "water_shield",
            "water_shroud",
            "breath_bubble",
            "cryo_wave",
            "frost_bastion",
            "ice_prison",
            "ice_shard",
            "ice_sheet",
            "hyper_cooling",
            "absolute_zero");

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.PHI_GLASS_PLAIN);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        float bonus = BalanceConfig.GLASS_PLAIN_PHI_BONUS.get().floatValue();
        if (PhiGlassStormService.isStorming(level, pos)) {
            bonus += BalanceConfig.GLASS_PLAIN_STORM_PHI_BONUS.get().floatValue();
        }
        // Noon heat shimmer
        long tod = level.getDayTime() % 24000L;
        if (tod > 4000L && tod < 8000L) {
            bonus += 0.15f;
        }
        return bonus;
    }

    public static float spellPowerMultiplier(Level level, Vec3 position, SpellDefinition spell) {
        if (!isIn(level, position) || spell == null) {
            return 1f;
        }
        MagicSchool school = spell.requiredSchool();
        String path = spell.id() != null ? spell.id().getPath() : "";
        if (school == MagicSchool.ELEMENTAL) {
            if (WATERISH.contains(path)) {
                return BalanceConfig.GLASS_PLAIN_WATER_SPELL_MULT.get().floatValue();
            }
            return BalanceConfig.GLASS_PLAIN_ELEMENTAL_SPELL_MULT.get().floatValue();
        }
        if (school == MagicSchool.SPATIAL) {
            float mult = BalanceConfig.GLASS_PLAIN_SPATIAL_SPELL_MULT.get().floatValue();
            if (PhiGlassStormService.isStorming(level, BlockPos.containing(position))) {
                mult *= 0.92f;
            }
            return mult;
        }
        return 1f;
    }

    /** Extra entropy / jitter chance for spatial casts during storms. */
    public static float spatialStormEntropyBonus(Level level, Vec3 position, MagicSchool school) {
        if (school != MagicSchool.SPATIAL
                || !isIn(level, position)
                || !PhiGlassStormService.isStorming(level, BlockPos.containing(position))) {
            return 0f;
        }
        return BalanceConfig.GLASS_PLAIN_SPATIAL_STORM_ENTROPY.get().floatValue();
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide() || !isIn(player.level(), player.position())) {
            return;
        }
        PhiGlassStormService.tickPlayer(player);
        // Mild dehydration outside storms
        if (player.tickCount % 100 == 0 && !PhiGlassStormService.isStorming(player.level(), player.blockPosition())) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 0, false, false, true));
        }
    }
}
