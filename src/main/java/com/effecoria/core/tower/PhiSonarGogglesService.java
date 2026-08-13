package com.effecoria.core.tower;

import com.effecoria.block.PhiSonarBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.content.ModItems;
import com.effecoria.core.artifact.CuriosAccess;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Pushes tower Φ-sonar maps to players wearing Φ-sonar goggles. */
public final class PhiSonarGogglesService {
    /** Max distance (blocks) from sonar for non-owner goggle feed. */
    public static final double FEED_RANGE = 192.0;
    private static final int EQUIP_CHUNK_RADIUS = 12;

    private PhiSonarGogglesService() {}

    public static boolean wearing(ServerPlayer player) {
        return CuriosAccess.hasEquipped(player, ModItems.PHI_SONAR_GOGGLES.get());
    }

    public static void distributeScan(
            ServerLevel level,
            TowerAnchorBlockEntity computer,
            PhiSonarService.ScanResult result,
            ServerPlayer primary) {
        ModNetworking.PhiSonarMapPayload payload = toPayload(result);
        Set<UUID> sent = new HashSet<>();
        send(primary, payload, sent);

        UUID owner = computer.ownerUuid();
        if (owner != null) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
            if (ownerPlayer != null && wearing(ownerPlayer)) {
                send(ownerPlayer, payload, sent);
            }
        }

        double rangeSq = FEED_RANGE * FEED_RANGE;
        for (ServerPlayer player : level.players()) {
            if (sent.contains(player.getUUID()) || !wearing(player)) {
                continue;
            }
            if (player.distanceToSqr(result.originX() + 0.5, result.originY() + 0.5, result.originZ() + 0.5)
                    <= rangeSq) {
                send(player, payload, sent);
            }
        }
    }

    /** On goggles equip: pull nearest stored tower scan in loaded chunks. */
    public static void trySyncOnEquip(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !wearing(player)) {
            return;
        }
        PhiSonarBlockEntity sonar = findNearbyStoredScan(level, player.blockPosition()).orElse(null);
        if (sonar == null) {
            return;
        }
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(level, sonar.getBlockPos()).orElse(null);
        if (computer == null || !computer.consecrated() || !computer.bound()) {
            return;
        }
        UUID owner = computer.ownerUuid();
        boolean ownerMatch = owner != null && owner.equals(player.getUUID());
        boolean inRange = player.distanceToSqr(sonar.getBlockPos().getCenter()) <= FEED_RANGE * FEED_RANGE;
        if (!ownerMatch && !inRange) {
            return;
        }
        ModNetworking.PhiSonarMapPayload payload = sonar.toMapPayload();
        if (payload != null) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static java.util.Optional<PhiSonarBlockEntity> findNearbyStoredScan(ServerLevel level, BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        PhiSonarBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -EQUIP_CHUNK_RADIUS; dx <= EQUIP_CHUNK_RADIUS; dx++) {
            for (int dz = -EQUIP_CHUNK_RADIUS; dz <= EQUIP_CHUNK_RADIUS; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof PhiSonarBlockEntity sonar) || !sonar.hasStoredScan()) {
                        continue;
                    }
                    double d = be.getBlockPos().distSqr(center);
                    if (d < bestDist) {
                        bestDist = d;
                        best = sonar;
                    }
                }
            }
        }
        return java.util.Optional.ofNullable(best);
    }

    private static ModNetworking.PhiSonarMapPayload toPayload(PhiSonarService.ScanResult result) {
        return new ModNetworking.PhiSonarMapPayload(
                result.originX(),
                result.originY(),
                result.originZ(),
                result.radius(),
                result.step(),
                result.width(),
                result.modeId(),
                result.heights(),
                result.terrain(),
                result.blips());
    }

    private static void send(ServerPlayer player, ModNetworking.PhiSonarMapPayload payload, Set<UUID> sent) {
        if (!sent.add(player.getUUID())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payload);
    }
}
