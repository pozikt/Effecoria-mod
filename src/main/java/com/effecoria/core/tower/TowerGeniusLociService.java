package com.effecoria.core.tower;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.glue.EssenceGlueData;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** Owner feels the soulbound tower as an extension of self (integrity / home bonuses). */
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
        if (!player.level().dimension().equals(data.towerDim())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockEntity be = level.getBlockEntity(data.towerPos());
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

        AABB hull = anchor.structureBounds();
        boolean inside = hull.contains(player.position());
        if (inside && TowerDomeService.isOnline(level, anchor)) {
            data.setCurrentPsi(data.currentPsi() + PSI_REGEN_INSIDE);
            if (player.tickCount % 100 == 0) {
                int pct = (int) Math.round(integrity * 100.0);
                player.displayClientMessage(
                        Component.translatable("message.effecoria.tower.loci_home", pct), true);
            }
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
    }

    /** Notify online owner when a glued cell of their tower component is broken. */
    public static void onGluedCellBroken(ServerLevel level, BlockPos broken) {
        EssenceGlueData glue = EssenceGlueData.get(level);
        if (!glue.isGlued(broken)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
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
            if (!glue.isGlued(towerPos)) {
                continue;
            }
            if (!glue.component(towerPos).contains(broken)) {
                continue;
            }
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.tower.loci_cell_broken",
                            broken.getX(),
                            broken.getY(),
                            broken.getZ()),
                    true);
            return;
        }
    }
}
