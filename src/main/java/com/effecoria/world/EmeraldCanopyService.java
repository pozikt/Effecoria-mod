package com.effecoria.world;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.EffecoriaMod;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Emerald Canopy — giant Φ-forest: low gravity, dense mist, Forest Mind anger.
 */
public final class EmeraldCanopyService {
    private static final ResourceLocation GRAVITY_ID = EffecoriaMod.id("emerald_canopy_gravity");
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    /** Chunk-ish anger keyed by section (x>>4, z>>4) packed. */
    private static final Map<Long, Float> FOREST_ANGER = new ConcurrentHashMap<>();
    private static final Set<UUID> FOREST_HARMERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_WARN_MS = new ConcurrentHashMap<>();

    private EmeraldCanopyService() {}

    public static boolean isBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomeTags.EMERALD_CANOPY);
    }

    public static boolean isIn(Level level, Vec3 position) {
        return isBiome(level, BlockPos.containing(position));
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        if (!isBiome(level, pos)) {
            return 0f;
        }
        float bonus = BalanceConfig.EMERALD_CANOPY_PHI_BONUS.get().floatValue();
        float anger = angerAt(pos);
        if (anger >= BalanceConfig.EMERALD_CANOPY_MIND_HOSTILE_THRESHOLD.get()) {
            bonus -= 0.25f;
        }
        return bonus;
    }

    public static float angerAt(BlockPos pos) {
        return FOREST_ANGER.getOrDefault(chunkKey(pos), 0f);
    }

    public static boolean isHostileMind(BlockPos pos) {
        return angerAt(pos) >= BalanceConfig.EMERALD_CANOPY_MIND_HOSTILE_THRESHOLD.get();
    }

    public static void onForestHarm(Level level, BlockPos pos, float amount) {
        if (!(level instanceof ServerLevel) || !isBiome(level, pos)) {
            return;
        }
        long key = chunkKey(pos);
        FOREST_ANGER.merge(key, amount, Float::sum);
    }

    public static boolean hasHarmedForest(Player player) {
        return FOREST_HARMERS.contains(player.getUUID());
    }

    public static void markForestHarmer(Player player) {
        if (player != null) {
            FOREST_HARMERS.add(player.getUUID());
        }
    }

    public static void onBlockBroken(Level level, BlockPos pos, BlockState state) {
        onBlockBroken(level, pos, state, null);
    }

    public static void onBlockBroken(Level level, BlockPos pos, BlockState state, Player breaker) {
        if (!isBiome(level, pos)) {
            return;
        }
        float harm = 0f;
        if (state.is(ModBlocks.ANCIENT_ESSENCE_WOOD.get()) || state.is(ModBlocks.GOLDEN_BARK.get())) {
            harm = 3.0f;
        } else if (state.is(ModBlocks.PHI_LOG.get()) || state.is(ModBlocks.PHI_PLANKS.get())) {
            harm = 1.5f;
        } else if (state.is(ModBlocks.PHI_LEAVES.get())) {
            harm = 0.15f;
        } else if (state.is(ModBlocks.PHI_SNARE_VINE.get()) || state.is(ModBlocks.PHI_BLADES.get())) {
            harm = 0.4f;
        }
        if (harm > 0f) {
            onForestHarm(level, pos, harm);
            markForestHarmer(breaker);
        }
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        if (!isIn(player.level(), player.position())) {
            clearGravity(player);
            WARNED.remove(id);
            return;
        }

        applyGravity(player);
        PhiFogService.tickPlayer(player);

        if (player instanceof ServerPlayer serverPlayer && WARNED.putIfAbsent(id, Boolean.TRUE) == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.emerald_canopy_enter"), true);
        }

        if (player.tickCount % 20 == 0) {
            decayAngerNear(player.blockPosition());
            tickForestMind(player);
        }
    }

    public static void clearPlayer(UUID id) {
        WARNED.remove(id);
        LAST_WARN_MS.remove(id);
        FOREST_HARMERS.remove(id);
    }

    private static void tickForestMind(Player player) {
        float anger = angerAt(player.blockPosition());
        float warn = BalanceConfig.EMERALD_CANOPY_MIND_WARN_THRESHOLD.get().floatValue();
        if (anger < warn) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_WARN_MS.get(player.getUUID());
        if (last != null && now - last < 12_000L) {
            return;
        }
        LAST_WARN_MS.put(player.getUUID(), now);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.emerald_canopy_mind_whisper"), true);
        }
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
        if (anger >= BalanceConfig.EMERALD_CANOPY_MIND_HOSTILE_THRESHOLD.get()) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, true));
        }
    }

    private static void decayAngerNear(BlockPos pos) {
        long key = chunkKey(pos);
        Float anger = FOREST_ANGER.get(key);
        if (anger == null) {
            return;
        }
        float next = anger - BalanceConfig.EMERALD_CANOPY_MIND_ANGER_DECAY.get().floatValue();
        if (next <= 0.05f) {
            FOREST_ANGER.remove(key);
        } else {
            FOREST_ANGER.put(key, next);
        }
    }

    private static void applyGravity(Player player) {
        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        double target = BalanceConfig.EMERALD_CANOPY_GRAVITY_MULT.get();
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

    private static long chunkKey(BlockPos pos) {
        return (((long) (pos.getX() >> 4)) << 32) | ((pos.getZ() >> 4) & 0xffffffffL);
    }
}
