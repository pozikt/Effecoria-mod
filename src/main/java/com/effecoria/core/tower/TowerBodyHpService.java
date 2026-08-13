package com.effecoria.core.tower;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.glue.EssenceGlueStructure;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Structural tower HP = present glued cells / glued cell count. */
public final class TowerBodyHpService {
    private TowerBodyHpService() {}

    public static void syncFromAnchor(ServerPlayer player, ServerLevel level, TowerAnchorBlockEntity anchor) {
        EssenceGlueStructure.Report report = EssenceGlueStructure.inspect(level, anchor.getBlockPos());
        PlayerPsiData data = PsiHelper.get(player);
        data.setTowerHp(report.presentBlocks(), report.gluedCells());
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
    }

    public static void syncOwnerOfComponent(ServerLevel level, BlockPos anyPosInComponent) {
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(level, anyPosInComponent).orElse(null);
        if (computer == null || !computer.bound() || computer.ownerUuid() == null) {
            return;
        }
        computer.refreshIntegrity(level);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(computer.ownerUuid());
        if (owner == null) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(owner);
        if (!data.towerBound() || !owner.getUUID().equals(computer.ownerUuid())) {
            return;
        }
        syncFromAnchor(owner, level, computer);
    }

    public static void tickSync(ServerPlayer player) {
        if (player.tickCount % 40 != 0) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerBound() || data.towerDim() == null || data.towerPos() == null) {
            if (data.towerMaxHp() != 0 || data.towerHp() != 0) {
                data.setTowerHp(0, 0);
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
            }
            return;
        }
        ServerLevel towerLevel = player.server.getLevel(data.towerDim());
        if (towerLevel == null) {
            return;
        }
        BlockEntity be = towerLevel.getBlockEntity(data.towerPos());
        if (be instanceof TowerAnchorBlockEntity anchor && anchor.bound()) {
            syncFromAnchor(player, towerLevel, anchor);
        }
    }
}
