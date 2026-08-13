package com.effecoria.core.tower;

import com.effecoria.block.PhiSonarBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.block.TowerConsoleBlockEntity;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Active Φ-sonar: surface heightmap + living/undead/player blips around a tower sonar emitter.
 */
public final class PhiSonarService {
    public static final int RADIUS = 64;
    public static final int STEP = 2;
    public static final int PHI_COST = 12;
    public static final int MAX_BLIPS = 64;

    public static final byte BLIP_LIVING = 0;
    public static final byte BLIP_UNDEAD = 1;
    public static final byte BLIP_PLAYER = 2;

    private PhiSonarService() {}

    public record Blip(short relX, short relZ, byte kind) {}

    public record ScanResult(
            int originX,
            int originY,
            int originZ,
            int radius,
            int step,
            int width,
            byte[] heights,
            List<Blip> blips) {}

    /**
     * Run a scan from the Φ-sonar in the console's glue component and send the map to the player.
     *
     * @return true if a map payload was sent
     */
    public static boolean requestScan(ServerPlayer player, BlockPos consolePos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!(level.getBlockEntity(consolePos) instanceof TowerConsoleBlockEntity)) {
            return false;
        }

        TowerAnchorBlockEntity computer = TowerFacility.findComputer(level, consolePos).orElse(null);
        if (computer == null || !computer.consecrated() || !computer.bound()) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.tower_offline"), true);
            return false;
        }

        PhiSonarBlockEntity sonar = TowerFacility.findInComponent(level, consolePos, PhiSonarBlockEntity.class)
                .orElse(null);
        if (sonar == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.missing"), true);
            return false;
        }
        if (!sonar.ready()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.phi_sonar.cooldown",
                            (sonar.cooldownTicks() + 19) / 20),
                    true);
            return false;
        }

        BlockPos origin = sonar.getBlockPos();
        if (!PhiPower.hasPower(level, origin) && !PhiPower.hasPower(level, computer.getBlockPos())) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.no_power"), true);
            return false;
        }
        if (!PhiPower.consumeTick(level, origin, PHI_COST)
                && !PhiPower.consumeTick(level, computer.getBlockPos(), PHI_COST)) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.no_power"), true);
            return false;
        }

        ScanResult result = scan(level, origin);
        sonar.markScanned(level.getGameTime());
        PacketDistributor.sendToPlayer(
                player,
                new ModNetworking.PhiSonarMapPayload(
                        result.originX(),
                        result.originY(),
                        result.originZ(),
                        result.radius(),
                        result.step(),
                        result.width(),
                        result.heights(),
                        result.blips()));
        player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.scanned"), true);
        return true;
    }

    public static ScanResult scan(ServerLevel level, BlockPos origin) {
        int width = (RADIUS * 2) / STEP + 1;
        byte[] heights = new byte[width * width];
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        int i = 0;
        for (int iz = -RADIUS; iz <= RADIUS; iz += STEP) {
            for (int ix = -RADIUS; ix <= RADIUS; ix += STEP) {
                int worldX = ox + ix;
                int worldZ = oz + iz;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                int rel = surfaceY - oy;
                if (rel > 127) {
                    rel = 127;
                } else if (rel < -128) {
                    rel = -128;
                }
                heights[i++] = (byte) rel;
            }
        }

        List<Blip> blips = new ArrayList<>();
        AABB box = new AABB(
                ox - RADIUS,
                level.getMinBuildHeight(),
                oz - RADIUS,
                ox + RADIUS + 1,
                level.getMaxBuildHeight(),
                oz + RADIUS + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (blips.size() >= MAX_BLIPS) {
                break;
            }
            if (!entity.isAlive()) {
                continue;
            }
            int dx = entity.blockPosition().getX() - ox;
            int dz = entity.blockPosition().getZ() - oz;
            if (Math.abs(dx) > RADIUS || Math.abs(dz) > RADIUS) {
                continue;
            }
            byte kind;
            if (entity instanceof Player) {
                kind = BLIP_PLAYER;
            } else if (entity.getType().is(EntityTypeTags.UNDEAD)) {
                kind = BLIP_UNDEAD;
            } else {
                kind = BLIP_LIVING;
            }
            blips.add(new Blip((short) dx, (short) dz, kind));
        }

        return new ScanResult(ox, oy, oz, RADIUS, STEP, width, heights, blips);
    }
}
