package com.effecoria.world;

import com.effecoria.block.PhiGeyserPhase;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Φ-fog density and exposure for Essence Plateau — atmospheric essonite mist, not water vapor.
 */
public final class PhiFogService {
    private PhiFogService() {}

    public enum Density {
        NONE(0),
        HAZE(1),
        DENSE(2),
        STORM(3);

        private final int level;

        Density(int level) {
            this.level = level;
        }

        public int level() {
            return level;
        }
    }

    private static final Map<UUID, Integer> EXPOSURE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, CachedDensity> DENSITY_CACHE = new ConcurrentHashMap<>();

    private record CachedDensity(long gameTime, Density density) {}

    public static Density densityAt(Level level, BlockPos pos) {
        return computeDensity(level, pos);
    }

    public static Density densityAt(Player player) {
        Level level = player.level();
        long now = level.getGameTime();
        CachedDensity cached = DENSITY_CACHE.get(player.getUUID());
        if (cached != null && now - cached.gameTime() < 10) {
            return cached.density();
        }
        Density density = computeDensity(level, BlockPos.containing(player.getEyePosition()));
        DENSITY_CACHE.put(player.getUUID(), new CachedDensity(now, density));
        return density;
    }

    private static Density computeDensity(Level level, BlockPos pos) {
        if (!EssencePlateauService.isBiome(level, pos)) {
            return Density.NONE;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        int y = pos.getY();
        // Deep underground / far above sky islands: no atmospheric mist
        if (y < surfaceY - 48 || y > surfaceY + 40) {
            return Density.NONE;
        }

        int density = BalanceConfig.PHI_FOG_BASE_DENSITY.get();

        if (isValleyHollow(level, pos, surfaceY)) {
            density += 1;
        }
        if (nearGeyser(level, pos, BalanceConfig.PHI_FOG_GEYSER_RADIUS.get())) {
            density += 1;
        }
        boolean storm = (level.isThundering() && BalanceConfig.PHI_FOG_STORM_ENABLED.get())
                || com.effecoria.world.weather.PhiWeatherService.isStormActive(level, pos);
        if (storm) {
            density = Math.max(density, Density.STORM.level());
        } else {
            density = Math.min(density, Density.DENSE.level());
        }

        density = Mth.clamp(density, 0, 3);
        return switch (density) {
            case 0 -> Density.NONE;
            case 1 -> Density.HAZE;
            case 2 -> Density.DENSE;
            default -> Density.STORM;
        };
    }

    private static boolean isValleyHollow(Level level, BlockPos pos, int surfaceY) {
        if (pos.getY() > surfaceY - 4) {
            return false;
        }
        int x = pos.getX();
        int z = pos.getZ();
        int n = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z - 8);
        int s = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z + 8);
        int e = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + 8, z);
        int w = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x - 8, z);
        int ridge = Math.max(Math.max(n, s), Math.max(e, w));
        return ridge - pos.getY() >= 10;
    }

    private static boolean nearGeyser(Level level, BlockPos pos, int radius) {
        int r = Math.max(2, radius);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 3; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) {
                        continue;
                    }
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.PHI_GEYSER.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Multiplier applied on top of plateau regen (1 = no fog). */
    public static float regenMultiplier(Player player) {
        Density density = densityAt(player);
        return switch (density) {
            case NONE -> 1f;
            case HAZE -> BalanceConfig.PHI_FOG_REGEN_HAZE.get().floatValue();
            case DENSE -> BalanceConfig.PHI_FOG_REGEN_DENSE.get().floatValue();
            case STORM -> BalanceConfig.PHI_FOG_REGEN_STORM.get().floatValue();
        };
    }

    public static float fogFarPlane(Density density) {
        return switch (density) {
            case NONE -> 0f;
            case HAZE -> BalanceConfig.PHI_FOG_FAR_HAZE.get().floatValue();
            case DENSE -> BalanceConfig.PHI_FOG_FAR_DENSE.get().floatValue();
            case STORM -> BalanceConfig.PHI_FOG_FAR_STORM.get().floatValue();
        };
    }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Density density = densityAt(serverPlayer);
        if (density == Density.NONE) {
            EXPOSURE_TICKS.remove(serverPlayer.getUUID());
            return;
        }

        int exposure = EXPOSURE_TICKS.merge(serverPlayer.getUUID(), 1, Integer::sum);
        PlayerPsiData data = PsiHelper.get(serverPlayer);

        if (density == Density.STORM && data.initiated() && serverPlayer.tickCount % 20 == 0) {
            float drain = BalanceConfig.PHI_FOG_STORM_PSI_DRAIN.get().floatValue();
            data.setCurrentPsi(Math.max(0f, data.currentPsi() - drain));
            ExhaustionService.addExhaustion(data, BalanceConfig.PHI_FOG_STORM_EXHAUSTION.get().floatValue());
            PsiHelper.set(serverPlayer, data);
        }

        if (data.initiated()) {
            // Mild Φ-intoxication after prolonged haze (hours in lore → minutes in-game)
            int mageThreshold = BalanceConfig.PHI_FOG_MAGE_INTOX_TICKS.get();
            if (exposure >= mageThreshold && serverPlayer.tickCount % 80 == 0) {
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, false, false, true));
                if (exposure >= mageThreshold * 2) {
                    ExhaustionService.addExhaustion(data, 1.5f);
                    PsiHelper.set(serverPlayer, data);
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, true));
                }
            }
        } else if (serverPlayer.tickCount % 40 == 0) {
            int nonMage = BalanceConfig.PHI_FOG_NON_MAGE_INTOX_TICKS.get();
            if (exposure >= nonMage) {
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, false, true));
            }
            if (density.level() >= Density.DENSE.level() && exposure >= nonMage) {
                // Hallucination cue — brief darkness pulses
                if (serverPlayer.getRandom().nextFloat() < 0.25f) {
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, true));
                }
            }
        }
    }

    public static void clearPlayer(UUID id) {
        EXPOSURE_TICKS.remove(id);
        DENSITY_CACHE.remove(id);
    }

    /** Geyser attractor: erupting geysers thicken local fog for clients querying nearby. */
    public static boolean isEruptingGeyserNearby(Level level, Vec3 pos, double radius) {
        BlockPos center = BlockPos.containing(pos);
        int r = Mth.ceil(radius);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 4; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(ModBlocks.PHI_GEYSER.get())
                            && state.getValue(com.effecoria.block.PhiGeyserBlock.PHASE) == PhiGeyserPhase.ERUPTING) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
