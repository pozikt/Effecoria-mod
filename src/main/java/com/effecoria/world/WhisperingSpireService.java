package com.effecoria.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.progression.ExhaustionService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Whispering Spire hazard zones (Minecraft-scaled) and Φ-whisper for initiated mages.
 *
 * <pre>
 * Black 0–R_black | Red …R_red | Yellow …R_yellow | Green …R_green
 * </pre>
 */
public final class WhisperingSpireService {
    public enum Zone {
        NONE,
        GREEN,
        YELLOW,
        RED,
        BLACK
    }

    private static final Map<ResourceKey<Level>, List<BlockPos>> SPIRES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> WHISPER_COOLDOWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Zone> LAST_ZONE = new ConcurrentHashMap<>();

    private static final String[] PHONEME_KEYS = {
        "message.effecoria.spire_whisper.1",
        "message.effecoria.spire_whisper.2",
        "message.effecoria.spire_whisper.3",
        "message.effecoria.spire_whisper.4",
        "message.effecoria.spire_whisper.5"
    };

    private WhisperingSpireService() {}

    public static void register(ServerLevel level, BlockPos pos) {
        List<BlockPos> list = SPIRES.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        synchronized (list) {
            BlockPos immutable = pos.immutable();
            if (!list.contains(immutable)) {
                list.add(immutable);
            }
        }
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        List<BlockPos> list = SPIRES.get(level.dimension());
        if (list == null) {
            return;
        }
        synchronized (list) {
            list.remove(pos.immutable());
            list.removeIf(p -> p.equals(pos));
        }
    }

    public static BlockPos nearestVent(Level level, BlockPos pos) {
        List<BlockPos> list = SPIRES.get(level.dimension());
        if (list == null || list.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        synchronized (list) {
            for (BlockPos vent : list) {
                double d = vent.distToCenterSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (d < bestDist) {
                    bestDist = d;
                    best = vent;
                }
            }
        }
        return best;
    }

    public static Zone zoneAt(Level level, BlockPos pos) {
        BlockPos vent = nearestVent(level, pos);
        if (vent == null) {
            return Zone.NONE;
        }
        double horiz = Math.sqrt(vent.distToCenterSqr(pos.getX() + 0.5, vent.getY() + 0.5, pos.getZ() + 0.5));
        if (horiz <= BalanceConfig.SPIRE_ZONE_BLACK.get()) {
            return Zone.BLACK;
        }
        if (horiz <= BalanceConfig.SPIRE_ZONE_RED.get()) {
            return Zone.RED;
        }
        if (horiz <= BalanceConfig.SPIRE_ZONE_YELLOW.get()) {
            return Zone.YELLOW;
        }
        if (horiz <= BalanceConfig.SPIRE_ZONE_GREEN.get()) {
            return Zone.GREEN;
        }
        return Zone.NONE;
    }

    public static float phiBonus(Level level, BlockPos pos) {
        return switch (zoneAt(level, pos)) {
            case GREEN -> BalanceConfig.SPIRE_PHI_GREEN.get().floatValue();
            case YELLOW -> BalanceConfig.SPIRE_PHI_YELLOW.get().floatValue();
            case RED -> BalanceConfig.SPIRE_PHI_RED.get().floatValue();
            case BLACK -> BalanceConfig.SPIRE_PHI_BLACK.get().floatValue();
            case NONE -> 0f;
        };
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player.tickCount % 10 != 0) {
            return;
        }
        Zone zone = zoneAt(player.level(), player.blockPosition());
        UUID id = player.getUUID();
        Zone prev = LAST_ZONE.put(id, zone);
        if (prev != zone && zone != Zone.NONE) {
            player.displayClientMessage(Component.translatable("message.effecoria.spire_zone." + zone.name().toLowerCase()), true);
        }
        if (zone == Zone.NONE) {
            WHISPER_COOLDOWN.remove(id);
            return;
        }

        PhiRadiationService.Shield shield = PhiRadiationService.evaluate(player);
        float remain = shield.remaining();
        boolean protected_ = shield.adequate();
        var data = PsiHelper.get(player);

        switch (zone) {
            case GREEN -> {
                if (data.initiated() && player.tickCount % 40 == 0) {
                    data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + 0.35f));
                    PsiHelper.set(player, data);
                } else if (!data.initiated() && player.tickCount % 100 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, false, false, true));
                }
            }
            case YELLOW -> {
                if (remain > 0.001f) {
                    if (!data.initiated()) {
                        player.hurt(
                                player.damageSources().magic(),
                                BalanceConfig.SPIRE_DMG_YELLOW.get().floatValue() * remain);
                        if (remain > 0.4f) {
                            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                        }
                    } else if (player.tickCount % 40 == 0) {
                        ExhaustionService.addExhaustion(data, 2.5f * remain);
                        PsiHelper.set(player, data);
                        player.hurt(
                                player.damageSources().magic(),
                                BalanceConfig.SPIRE_DMG_YELLOW.get().floatValue() * 0.5f * remain);
                    }
                }
                maybeWhisper(player, zone);
            }
            case RED -> {
                if (remain > 0.001f) {
                    player.hurt(
                            player.damageSources().magic(),
                            BalanceConfig.SPIRE_DMG_RED.get().floatValue() * remain);
                    if (!shield.omega() && remain > 0.25f) {
                        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false, true));
                    }
                    if (remain > 0.35f) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
                    }
                    if (data.initiated()) {
                        ExhaustionService.addExhaustion(data, 8f * remain);
                        PsiHelper.set(player, data);
                    }
                } else if (data.initiated() && player.tickCount % 20 == 0) {
                    ExhaustionService.addExhaustion(data, 1.5f);
                    PsiHelper.set(player, data);
                }
                maybeWhisper(player, zone);
            }
            case BLACK -> {
                // Soul-burn: protection only softens; still lethal without creative
                float dmg = BalanceConfig.SPIRE_DMG_BLACK.get().floatValue() * Math.max(0.2f, remain);
                if (protected_) {
                    dmg *= 0.5f;
                }
                if (!player.getAbilities().invulnerable) {
                    player.hurt(player.damageSources().magic(), dmg);
                    if (!shield.omega()) {
                        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 2, false, false, true));
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
                    if (data.initiated()) {
                        ExhaustionService.addExhaustion(data, 14f * Math.max(0.35f, remain));
                        data.setCurrentPsi(Math.max(0f, data.currentPsi() - 4f * Math.max(0.35f, remain)));
                        PsiHelper.set(player, data);
                    }
                }
                maybeWhisper(player, zone);
            }
            default -> {}
        }
    }

    private static void maybeWhisper(ServerPlayer player, Zone zone) {
        if (!PsiHelper.get(player).initiated()) {
            // non-mages feel infrasound as nausea / bone ache
            if (player.tickCount % 80 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, false, true));
            }
            return;
        }
        int cd = WHISPER_COOLDOWN.getOrDefault(player.getUUID(), 0) - 10;
        if (cd > 0) {
            WHISPER_COOLDOWN.put(player.getUUID(), cd);
            return;
        }
        int interval = switch (zone) {
            case YELLOW -> 240;
            case RED -> 140;
            case BLACK -> 80;
            default -> 400;
        };
        WHISPER_COOLDOWN.put(player.getUUID(), interval);
        String key = PHONEME_KEYS[player.getRandom().nextInt(PHONEME_KEYS.length)];
        player.displayClientMessage(Component.translatable(key), true);
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.AMBIENT,
                0.35f,
                0.6f);
    }

    /** Drop stale unloaded vents occasionally. */
    public static void prune(ServerLevel level) {
        List<BlockPos> list = SPIRES.get(level.dimension());
        if (list == null) {
            return;
        }
        synchronized (list) {
            Iterator<BlockPos> it = list.iterator();
            while (it.hasNext()) {
                BlockPos p = it.next();
                if (!level.isLoaded(p)) {
                    continue;
                }
                if (!(level.getBlockEntity(p) instanceof com.effecoria.block.WhisperingSpireVentBlockEntity)) {
                    it.remove();
                }
            }
        }
    }

    public static boolean isNearSpire(Level level, Vec3 pos, double maxHoriz) {
        BlockPos vent = nearestVent(level, BlockPos.containing(pos));
        if (vent == null) {
            return false;
        }
        double dx = vent.getX() + 0.5 - pos.x;
        double dz = vent.getZ() + 0.5 - pos.z;
        return dx * dx + dz * dz <= maxHoriz * maxHoriz;
    }
}
