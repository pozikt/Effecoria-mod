package com.effecoria.core.tower;

import com.effecoria.block.PhiGeyserBlock;
import com.effecoria.block.PhiGeyserPhase;
import com.effecoria.block.PhiSonarBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.content.ModBlockTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.network.ModNetworking;
import com.effecoria.world.OmegaScarService;
import com.effecoria.world.weather.PhiWeatherService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Tower Φ-sonar: heightmap + per-cell terrain class + entity/anomaly blips. */
public final class PhiSonarService {
    public static final int MAX_BLIPS = 96;
    public static final int DEEP_PROBE = 48;

    public static final byte BLIP_LIVING = 0;
    public static final byte BLIP_UNDEAD = 1;
    public static final byte BLIP_PLAYER = 2;
    public static final byte BLIP_GEYSER = 3;
    public static final byte BLIP_OMEGA = 4;

    public static final byte TERRAIN_GROUND = 0;
    public static final byte TERRAIN_WATER = 1;
    public static final byte TERRAIN_STONE = 2;
    public static final byte TERRAIN_FOLIAGE = 3;
    public static final byte TERRAIN_SAND = 4;
    public static final byte TERRAIN_ESSONITE = 5;
    public static final byte TERRAIN_MITHRIL = 6;
    public static final byte TERRAIN_OMEGA = 7;
    public static final byte TERRAIN_GEYSER = 8;
    public static final byte TERRAIN_SHIELD = 9;
    public static final byte TERRAIN_CAVE = 10;
    public static final byte TERRAIN_METAL = 11;

    public enum Mode {
        ACTIVE(0, 64, 2, 12, 160),
        DEEP(1, 32, 1, 24, 240),
        LONG_RANGE(2, 128, 4, 36, 400);

        private final int id;
        private final int radius;
        private final int step;
        private final int phiCost;
        private final int cooldownTicks;

        Mode(int id, int radius, int step, int phiCost, int cooldownTicks) {
            this.id = id;
            this.radius = radius;
            this.step = step;
            this.phiCost = phiCost;
            this.cooldownTicks = cooldownTicks;
        }

        public int id() {
            return id;
        }

        public int radius() {
            return radius;
        }

        public int step() {
            return step;
        }

        public int phiCost() {
            return phiCost;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public static Mode fromId(int id) {
            for (Mode m : values()) {
                if (m.id == id) {
                    return m;
                }
            }
            return ACTIVE;
        }

        public Mode next() {
            Mode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    private PhiSonarService() {}

    public record Blip(short relX, short relZ, byte kind) {}

    public record ScanResult(
            int originX,
            int originY,
            int originZ,
            int radius,
            int step,
            int width,
            int modeId,
            byte[] heights,
            byte[] terrain,
            List<Blip> blips) {}

    public static boolean requestScan(ServerPlayer player, BlockPos accessPos, int modeId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        TowerAnchorBlockEntity computer = TowerFacility.findComputer(level, accessPos).orElse(null);
        if (computer == null || !computer.consecrated() || !computer.bound()) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.tower_offline"), true);
            return false;
        }

        PhiSonarBlockEntity sonar = TowerFacility.findInComponent(level, accessPos, PhiSonarBlockEntity.class)
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
        if (PhiWeatherService.isStormActive(level, origin)) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.storm"), true);
            return false;
        }

        Mode mode = Mode.fromId(modeId);
        if (!PhiPower.hasPower(level, origin) && !PhiPower.hasPower(level, computer.getBlockPos())) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.no_power"), true);
            return false;
        }
        if (!PhiPower.consumeTick(level, origin, mode.phiCost())
                && !PhiPower.consumeTick(level, computer.getBlockPos(), mode.phiCost())) {
            player.displayClientMessage(Component.translatable("message.effecoria.phi_sonar.no_power"), true);
            return false;
        }

        ScanResult result = scan(level, origin, mode);
        sonar.markScanned(level.getGameTime(), mode.cooldownTicks());
        PacketDistributor.sendToPlayer(
                player,
                new ModNetworking.PhiSonarMapPayload(
                        result.originX(),
                        result.originY(),
                        result.originZ(),
                        result.radius(),
                        result.step(),
                        result.width(),
                        result.modeId(),
                        result.heights(),
                        result.terrain(),
                        result.blips()));
        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.phi_sonar.scanned_mode",
                        Component.translatable("gui.effecoria.phi_sonar.mode." + mode.name().toLowerCase())),
                true);
        return true;
    }

    public static ScanResult scan(ServerLevel level, BlockPos origin, Mode mode) {
        int radius = mode.radius();
        int step = mode.step();
        int width = (radius * 2) / step + 1;
        byte[] heights = new byte[width * width];
        byte[] terrain = new byte[width * width];
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        int i = 0;
        for (int iz = -radius; iz <= radius; iz += step) {
            for (int ix = -radius; ix <= radius; ix += step) {
                int worldX = ox + ix;
                int worldZ = oz + iz;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
                int rel = Math.max(-128, Math.min(127, surfaceY - oy));
                heights[i] = (byte) rel;
                terrain[i] = classifyColumn(level, worldX, worldZ, surfaceY, mode);
                i++;
            }
        }

        List<Blip> blips = new ArrayList<>();
        collectEntityBlips(level, ox, oz, radius, blips);
        collectAnomalyBlips(level, ox, oy, oz, radius, mode, blips);

        return new ScanResult(ox, oy, oz, radius, step, width, mode.id(), heights, terrain, blips);
    }

    private static byte classifyColumn(ServerLevel level, int x, int z, int surfaceY, Mode mode) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, surfaceY - 1, z);
        if (surfaceY <= level.getMinBuildHeight()) {
            return TERRAIN_STONE;
        }

        BlockState fluidCheck = level.getBlockState(cursor.set(x, surfaceY, z));
        if (!fluidCheck.getFluidState().isEmpty()
                || fluidCheck.is(Blocks.WATER)
                || fluidCheck.is(Blocks.LAVA)
                || fluidCheck.is(ModBlocks.PHI_WATER.get())
                || fluidCheck.is(ModBlocks.PURIFIED_PHI_WATER.get())) {
            return TERRAIN_WATER;
        }

        BlockState top = level.getBlockState(cursor.set(x, Math.max(level.getMinBuildHeight(), surfaceY - 1), z));
        if (isShield(top)) {
            return TERRAIN_SHIELD;
        }
        if (top.is(ModBlocks.PHI_GEYSER.get())) {
            return TERRAIN_GEYSER;
        }
        if (OmegaScarService.isBiome(level, cursor) || isOmegaBlock(top)) {
            return TERRAIN_OMEGA;
        }
        if (top.is(ModBlockTags.ESSONITE_ORES)
                || top.is(ModBlocks.ESSONITE_BLOCK.get())
                || top.is(ModBlocks.STAR_ESSONITE_BLOCK.get())) {
            return TERRAIN_ESSONITE;
        }
        if (top.is(ModBlockTags.MITHRIL_ORES) || top.is(ModBlocks.MITHRIL_BLOCK.get())) {
            return TERRAIN_MITHRIL;
        }
        if (top.is(ModBlockTags.PHI_CONDUCTORS) || top.is(Blocks.IRON_BLOCK) || top.is(Blocks.COPPER_BLOCK)) {
            return TERRAIN_METAL;
        }
        if (top.is(BlockTags.LEAVES) || top.is(BlockTags.LOGS) || top.is(BlockTags.FLOWERS)) {
            return TERRAIN_FOLIAGE;
        }
        if (top.is(BlockTags.SAND) || top.is(Blocks.GRAVEL) || top.is(Blocks.RED_SAND)) {
            return TERRAIN_SAND;
        }
        if (top.is(BlockTags.BASE_STONE_OVERWORLD) || top.is(BlockTags.BASE_STONE_NETHER)) {
            return TERRAIN_STONE;
        }

        if (mode == Mode.DEEP) {
            byte deep = probeDeep(level, x, z, surfaceY);
            if (deep != TERRAIN_GROUND) {
                return deep;
            }
        }

        if (mode == Mode.LONG_RANGE) {
            for (int dy = 1; dy <= 8; dy++) {
                BlockState below = level.getBlockState(cursor.set(x, surfaceY - dy, z));
                if (below.is(ModBlockTags.ESSONITE_ORES)) {
                    return TERRAIN_ESSONITE;
                }
                if (below.is(ModBlockTags.MITHRIL_ORES)) {
                    return TERRAIN_MITHRIL;
                }
                if (isOmegaBlock(below)) {
                    return TERRAIN_OMEGA;
                }
            }
        }

        MapColor color = top.getMapColor(level, cursor);
        if (color == MapColor.WATER || color == MapColor.ICE) {
            return TERRAIN_WATER;
        }
        if (color == MapColor.STONE || color == MapColor.DEEPSLATE || color == MapColor.TERRACOTTA_GRAY) {
            return TERRAIN_STONE;
        }
        return TERRAIN_GROUND;
    }

    private static byte probeDeep(ServerLevel level, int x, int z, int surfaceY) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinBuildHeight() + 1, surfaceY - DEEP_PROBE);
        boolean sawCave = false;
        for (int y = surfaceY - 2; y >= minY; y--) {
            BlockState state = level.getBlockState(cursor.set(x, y, z));
            if (state.isAir()) {
                sawCave = true;
                continue;
            }
            if (state.is(ModBlockTags.ESSONITE_ORES)) {
                return TERRAIN_ESSONITE;
            }
            if (state.is(ModBlockTags.MITHRIL_ORES)) {
                return TERRAIN_MITHRIL;
            }
            if (isOmegaBlock(state)) {
                return TERRAIN_OMEGA;
            }
            if (isShield(state)) {
                return TERRAIN_SHIELD;
            }
            if (!state.getFluidState().isEmpty()) {
                return TERRAIN_WATER;
            }
        }
        return sawCave ? TERRAIN_CAVE : TERRAIN_GROUND;
    }

    private static boolean isShield(BlockState state) {
        return state.is(Blocks.GOLD_BLOCK)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.RAW_GOLD_BLOCK)
                || state.is(ModBlocks.LEAD_BLOCK.get())
                || state.is(ModBlocks.LEAD_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_LEAD_ORE.get())
                || state.is(ModBlockTags.ZERO_FLUX);
    }

    private static boolean isOmegaBlock(BlockState state) {
        return state.is(ModBlocks.VOID_OBSIDIAN.get())
                || state.is(ModBlocks.OMEGA_TAINTED_OBSIDIAN.get())
                || state.is(ModBlocks.OMEGA_CRYSTAL.get())
                || state.is(ModBlocks.ASH_SOIL.get())
                || state.is(ModBlocks.OMEGA_BLADES.get());
    }

    private static void collectEntityBlips(
            ServerLevel level, int ox, int oz, int radius, List<Blip> blips) {
        AABB box = new AABB(
                ox - radius,
                level.getMinBuildHeight(),
                oz - radius,
                ox + radius + 1,
                level.getMaxBuildHeight(),
                oz + radius + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (blips.size() >= MAX_BLIPS) {
                return;
            }
            if (!entity.isAlive()) {
                continue;
            }
            int dx = entity.blockPosition().getX() - ox;
            int dz = entity.blockPosition().getZ() - oz;
            if (Math.abs(dx) > radius || Math.abs(dz) > radius) {
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
    }

    private static void collectAnomalyBlips(
            ServerLevel level, int ox, int oy, int oz, int radius, Mode mode, List<Blip> blips) {
        int sample = mode == Mode.LONG_RANGE ? 8 : 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dz = -radius; dz <= radius && blips.size() < MAX_BLIPS; dz += sample) {
            for (int dx = -radius; dx <= radius && blips.size() < MAX_BLIPS; dx += sample) {
                cursor.set(ox + dx, oy, oz + dz);
                if (OmegaScarService.isBiome(level, cursor)) {
                    blips.add(new Blip((short) dx, (short) dz, BLIP_OMEGA));
                    continue;
                }
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox + dx, oz + dz);
                BlockState at = level.getBlockState(
                        cursor.set(ox + dx, Math.max(level.getMinBuildHeight(), surfaceY - 1), oz + dz));
                if (at.is(ModBlocks.PHI_GEYSER.get())) {
                    PhiGeyserPhase phase = at.getValue(PhiGeyserBlock.PHASE);
                    if (phase == PhiGeyserPhase.ERUPTING
                            || phase == PhiGeyserPhase.PRECURSOR
                            || mode == Mode.LONG_RANGE) {
                        blips.add(new Blip((short) dx, (short) dz, BLIP_GEYSER));
                    }
                }
            }
        }
    }
}
