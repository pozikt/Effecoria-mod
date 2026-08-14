package com.effecoria.core.tower;

import com.effecoria.block.RegenChamberBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.loci.LexLociCompiler;
import com.effecoria.core.loci.LociActuator;
import com.effecoria.core.loci.LociEvent;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

/** Playable Φ-ghost while the soulbound tower reactor is offline. */
public final class TowerGhostService {
    private TowerGhostService() {}

    public static void enterGhost(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        data.setTowerGhost(true);
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        applyGhostEffects(player);
        enableGhostFlight(player);
        player.displayClientMessage(Component.translatable("message.effecoria.tower.ghost_enter"), false);
        player.displayClientMessage(Component.translatable("message.effecoria.tower.ghost_wait"), true);
        pulseGhostBeacon(player);
    }

    public static void tickPlayer(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerGhost()) {
            return;
        }
        if (!data.towerBound() || data.towerDim() == null || data.towerPos() == null) {
            exitGhostAbilities(player);
            data.setTowerGhost(false);
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
            return;
        }

        if (player.tickCount % 40 == 0) {
            applyGhostEffects(player);
            enableGhostFlight(player);
            pulseGhostBeacon(player);
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        ResourceKey<Level> dim = data.towerDim();
        BlockPos pos = data.towerPos();
        ServerLevel towerLevel = player.server.getLevel(dim);
        if (towerLevel == null) {
            return;
        }
        if (!TowerSoulbindService.towerAliveFor(player)) {
            return;
        }
        if (!PhoenixInrushService.canSupportRevive(towerLevel, pos)) {
            return;
        }
        BlockEntity be = towerLevel.getBlockEntity(pos);
        if (!(be instanceof TowerAnchorBlockEntity anchor) || !anchor.bound()) {
            return;
        }
        materialize(player, anchor);
    }

    public static void materialize(ServerPlayer player, TowerAnchorBlockEntity anchor) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerGhost()) {
            return;
        }
        int savedXp = data.savedTowerXpTotal();
        data.setTowerGhost(false);
        data.clearPendingTowerRevive();
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        exitGhostAbilities(player);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        TowerBodyType body = anchor.bodyType();
        if (!(anchor.getLevel() instanceof ServerLevel server)
                || !TowerFacility.payBodyCosts(server, anchor.getBlockPos(), anchor, body)) {
            body = TowerBodyType.BASIC;
            player.displayClientMessage(Component.translatable("message.effecoria.tower.body_fallback"), true);
        }
        TowerReviveService.finishRevive(player, anchor, body, savedXp);
        player.displayClientMessage(Component.translatable("message.effecoria.tower.ghost_materialize"), true);
    }

    public static boolean isGhost(ServerPlayer player) {
        return PsiHelper.get(player).towerGhost();
    }

    /** Lex Loci {@code soul_ghost} + {@code beacon}: point the ghost at the regen chamber. */
    private static void pulseGhostBeacon(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerBound() || data.towerDim() == null || data.towerPos() == null) {
            return;
        }
        ServerLevel towerLevel = player.server.getLevel(data.towerDim());
        BlockPos anchorPos = data.towerPos();
        if (towerLevel == null) {
            return;
        }
        BlockEntity be = towerLevel.getBlockEntity(anchorPos);
        if (!(be instanceof TowerAnchorBlockEntity anchor)
                || !anchor.bound()
                || !anchor.phoenixEdictEnabled()) {
            return;
        }
        var program = LexLociCompiler.compile(anchor.lociTokens());
        if (!program.ok() || !program.actuatorsFor(LociEvent.SOUL_GHOST).contains(LociActuator.BEACON)) {
            return;
        }
        BlockPos at = TowerFacility.findInComponent(towerLevel, anchorPos, RegenChamberBlockEntity.class)
                .map(BlockEntity::getBlockPos)
                .orElse(anchorPos);
        player.displayClientMessage(
                Component.translatable("message.effecoria.tower.ghost_beacon", at.getX(), at.getY(), at.getZ()),
                true);
        towerLevel.sendParticles(
                ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 10, 0.45, 0.55, 0.45, 0.02);
        towerLevel.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.45f, 1.35f);
    }

    private static void applyGhostEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 4, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false));
    }

    private static void enableGhostFlight(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    public static void exitGhostAbilities(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
    }
}
