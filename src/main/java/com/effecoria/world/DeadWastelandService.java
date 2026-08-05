package com.effecoria.world;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zero Φ-flow Dead Wasteland — Φ_nature ≈ 0. Magic sleeps; cells and crystals bleed charge.
 */
public final class DeadWastelandService {
    private DeadWastelandService() {}

    private static final Map<UUID, Integer> STAY_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> COMA_WARNED = new ConcurrentHashMap<>();

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.DEAD_WASTELAND);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (!isIn(player.level(), player.position())) {
            STAY_TICKS.remove(id);
            WARNED.remove(id);
            COMA_WARNED.remove(id);
            return;
        }

        int ticks = STAY_TICKS.getOrDefault(id, 0) + 1;
        STAY_TICKS.put(id, ticks);

        if (player instanceof ServerPlayer serverPlayer && WARNED.putIfAbsent(id, Boolean.TRUE) == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.dead_wasteland_enter"), true);
        }

        // Strip seepage near the player (small budget; never on chunk load).
        if (ticks % 80 == 0 && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            DeadWastelandHydrology.dryAround(serverLevel, player.blockPosition(), 6, 4);
        }

        // Every second: drain Φ gear and apply mood/body strain.
        if (ticks % 20 != 0) {
            return;
        }

        drainArtifacts(player);
        var data = PsiHelper.get(player);
        if (data.initiated()) {
            tickMageExposure(player, data, ticks);
        } else {
            tickNonMageExposure(player);
        }
    }

    private static void drainArtifacts(Player player) {
        float cellDrain = BalanceConfig.WASTELAND_CELL_DRAIN_PER_SECOND.get().floatValue();
        if (cellDrain > 0f) {
            ItemStack cell = PhiHarnessItems.findPhiCell(player);
            if (!cell.isEmpty()) {
                float charge = PhiHarnessItems.cellCharge(cell);
                if (charge > 0f) {
                    PhiHarnessItems.setCellCharge(cell, Math.max(0f, charge - cellDrain));
                }
            }
        }

        // Essonite crystals / dust slowly lose coherence (inventory bleed).
        if (player.getRandom().nextFloat() < BalanceConfig.WASTELAND_CRYSTAL_BLEED_CHANCE.get()) {
            bleedStack(player, new ItemStack(ModItems.ESSENITE_DUST.get()));
            bleedStack(player, new ItemStack(ModItems.ESSONITE_CRYSTAL.get()));
            bleedStack(player, new ItemStack(ModItems.ESSONITE_SHARD.get()));
        }
    }

    private static void bleedStack(Player player, ItemStack prototype) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(prototype.getItem()) && !stack.isEmpty()) {
                stack.shrink(1);
                return;
            }
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(prototype.getItem()) && !off.isEmpty()) {
            off.shrink(1);
        }
    }

    private static void tickMageExposure(Player player, com.effecoria.core.psi.PlayerPsiData data, int stayTicks) {
        // Orkanum “sleeps”: mild weakness while awake; after threshold → coma-like lock.
        int comaAfter = BalanceConfig.WASTELAND_MAGE_COMA_TICKS.get();
        if (stayTicks >= comaAfter) {
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, true));
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 5, false, false, true));
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 3, false, false, true));
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.WEAKNESS, 80, 2, false, false, true));
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
            if (player instanceof ServerPlayer serverPlayer
                    && COMA_WARNED.putIfAbsent(player.getUUID(), Boolean.TRUE) == null) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.effecoria.dead_wasteland_coma"), true);
            }
        } else {
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
            // Soft Ψ decay — cannot recover here anyway under zero-flux.
            if (data.currentPsi() > 0f) {
                data.setCurrentPsi(Math.max(0f, data.currentPsi() - 0.35f));
                PsiHelper.set(player, data);
            }
        }
    }

    private static void tickNonMageExposure(Player player) {
        BreathDebuffs.apply(
                player, new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
        BreathDebuffs.apply(
                player, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        BreathDebuffs.apply(
                player, new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, false, true));
        if (player.tickCount % 200 == 0) {
            BreathDebuffs.apply(
                    player, new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
        }
    }
}
