package com.effecoria.core.tower;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.effecoria.world.weather.PhiWeatherService;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Return Protocol — personal amulet recall to the bound Mage Tower.
 * Spends player Ψ and tower Φ proportional to travel distance.
 */
public final class TowerReturnProtocol {
    /** Base Ψ before distance scaling. */
    public static final float BASE_PSI = 18f;
    /** Extra Ψ per block of travel (horizontal in-world, or escalated cross-dim). */
    public static final float PSI_PER_BLOCK = 0.08f;
    public static final float MAX_PSI_COST = 220f;

    /** Base Φ-load ticks on the tower reactor. */
    public static final int BASE_PHI = 10;
    /** Extra Φ-load per 16 blocks. */
    public static final int PHI_PER_CHUNK = 1;

    public static final int COOLDOWN_TICKS = 20 * 25;
    public static final int CHANNEL_TICKS = 36; // 1.8s

    /** Flat distance penalty when recalling across dimensions. */
    private static final double CROSS_DIM_PENALTY = 640.0;

    private TowerReturnProtocol() {}

    public record Quote(double distance, float psiCost, int phiCost, boolean crossDim) {}

    public static Quote quote(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerBound() || data.towerDim() == null || data.towerPos() == null) {
            return new Quote(0, 0, 0, false);
        }
        boolean cross = !player.level().dimension().equals(data.towerDim());
        double dist;
        if (cross) {
            BlockPos here = player.blockPosition();
            BlockPos there = data.towerPos();
            double planar = Math.sqrt(here.distToCenterSqr(there.getX() + 0.5, here.getY(), there.getZ() + 0.5));
            dist = planar + CROSS_DIM_PENALTY;
        } else {
            BlockPos here = player.blockPosition();
            BlockPos there = data.towerPos();
            dist = Math.sqrt(here.distToCenterSqr(there.getX() + 0.5, here.getY(), there.getZ() + 0.5));
        }
        float psi = Math.min(MAX_PSI_COST, BASE_PSI + (float) (dist * PSI_PER_BLOCK));
        int phi = BASE_PHI + (int) Math.ceil(dist / 16.0) * PHI_PER_CHUNK;
        return new Quote(dist, psi, phi, cross);
    }

    /**
     * Attempts the Return Protocol. Returns {@code true} if the mage arrives at the tower.
     */
    public static boolean tryReturn(ServerPlayer player) {
        if (!TechnomagicGates.checkOperate(player, TechnomagicEra.VI)) {
            return false;
        }
        if (!TowerSoulbindService.towerAliveFor(player)) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.return_offline"), true);
            return false;
        }

        PlayerPsiData data = PsiHelper.get(player);
        ResourceKey<Level> dim = data.towerDim();
        BlockPos towerPos = data.towerPos();
        if (dim == null || towerPos == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.unbound"), true);
            return false;
        }

        ServerLevel towerLevel = player.server.getLevel(dim);
        if (towerLevel == null) {
            return false;
        }
        BlockEntity be = towerLevel.getBlockEntity(towerPos);
        if (!(be instanceof TowerAnchorBlockEntity anchor) || !anchor.bound()) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.return_offline"), true);
            return false;
        }

        Quote q = quote(player);
        if (data.currentPsi() < q.psiCost() && !player.getAbilities().instabuild) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.tower.return_no_psi",
                            String.format("%.0f", q.psiCost()),
                            String.format("%.0f", data.currentPsi())),
                    true);
            return false;
        }

        if (PhiWeatherService.isStormActive(player.level(), player.blockPosition())
                || PhiWeatherService.isStormActive(towerLevel, towerPos)) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.return_storm"), true);
            return false;
        }

        if (!player.getAbilities().instabuild) {
            if (!PhiPower.consumeTick(towerLevel, towerPos, q.phiCost())) {
                player.displayClientMessage(Component.translatable("message.effecoria.tower.return_no_phi"), true);
                return false;
            }
            data.setCurrentPsi(data.currentPsi() - q.psiCost());
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
        }

        BlockPos dest = anchor.revivePos();
        double x = dest.getX() + 0.5;
        double y = dest.getY() + 0.05;
        double z = dest.getZ() + 0.5;

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.85f,
                0.9f);

        if (player.level() == towerLevel) {
            player.teleportTo(x, y, z);
        } else {
            player.changeDimension(new DimensionTransition(
                    towerLevel,
                    new Vec3(x, y, z),
                    Vec3.ZERO,
                    player.getYRot(),
                    player.getXRot(),
                    DimensionTransition.DO_NOTHING));
        }

        towerLevel.playSound(null, dest, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.35f);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0, false, false));
        player.getCooldowns().addCooldown(
                com.effecoria.content.ModItems.PSI_FOCUS.get(), COOLDOWN_TICKS);

        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.tower.returned",
                        String.format("%.0f", q.distance()),
                        String.format("%.0f", q.psiCost()),
                        q.phiCost()),
                true);
        return true;
    }
}
