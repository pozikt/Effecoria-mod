package com.effecoria.world;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Regional Φ-storms over the Glass Plain — dust abrasion, Φ-burn, spontaneous lightning.
 */
public final class PhiGlassStormService {
    private PhiGlassStormService() {}

    public static boolean isStorming(Level level, BlockPos pos) {
        if (!PhiGlassPlainService.isBiome(level, pos)) {
            return false;
        }
        int calm = BalanceConfig.GLASS_PLAIN_STORM_CALM_TICKS.get();
        int storm = BalanceConfig.GLASS_PLAIN_STORM_DURATION_TICKS.get();
        int cycle = Math.max(1, calm + storm);
        long seed = (pos.getX() >> 4) * 734287L + (pos.getZ() >> 4) * 912391L;
        long t = level.getGameTime() + seed;
        return (t % cycle) < storm;
    }

    public static void tickPlayer(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = player.blockPosition();
        if (!isStorming(level, pos)) {
            return;
        }

        // Abrasion + Φ-burn every second
        if (player.tickCount % 20 == 0) {
            float dmg = BalanceConfig.GLASS_PLAIN_STORM_ABRASION.get().floatValue();
            if (dmg > 0f && !player.isCreative() && !player.isSpectator()) {
                player.hurt(level.damageSources().magic(), dmg);
            }
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
            if (player instanceof ServerPlayer sp && PsiHelper.get(sp).initiated()) {
                var data = PsiHelper.get(sp);
                ExhaustionService.addExhaustion(data, BalanceConfig.GLASS_PLAIN_STORM_EXHAUSTION.get().floatValue());
                PsiHelper.set(sp, data);
            }
        }

        // Dust particles around player
        if (player.tickCount % 4 == 0) {
            RandomSource random = level.random;
            for (int i = 0; i < 6; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5) * 10.0;
                double y = player.getY() + random.nextDouble() * 3.0 + 0.2;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 10.0;
                level.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        x,
                        y,
                        z,
                        1,
                        0.05,
                        0.08,
                        0.05,
                        0.01);
            }
        }

        // Rare Φ-lightning
        if (player.tickCount % 40 == 0
                && level.random.nextFloat() < BalanceConfig.GLASS_PLAIN_STORM_LIGHTNING_CHANCE.get()) {
            strikeNear(level, player.position(), level.random);
        }
    }

    private static void strikeNear(ServerLevel level, Vec3 around, RandomSource random) {
        double ox = (random.nextDouble() - 0.5) * 24.0;
        double oz = (random.nextDouble() - 0.5) * 24.0;
        BlockPos strike = BlockPos.containing(around.x + ox, around.y, around.z + oz);
        BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, strike);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(Vec3.atBottomCenterOf(surface));
        bolt.setVisualOnly(false);
        level.addFreshEntity(bolt);
        // Φ-burn splash
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5) < 36.0) {
                p.hurt(level.damageSources().magic(), 2.0f);
                p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
            }
        }
    }

    /** 0..1 storm intensity for client fog (1 = full storm). */
    public static float clientStormIntensity(Level level, BlockPos pos) {
        return isStorming(level, pos) ? 1f : 0f;
    }
}
