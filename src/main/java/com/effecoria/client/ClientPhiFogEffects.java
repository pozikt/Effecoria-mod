package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.PhiFogService;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * World-space Φ-mist banks (visible from outside) + immersive camera fog when inside / approaching.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientPhiFogEffects {
    private static final float APPROACH_RANGE = 40f;
    private static final int[] SAMPLE_DISTANCES = {10, 18, 28, 40, 56, 72, 88};
    private static final int SAMPLE_ANGLES = 10;

    /** 0 = clear air, 1 = fully immersed in plateau mist. */
    private static float immersion;
    private static PhiFogService.Density localDensity = PhiFogService.Density.NONE;
    private static float nearestFogDist = APPROACH_RANGE;

    private ClientPhiFogEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            immersion = 0f;
            localDensity = PhiFogService.Density.NONE;
            return;
        }

        Level level = minecraft.level;
        RandomSource random = level.random;
        Vec3 eye = player.getEyePosition(1f);

        localDensity = PhiFogService.densityAt(player);
        nearestFogDist = APPROACH_RANGE;

        // Always paint distant mist banks so the plateau reads from outside.
        if (player.tickCount % 2 == 0) {
            spawnWorldBanks(level, player, eye, random);
        }

        if (localDensity != PhiFogService.Density.NONE) {
            nearestFogDist = 0f;
            spawnLocalVolume(level, player, eye, random, localDensity);
            spawnWake(level, player, eye, random);
        }

        float targetImmersion;
        if (localDensity != PhiFogService.Density.NONE) {
            targetImmersion = switch (localDensity) {
                case HAZE -> 0.55f;
                case DENSE -> 0.85f;
                case STORM -> 1f;
                default -> 0f;
            };
        } else {
            // Soft approach: fog plane thickens before you step in
            targetImmersion = Mth.clamp(1f - nearestFogDist / APPROACH_RANGE, 0f, 0.45f);
        }
        immersion = Mth.lerp(0.18f, immersion, targetImmersion);
        if (immersion < 0.02f) {
            immersion = 0f;
        }
    }

    private static void spawnWorldBanks(Level level, LocalPlayer player, Vec3 eye, RandomSource random) {
        BlockPos origin = player.blockPosition();
        int angleOffset = (player.tickCount / 2) % SAMPLE_ANGLES;

        for (int dist : SAMPLE_DISTANCES) {
            for (int a = 0; a < SAMPLE_ANGLES; a++) {
                // Rotate samples over ticks so the bank fills without a fixed grid look
                double angle = ((a + angleOffset) / (double) SAMPLE_ANGLES) * Math.PI * 2.0
                        + (random.nextDouble() - 0.5) * 0.15;
                int x = origin.getX() + Mth.floor(Math.cos(angle) * dist);
                int z = origin.getZ() + Mth.floor(Math.sin(angle) * dist);
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }

                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                // Mist lakes hug the ground / hollows; sample mid-bank height
                BlockPos sample = new BlockPos(x, surfaceY + 2, z);
                PhiFogService.Density density = PhiFogService.densityAt(level, sample);
                if (density == PhiFogService.Density.NONE) {
                    continue;
                }

                double dx = x + 0.5 - eye.x;
                double dz = z + 0.5 - eye.z;
                float horiz = (float) Math.sqrt(dx * dx + dz * dz);
                if (localDensity == PhiFogService.Density.NONE) {
                    nearestFogDist = Math.min(nearestFogDist, horiz);
                }

                int puffs = switch (density) {
                    case HAZE -> 2;
                    case DENSE -> 5;
                    case STORM -> 7;
                    default -> 0;
                };
                // Far banks need more / higher puffs to read at distance
                if (dist >= 40) {
                    puffs += 2;
                }

                float bankBottom = surfaceY + 0.4f;
                float bankTop = surfaceY + (density == PhiFogService.Density.DENSE ? 5.5f : 3.5f);
                if (density == PhiFogService.Density.STORM) {
                    bankTop = surfaceY + 8f;
                }

                for (int i = 0; i < puffs; i++) {
                    double px = x + 0.5 + (random.nextDouble() - 0.5) * 6.5;
                    double pz = z + 0.5 + (random.nextDouble() - 0.5) * 6.5;
                    double py = Mth.lerp(random.nextDouble(), bankBottom, bankTop);
                    // Slow Φ-gradient drift (not wind)
                    double vx = (random.nextDouble() - 0.5) * 0.015;
                    double vy = 0.004 + random.nextDouble() * 0.012;
                    double vz = (random.nextDouble() - 0.5) * 0.015;
                    level.addParticle(ModParticleTypes.PHI_MIST.get(), px, py, pz, vx, vy, vz);
                    if (random.nextFloat() < 0.2f * density.level()) {
                        level.addParticle(
                                ModParticleTypes.PHI_SPARK.get(),
                                px,
                                py + 0.4,
                                pz,
                                0,
                                0.02,
                                0);
                    }
                }
            }
        }
    }

    private static void spawnLocalVolume(
            Level level, LocalPlayer player, Vec3 eye, RandomSource random, PhiFogService.Density density) {
        int mistCount = switch (density) {
            case HAZE -> 3;
            case DENSE -> 7;
            case STORM -> 12;
            default -> 0;
        };
        for (int i = 0; i < mistCount; i++) {
            double ox = (random.nextDouble() - 0.5) * 18;
            double oy = (random.nextDouble() - 0.25) * 6;
            double oz = (random.nextDouble() - 0.5) * 18;
            level.addParticle(
                    ModParticleTypes.PHI_MIST.get(),
                    eye.x + ox,
                    eye.y + oy - 1.0,
                    eye.z + oz,
                    (random.nextDouble() - 0.5) * 0.02,
                    0.01 + random.nextDouble() * 0.02,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        int sparks = density.level() + (density == PhiFogService.Density.STORM ? 3 : 0);
        for (int i = 0; i < sparks; i++) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    eye.x + (random.nextDouble() - 0.5) * 12,
                    eye.y + (random.nextDouble() - 0.5) * 5,
                    eye.z + (random.nextDouble() - 0.5) * 12,
                    0,
                    0.02,
                    0);
        }
    }

    private static void spawnWake(Level level, LocalPlayer player, Vec3 eye, RandomSource random) {
        Vec3 motion = player.getDeltaMovement();
        if (motion.horizontalDistanceSqr() <= 0.002) {
            return;
        }
        Vec3 behind = eye.subtract(player.getLookAngle().scale(0.7)).add(0, -0.35, 0);
        for (int i = 0; i < 3; i++) {
            level.addParticle(
                    ModParticleTypes.PHI_MIST.get(),
                    behind.x + (random.nextDouble() - 0.5) * 0.7,
                    behind.y,
                    behind.z + (random.nextDouble() - 0.5) * 0.7,
                    -motion.x * 0.35,
                    0.02,
                    -motion.z * 0.35);
        }
        if (random.nextFloat() < 0.4f) {
            level.addParticle(ModParticleTypes.PHI_SPARK.get(), behind.x, behind.y + 0.25, behind.z, 0, 0.05, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || immersion <= 0.02f) {
            return;
        }

        PhiFogService.Density density =
                localDensity != PhiFogService.Density.NONE ? localDensity : PhiFogService.Density.HAZE;
        float targetFar = PhiFogService.fogFarPlane(density);
        if (targetFar <= 0f) {
            return;
        }

        // Outside: long view with soft haze; inside: tight plateau fog
        float clearFar = 110f;
        float far = Mth.lerp(immersion, clearFar, targetFar);
        float near = Mth.lerp(immersion, 4f, density == PhiFogService.Density.STORM ? 0.2f : 0.45f);

        float dayFactor = minecraft.level.getSkyDarken(1f);
        if (dayFactor > 0.2f) {
            far *= Mth.lerp(Mth.clamp(dayFactor, 0f, 1f), 1f, 0.75f);
        }

        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(Math.max(3f, far));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || immersion <= 0.02f) {
            return;
        }
        PhiFogService.Density density =
                localDensity != PhiFogService.Density.NONE ? localDensity : PhiFogService.Density.HAZE;
        float t = (density.level() - 1) / 2f;
        float r = Mth.lerp(t, 0.12f, 0.18f);
        float g = Mth.lerp(t, 0.18f, 0.08f);
        float b = Mth.lerp(t, 0.55f, 0.42f);
        // Blend toward vanilla sky as immersion drops (approaching from outside)
        event.setRed(Mth.lerp(immersion, event.getRed(), r));
        event.setGreen(Mth.lerp(immersion, event.getGreen(), g));
        event.setBlue(Mth.lerp(immersion, event.getBlue(), b));
    }
}
