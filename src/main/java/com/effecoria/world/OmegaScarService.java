package com.effecoria.world;

import com.effecoria.EffecoriaMod;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ω-Scar — active wound in spacetime. Causality echoes, unstable gravity, Ω-entropy,
 * necro/corruption affinity; ambient Ω fog/rain via {@link com.effecoria.world.weather.PhiWeatherService}.
 */
public final class OmegaScarService {
    private OmegaScarService() {}

    private static final ResourceLocation GRAVITY_ID = EffecoriaMod.id("omega_scar_gravity");

    private static final Map<UUID, Integer> STAY_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> GRAVITY_MULT = new ConcurrentHashMap<>();

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.OMEGA_SCAR);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static boolean favorsOmegaSchool(MagicSchool school) {
        return school == MagicSchool.NECROMANCY || school == MagicSchool.CORRUPTION;
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        return BalanceConfig.OMEGA_SCAR_PHI_BONUS.get().floatValue();
    }

    /** Necro / Corruption surge; other schools thin under Ω noise. */
    public static float spellPowerMultiplier(Player player) {
        if (!isIn(player.level(), player.position())) {
            return 1f;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            return 1f;
        }
        if (favorsOmegaSchool(data.school())) {
            return BalanceConfig.OMEGA_SCAR_FAVORED_POWER.get().floatValue();
        }
        return BalanceConfig.OMEGA_SCAR_OTHER_POWER.get().floatValue();
    }

    public static float spellCostMultiplier(Player player) {
        if (!isIn(player.level(), player.position())) {
            return 1f;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (data.initiated() && favorsOmegaSchool(data.school())) {
            return BalanceConfig.OMEGA_SCAR_FAVORED_COST.get().floatValue();
        }
        return 1f;
    }

    /** Cast chaos for non-favored schools inside the Scar (stacks conceptually with weather). */
    public static float castChaosChance(Player player) {
        if (!isIn(player.level(), player.position())) {
            return 0f;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated() || favorsOmegaSchool(data.school())) {
            return 0f;
        }
        return BalanceConfig.OMEGA_SCAR_CAST_CHAOS.get().floatValue();
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (!isIn(player.level(), player.position())) {
            clearGravity(player);
            STAY_TICKS.remove(id);
            WARNED.remove(id);
            GRAVITY_MULT.remove(id);
            return;
        }

        int ticks = STAY_TICKS.getOrDefault(id, 0) + 1;
        STAY_TICKS.put(id, ticks);

        if (player instanceof ServerPlayer serverPlayer && WARNED.putIfAbsent(id, Boolean.TRUE) == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.omega_scar_enter"), true);
        }

        tickGravity(player, ticks);
        tickExposure(player, ticks);
        maybeTimeLoop(player, ticks);
    }

    private static void tickGravity(Player player, int stayTicks) {
        if (stayTicks % BalanceConfig.OMEGA_SCAR_GRAVITY_PERIOD.get() != 0) {
            applyGravity(player, GRAVITY_MULT.getOrDefault(player.getUUID(), 1.0));
            return;
        }
        // Unstable: lighter or heavier than material law allows.
        double mult = player.getRandom().nextBoolean()
                ? BalanceConfig.OMEGA_SCAR_GRAVITY_LIGHT.get()
                : BalanceConfig.OMEGA_SCAR_GRAVITY_HEAVY.get();
        GRAVITY_MULT.put(player.getUUID(), mult);
        applyGravity(player, mult);
    }

    private static void applyGravity(Player player, double mult) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        gravity.removeModifier(GRAVITY_ID);
        gravity.addTransientModifier(
                new AttributeModifier(GRAVITY_ID, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void clearGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(GRAVITY_ID);
        }
    }

    private static void tickExposure(Player player, int stayTicks) {
        if (stayTicks % 40 != 0) {
            return;
        }
        var data = PsiHelper.get(player);
        if (!data.initiated()) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
            if (stayTicks >= 160) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
            }
            if (stayTicks >= 400) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 0, false, false, true));
            }
            return;
        }

        float pulse = BalanceConfig.OMEGA_SCAR_ENTROPY_PULSE.get().floatValue();
        if (favorsOmegaSchool(data.school())) {
            pulse *= 0.35f;
        } else {
            pulse *= 1.35f;
        }
        if (stayTicks >= 200) {
            data.setEntropyB(data.entropyB() + pulse);
            PsiHelper.set(player, data);
        }
        if (!favorsOmegaSchool(data.school()) && stayTicks >= 600 && stayTicks % 80 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 50, 0, false, false, true));
        }
    }

    private static void maybeTimeLoop(Player player, int stayTicks) {
        if (stayTicks < 400 || stayTicks % 100 != 0) {
            return;
        }
        if (player.getRandom().nextFloat() >= BalanceConfig.OMEGA_SCAR_LOOP_CHANCE.get().floatValue()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 50, 1, false, false, true));
        // Micro-rewind feel: damp velocity and nudge slightly back along look.
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = true;
        player.teleportTo(
                player.getX() - look.x * 0.8,
                player.getY(),
                player.getZ() - look.z * 0.8);
    }

    public static void clearPlayer(UUID id) {
        STAY_TICKS.remove(id);
        WARNED.remove(id);
        GRAVITY_MULT.remove(id);
    }
}
