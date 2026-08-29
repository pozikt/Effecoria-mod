package com.effecoria.core.tower;

import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.block.FoundationAmuletBlockEntity;
import com.effecoria.block.HeartReactorBlockEntity;
import com.effecoria.block.PhiTurretBlockEntity;
import com.effecoria.block.SparkReactorBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.glue.EssenceGlueData;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Owner feels the soulbound tower as an extension of self (integrity / pain / home bonuses). */
public final class TowerGeniusLociService {
    private static final float INTEGRITY_WARN_DELTA = 0.02f;
    private static final float PSI_REGEN_INSIDE = 0.35f;

    private TowerGeniusLociService() {}

    /** Call every 20 player ticks. */
    public static void tickPlayer(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerBound() || data.towerDim() == null || data.towerPos() == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }

        ServerLevel towerLevel = player.server.getLevel(data.towerDim());
        if (towerLevel == null) {
            return;
        }

        BlockEntity be = towerLevel.getBlockEntity(data.towerPos());
        if (!(be instanceof TowerAnchorBlockEntity anchor) || !anchor.bound()) {
            return;
        }
        if (!player.getUUID().equals(anchor.ownerUuid())) {
            return;
        }

        double integrity = anchor.integrity();
        float prev = data.lastTowerIntegrity();
        if (prev > 0f && integrity + INTEGRITY_WARN_DELTA < prev && data.lociWarnCooldown() <= 0) {
            int pct = (int) Math.round(integrity * 100.0);
            player.displayClientMessage(
                    Component.translatable("message.effecoria.tower.loci_damaged", pct), true);
            data.setLociWarnCooldown(100);
        }
        data.setLastTowerIntegrity((float) integrity);
        if (data.lociWarnCooldown() > 0) {
            data.setLociWarnCooldown(Math.max(0, data.lociWarnCooldown() - 20));
        }

        if (player.level().dimension().equals(data.towerDim())) {
            AABB hull = anchor.structureBounds();
            boolean inside = hull.contains(player.position());
            if (inside && TowerDomeService.isOnline(towerLevel, anchor)) {
                data.setCurrentPsi(data.currentPsi() + PSI_REGEN_INSIDE);
            }
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        TowerBodyHpService.syncFromAnchor(player, towerLevel, anchor);
    }

    /** Notify online owner when a glued cell of their tower component is broken. */
    public static void onGluedCellBroken(ServerLevel level, BlockPos broken) {
        EssenceGlueData glue = EssenceGlueData.get(level);
        if (!glue.isGlued(broken)) {
            return;
        }

        BlockState state = level.getBlockState(broken);
        BlockEntity brokenBe = level.getBlockEntity(broken);

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PlayerPsiData data = PsiHelper.get(player);
            if (!data.towerBound() || data.towerPos() == null || data.towerDim() == null) {
                continue;
            }
            if (!level.dimension().equals(data.towerDim())) {
                continue;
            }
            BlockPos towerPos = data.towerPos();
            BlockEntity be = level.getBlockEntity(towerPos);
            if (!(be instanceof TowerAnchorBlockEntity anchor)
                    || !player.getUUID().equals(anchor.ownerUuid())) {
                continue;
            }
            if (!glue.isGlued(towerPos) || !glue.component(towerPos).contains(broken)) {
                continue;
            }

            applyBodyPain(player, brokenBe, state);
            TowerBodyHpService.syncOwnerOfComponent(level, towerPos);
            return;
        }
    }

    private static void applyBodyPain(ServerPlayer player, BlockEntity brokenBe, BlockState state) {
        if (TowerGhostService.isGhost(player)) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_pain_ghost"), true);
            return;
        }

        if (brokenBe instanceof FoundationAmuletBlockEntity) {
            // Soul death handled by FoundationAmuletBlock.onRemove → onAmuletBroken
            return;
        }

        if (brokenBe instanceof PhiTurretBlockEntity
                || state.is(ModBlocks.TURRET_MOUNT.get())
                || state.is(ModBlocks.PLASMA_TURRET.get())
                || state.is(ModBlocks.KINETIC_TURRET.get())
                || state.is(ModBlocks.MENTAL_TURRET.get())
                || state.is(ModBlocks.SPATIAL_TURRET.get())
                || state.is(ModBlocks.OMEGA_TURRET.get())) {
            player.hurt(player.damageSources().magic(), 4f);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_pain_turret"), true);
            return;
        }

        if (state.is(ModBlocks.PHI_BUS.get())) {
            player.hurt(player.damageSources().magic(), 2f);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false));
            player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_pain_bus"), true);
            return;
        }

        if (brokenBe instanceof TowerAnchorBlockEntity) {
            player.hurt(player.damageSources().magic(), 8f);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
            PlayerPsiData psi = PsiHelper.get(player);
            psi.setCurrentPsi(Math.max(0f, psi.currentPsi() * 0.5f));
            PsiHelper.set(player, psi);
            player.syncData(ModAttachments.PSI.get());
            player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_pain_computer"), true);
            return;
        }

        if (brokenBe instanceof SparkReactorBlockEntity
                || brokenBe instanceof HeartReactorBlockEntity
                || brokenBe instanceof ForgeReactorBlockEntity
                || state.is(ModBlocks.SPARK_REACTOR.get())
                || state.is(ModBlocks.HEART_REACTOR_CORE.get())
                || state.is(ModBlocks.FORGE_REACTOR_CORE.get())) {
            player.hurt(player.damageSources().magic(), 14f);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3, false, false));
            player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_pain_reactor"), true);
            if (player.getHealth() <= 0f || !player.isAlive()) {
                return;
            }
            if (player.getHealth() < 4f) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 30, 0, false, false));
                player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_coma"), true);
            }
            return;
        }

        player.hurt(player.damageSources().magic(), 1.5f);
        player.displayClientMessage(Component.translatable("message.effecoria.tower.loci_pain_wall"), true);
    }
}
