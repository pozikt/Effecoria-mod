package com.effecoria.world;

import com.effecoria.EffecoriaMod;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gameplay modifiers inside the Essence Plateau biome: gravity, Φ exposure, mage buffs / non-mage strain.
 */
public final class EssencePlateauService {
    private EssencePlateauService() {}

    private static final ResourceLocation GRAVITY_ID = EffecoriaMod.id("essence_plateau_gravity");

    private static final Map<UUID, Integer> PLATEAU_TICKS = new ConcurrentHashMap<>();

    /**
     * True on the mountain surface biome, or anywhere in the vertical column beneath it
     * (caves stay “plateau” for gameplay even when 3D underground biomes differ).
     */
    public static boolean isBiome(Level level, BlockPos pos) {
        if (level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU)) {
            return true;
        }
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (pos.getY() > surfaceY + 32) {
            return false;
        }
        BlockPos surfacePos = new BlockPos(pos.getX(), Math.max(surfaceY, level.getMinBuildHeight()), pos.getZ());
        return level.getBiome(surfacePos).is(ModBiomeTags.ESSENCE_PLATEAU);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        float bonus = BalanceConfig.PLATEAU_PHI_BONUS.get().floatValue();
        int y = pos.getY();
        if (y <= BalanceConfig.PLATEAU_ROOT_MAX_Y.get()) {
            bonus += BalanceConfig.PLATEAU_ROOT_PHI_BONUS.get().floatValue();
        } else if (y <= BalanceConfig.PLATEAU_CAVE_MAX_Y.get()) {
            bonus += BalanceConfig.PLATEAU_CAVE_PHI_BONUS.get().floatValue();
        }
        return bonus;
    }

    /** Depth band across full world height (−64…320). */
    public static PlateauLayer layerAt(int y) {
        if (y <= BalanceConfig.PLATEAU_ROOT_MAX_Y.get()) {
            return PlateauLayer.ROOT;
        }
        if (y <= BalanceConfig.PLATEAU_CAVE_MAX_Y.get()) {
            return PlateauLayer.CAVE;
        }
        if (y >= BalanceConfig.PLATEAU_SKY_MIN_Y.get()) {
            return PlateauLayer.SKY;
        }
        if (y <= BalanceConfig.PLATEAU_CRUST_MAX_Y.get()) {
            return PlateauLayer.CRUST;
        }
        return PlateauLayer.SURFACE;
    }

    public enum PlateauLayer {
        SKY,
        SURFACE,
        CRUST,
        CAVE,
        ROOT
    }

    public static float spellPowerMultiplier(Level level, Vec3 position) {
        if (!isIn(level, position)) {
            return 1f;
        }
        return BalanceConfig.PLATEAU_SPELL_POWER_MULT.get().floatValue();
    }

    public static float spellCostMultiplier(Level level, Vec3 position) {
        if (!isIn(level, position)) {
            return 1f;
        }
        return BalanceConfig.PLATEAU_SPELL_COST_MULT.get().floatValue();
    }

    public static float regenMultiplier(Level level, Vec3 position) {
        if (!isIn(level, position)) {
            return 1f;
        }
        return BalanceConfig.PLATEAU_REGEN_MULT.get().floatValue();
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        boolean inside = isIn(player.level(), player.position());
        if (inside) {
            applyGravity(player);
            tickExposure(player);
        } else {
            clearGravity(player);
            PLATEAU_TICKS.remove(player.getUUID());
        }
    }

    private static void applyGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        double target = BalanceConfig.PLATEAU_GRAVITY_MULT.get();
        if (gravity.getModifier(GRAVITY_ID) == null) {
            gravity.addTransientModifier(
                    new AttributeModifier(GRAVITY_ID, target - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void clearGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(GRAVITY_ID);
        }
    }

    private static void tickExposure(Player player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        UUID id = player.getUUID();
        int ticks = PLATEAU_TICKS.getOrDefault(id, 0) + 20;
        PLATEAU_TICKS.put(id, ticks);

        var data = PsiHelper.get(player);
        if (data.initiated()) {
            if (ticks % 600 == 0 && !hasPhiProtection(player)) {
                ExhaustionService.addExhaustion(data, BalanceConfig.PLATEAU_EXHAUSTION_SPIKE.get().floatValue());
                PsiHelper.set(player, data);
            }
        } else if (ticks % 400 == 0) {
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, true));
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.CONFUSION, 100, 1, false, false, true));
        }

        int burnInterval = BalanceConfig.PLATEAU_BURN_INTERVAL_TICKS.get();
        if (burnInterval > 0 && ticks >= burnInterval && ticks % burnInterval == 0 && !hasPhiProtection(player)) {
            float damage = BalanceConfig.PLATEAU_BURN_DAMAGE.get().floatValue();
            if (damage > 0f) {
                player.hurt(player.damageSources().magic(), damage);
            }
            if (data.initiated()) {
                ExhaustionService.addExhaustion(data, BalanceConfig.PLATEAU_BURN_EXHAUSTION.get().floatValue());
                PsiHelper.set(player, data);
            }
        }

        // Φ-root: lethal radiation without protection
        if (player.getY() <= BalanceConfig.PLATEAU_ROOT_MAX_Y.get() && !hasPhiProtection(player)) {
            float rootDmg = BalanceConfig.PLATEAU_ROOT_RADIATION_DAMAGE.get().floatValue();
            if (rootDmg > 0f) {
                player.hurt(player.damageSources().magic(), rootDmg);
            }
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false, true));
            if (data.initiated()) {
                ExhaustionService.addExhaustion(data, 4f);
                PsiHelper.set(player, data);
            }
        }
    }

    /** Gold / charged Φ-cell / resonance focus — partial Φ shielding from overexposure. */
    public static boolean hasPhiProtection(Player player) {
        if (PhiHarnessItems.bestFocusTier(player) > 0) {
            return true;
        }
        if (!PhiHarnessItems.findPhiCell(player).isEmpty()) {
            return true;
        }
        for (var stack : player.getInventory().items) {
            if (stack.is(ModItems.ESSENITE_DUST.get())) {
                return true;
            }
        }
        for (var slot : player.getArmorSlots()) {
            if (isGoldIsolation(slot)) {
                return true;
            }
        }
        return isGoldIsolation(player.getMainHandItem()) || isGoldIsolation(player.getOffhandItem());
    }

    private static boolean isGoldIsolation(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && "minecraft".equals(key.getNamespace()) && key.getPath().startsWith("gold_");
    }
}
