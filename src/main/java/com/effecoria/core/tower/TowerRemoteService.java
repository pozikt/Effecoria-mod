package com.effecoria.core.tower;

import com.effecoria.block.PhiBeaconBlockEntity;
import com.effecoria.block.PhiTelegraphBlock;
import com.effecoria.block.PhiTurretBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.block.TowerConsoleBlockEntity;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.glue.EssenceGlueData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Remote tower commands from the local console or a Φ-telegraph linked into the tower component.
 */
public final class TowerRemoteService {
    public static final int ACTION_SCAN = 0;
    public static final int ACTION_TURRET_TOGGLE = 1;
    public static final int ACTION_BEACON_QUERY = 2;
    public static final int ACTION_OPEN_CONSOLE = 3;

    private TowerRemoteService() {}

    public static boolean authorize(ServerPlayer player, ServerLevel accessLevel, BlockPos accessPos) {
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(accessLevel, accessPos).orElse(null);
        if (computer != null && computer.bound() && player.getUUID().equals(computer.ownerUuid())) {
            return true;
        }
        // Telegraph linked into a tower the player owns.
        BlockEntity be = accessLevel.getBlockEntity(accessPos);
        if (be instanceof PhiTelegraphBlock.PhiTelegraphBlockEntity telegraph && telegraph.hasLink()) {
            ServerLevel linkedLevel = resolveLinkedLevel(player, telegraph);
            BlockPos linkedPos = telegraph.linkedPos();
            if (linkedLevel == null || linkedPos == null) {
                return false;
            }
            TowerAnchorBlockEntity remote = TowerFacility.findComputer(linkedLevel, linkedPos).orElse(null);
            return remote != null && remote.bound() && player.getUUID().equals(remote.ownerUuid());
        }
        return false;
    }

    /** Resolve the tower component access position for remote ops (local or linked). */
    @Nullable
    public static Access resolveTowerAccess(ServerPlayer player, ServerLevel accessLevel, BlockPos accessPos) {
        TowerAnchorBlockEntity local = TowerFacility.findComputer(accessLevel, accessPos).orElse(null);
        if (local != null && local.bound() && player.getUUID().equals(local.ownerUuid())) {
            return new Access(accessLevel, local.getBlockPos(), local);
        }
        BlockEntity be = accessLevel.getBlockEntity(accessPos);
        if (!(be instanceof PhiTelegraphBlock.PhiTelegraphBlockEntity telegraph) || !telegraph.hasLink()) {
            return null;
        }
        ServerLevel linkedLevel = resolveLinkedLevel(player, telegraph);
        BlockPos linkedPos = telegraph.linkedPos();
        if (linkedLevel == null || linkedPos == null) {
            return null;
        }
        TowerAnchorBlockEntity remote = TowerFacility.findComputer(linkedLevel, linkedPos).orElse(null);
        if (remote == null || !remote.bound() || !player.getUUID().equals(remote.ownerUuid())) {
            return null;
        }
        return new Access(linkedLevel, remote.getBlockPos(), remote);
    }

    public static boolean execute(
            ServerPlayer player, BlockPos accessPos, int actionId, @Nullable BlockPos targetPos, int modeId) {
        if (!(player.level() instanceof ServerLevel accessLevel)) {
            return false;
        }
        Access access = resolveTowerAccess(player, accessLevel, accessPos);
        if (access == null) {
            // Also allow when accessPos is already inside owned tower (console).
            if (!authorize(player, accessLevel, accessPos)) {
                player.displayClientMessage(Component.translatable("message.effecoria.tower.remote_denied"), true);
                return false;
            }
            TowerAnchorBlockEntity computer = TowerFacility.findComputer(accessLevel, accessPos).orElse(null);
            if (computer == null) {
                return false;
            }
            access = new Access(accessLevel, computer.getBlockPos(), computer);
        }

        return switch (actionId) {
            case ACTION_SCAN -> {
                BlockPos sonarPos = targetPos != null
                        ? targetPos
                        : TowerFacility.findInComponent(access.level(), access.computerPos(), com.effecoria.block.PhiSonarBlockEntity.class)
                                .map(BlockEntity::getBlockPos)
                                .orElse(access.computerPos());
                PhiSonarService.requestScan(player, sonarPos, modeId);
                yield true;
            }
            case ACTION_TURRET_TOGGLE -> {
                if (targetPos == null) {
                    yield false;
                }
                if (!inSameComponent(access.level(), access.computerPos(), targetPos)) {
                    player.displayClientMessage(Component.translatable("message.effecoria.tower.remote_denied"), true);
                    yield false;
                }
                BlockEntity be = access.level().getBlockEntity(targetPos);
                if (!(be instanceof PhiTurretBlockEntity turret) || !turret.formed()) {
                    player.displayClientMessage(Component.translatable("message.effecoria.tower.remote_turret_bad"), true);
                    yield false;
                }
                if (!PhiPower.hasPower(access.level(), targetPos)) {
                    player.displayClientMessage(Component.translatable("message.effecoria.tower.remote_no_power"), true);
                    yield false;
                }
                turret.toggleArmed();
                player.displayClientMessage(
                        Component.translatable(
                                turret.armed()
                                        ? "message.effecoria.tower.remote_turret_armed"
                                        : "message.effecoria.tower.remote_turret_disarmed"),
                        true);
                yield true;
            }
            case ACTION_BEACON_QUERY -> {
                if (targetPos == null) {
                    yield false;
                }
                if (!inSameComponent(access.level(), access.computerPos(), targetPos)) {
                    yield false;
                }
                BlockEntity be = access.level().getBlockEntity(targetPos);
                if (!(be instanceof PhiBeaconBlockEntity beacon)) {
                    yield false;
                }
                String name = beacon.beaconName();
                if (name == null || name.isEmpty()) {
                    name = "?";
                }
                boolean power = PhiPower.hasPower(access.level(), targetPos);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.tower.remote_beacon",
                                name,
                                targetPos.getX(),
                                targetPos.getY(),
                                targetPos.getZ(),
                                power ? "Φ" : "—"),
                        false);
                yield true;
            }
            case ACTION_OPEN_CONSOLE -> {
                TowerConsoleBlockEntity console = TowerFacility.findInComponent(
                                access.level(), access.computerPos(), TowerConsoleBlockEntity.class)
                        .orElse(null);
                if (console == null) {
                    player.displayClientMessage(Component.translatable("message.effecoria.tower.remote_no_console"), true);
                    yield false;
                }
                player.openMenu(console, buf -> buf.writeBlockPos(console.getBlockPos()));
                yield true;
            }
            default -> false;
        };
    }

    /** Empty-hand use on a linked telegraph opens the remote tower console when authorized. */
    public static boolean tryOpenRemoteFromTelegraph(
            ServerPlayer player, ServerLevel level, PhiTelegraphBlock.PhiTelegraphBlockEntity telegraph) {
        if (!telegraph.hasLink()) {
            return false;
        }
        Access access = resolveTowerAccess(player, level, telegraph.getBlockPos());
        if (access == null) {
            return false;
        }
        return execute(player, telegraph.getBlockPos(), ACTION_OPEN_CONSOLE, null, 0);
    }

    private static boolean inSameComponent(ServerLevel level, BlockPos a, BlockPos b) {
        return EssenceGlueData.get(level).component(a).contains(b);
    }

    @Nullable
    private static ServerLevel resolveLinkedLevel(
            ServerPlayer player, PhiTelegraphBlock.PhiTelegraphBlockEntity telegraph) {
        ResourceKey<Level> dim = telegraph.linkedDim();
        if (dim == null) {
            return null;
        }
        return player.server.getLevel(dim);
    }

    public record Access(ServerLevel level, BlockPos computerPos, TowerAnchorBlockEntity computer) {}
}
