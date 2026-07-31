package com.effecoria.effect.spatial;

import com.effecoria.block.SubspacePortalBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.world.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Subspace voyage — walk a foggy flat world where 1 block ≈ 100 overworld blocks.
 */
public final class SubspaceVoyageService {
    public static final int SCALE = 100;
    private static final int FLOOR_Y = 1;

    private SubspaceVoyageService() {}

    public static SubspaceVoyageData get(ServerPlayer player) {
        return player.getData(ModAttachments.SUBSPACE_VOYAGE.get());
    }

    public static void set(ServerPlayer player, SubspaceVoyageData data) {
        player.setData(ModAttachments.SUBSPACE_VOYAGE.get(), data);
    }

    /** Cast handler for {@code subspace_voyage}. */
    public static void cast(ServerPlayer caster) {
        if (ModDimensions.isSubspace(caster.level())) {
            openOrMoveExit(caster);
        } else {
            openEntry(caster);
        }
    }

    private static void openEntry(ServerPlayer caster) {
        SubspaceVoyageData data = get(caster);
        if (data.active() || data.pendingEntry()) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.already_active"), true);
            return;
        }
        if (ModDimensions.subspace(caster.server) == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.unavailable"), true);
            return;
        }

        BlockPos portalPos = placePortalNear(caster);
        if (portalPos == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.no_space"), true);
            return;
        }

        UUID session = UUID.randomUUID();
        configurePortal(caster.serverLevel(), portalPos, caster.getUUID(), SubspacePortalBlockEntity.Role.ENTRY, session);

        data.beginPending(session, caster.level().dimension(), caster.blockPosition(), portalPos);
        set(caster, data);

        fx(caster.serverLevel(), Vec3.atCenterOf(portalPos));
        caster.displayClientMessage(Component.translatable("message.effecoria.subspace.entry_opened"), true);
    }

    private static void openOrMoveExit(ServerPlayer caster) {
        SubspaceVoyageData data = get(caster);
        if (!data.active() || data.sessionId() == null || data.originPos() == null || data.entrySubspacePos() == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.not_voyaging"), true);
            return;
        }

        ServerLevel originLevel = resolveOrigin(caster.server, data);
        if (originLevel == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.unavailable"), true);
            return;
        }

        if (data.exitPortalSubspacePos() != null) {
            removePortal(caster.serverLevel(), data.exitPortalSubspacePos());
        }
        if (data.exitPortalOverworldPos() != null) {
            removePortal(originLevel, data.exitPortalOverworldPos());
        }

        BlockPos subspaceExit = placePortalAt(caster.serverLevel(), caster.blockPosition());
        if (subspaceExit == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.no_space"), true);
            return;
        }

        BlockPos mapped = mapToOrigin(data, caster.blockPosition());
        BlockPos overworldExit = findSafeLanding(originLevel, mapped);
        placePlatformIfNeeded(originLevel, overworldExit);
        // Clear air at landing then place exit portal block there.
        originLevel.setBlock(overworldExit, ModBlocks.SUBSPACE_PORTAL.get().defaultBlockState(), 3);

        configurePortal(
                caster.serverLevel(),
                subspaceExit,
                caster.getUUID(),
                SubspacePortalBlockEntity.Role.EXIT,
                data.sessionId());
        configurePortal(
                originLevel,
                overworldExit,
                caster.getUUID(),
                SubspacePortalBlockEntity.Role.EXIT,
                data.sessionId());

        data.setExitPortals(subspaceExit, overworldExit);
        set(caster, data);

        fx(caster.serverLevel(), Vec3.atCenterOf(subspaceExit));
        fx(originLevel, Vec3.atCenterOf(overworldExit));
        int dist = horizontalDistance(data.originPos(), overworldExit);
        caster.displayClientMessage(Component.translatable("message.effecoria.subspace.exit_opened", dist), true);
    }

    public static void onPortalTouch(ServerPlayer player, SubspacePortalBlockEntity portal) {
        SubspaceVoyageData data = get(player);
        if (data.sessionId() == null || !data.sessionId().equals(portal.sessionId())) {
            return;
        }

        if (portal.role() == SubspacePortalBlockEntity.Role.ENTRY && !ModDimensions.isSubspace(player.level())) {
            enterSubspace(player, data);
            return;
        }

        if (portal.role() == SubspacePortalBlockEntity.Role.EXIT && ModDimensions.isSubspace(player.level())) {
            exitSubspace(player, data);
        }
    }

    private static void enterSubspace(ServerPlayer player, SubspaceVoyageData data) {
        ServerLevel subspace = ModDimensions.subspace(player.server);
        if (subspace == null || data.sessionId() == null) {
            return;
        }

        BlockPos spawn = subspaceSpawn(player.getUUID());
        ensureFloor(subspace, spawn);
        data.markEntered(spawn);
        set(player, data);

        teleport(player, subspace, spawn);
        fx(subspace, Vec3.atCenterOf(spawn));
        player.displayClientMessage(Component.translatable("message.effecoria.subspace.entered"), true);
    }

    private static void exitSubspace(ServerPlayer player, SubspaceVoyageData data) {
        if (data.exitPortalOverworldPos() == null || data.originDim() == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.subspace.no_exit"), true);
            return;
        }
        ServerLevel originLevel = resolveOrigin(player.server, data);
        if (originLevel == null) {
            return;
        }

        BlockPos landing = data.exitPortalOverworldPos().immutable();
        collapsePortals(player.server, data);
        data.clear();
        set(player, data);

        // Landing was a portal block — restore air and stand there.
        if (originLevel.getBlockState(landing).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            originLevel.setBlock(landing, Blocks.AIR.defaultBlockState(), 3);
        }
        placePlatformIfNeeded(originLevel, landing);
        teleport(player, originLevel, landing);
        fx(originLevel, Vec3.atCenterOf(landing));
        player.displayClientMessage(Component.translatable("message.effecoria.subspace.exited"), true);
    }

    /** Collapse gates and remember origin for post-death return. */
    public static void handleSubspaceDeath(ServerPlayer player) {
        if (!ModDimensions.isSubspace(player.level())) {
            return;
        }
        SubspaceVoyageData data = get(player);
        if (data.originDim() == null || data.originPos() == null) {
            return;
        }
        var originDim = data.originDim();
        BlockPos originPos = data.originPos().immutable();
        collapsePortals(player.server, data);
        SubspaceVoyageData next = SubspaceVoyageData.createDefault();
        next.prepareRespawnAtOrigin(originDim, originPos);
        set(player, next);
    }

    public static void onRespawn(ServerPlayer player) {
        SubspaceVoyageData data = get(player);
        if (!data.returnOriginOnRespawn() || data.originDim() == null || data.originPos() == null) {
            return;
        }
        ServerLevel origin = player.server.getLevel(data.originDim());
        BlockPos originPos = data.originPos().immutable();
        data.clear();
        set(player, data);
        if (origin != null) {
            teleport(player, origin, originPos);
        }
    }

    public static void collapsePortals(MinecraftServer server, SubspaceVoyageData data) {
        if (data.originDim() != null) {
            ServerLevel origin = server.getLevel(data.originDim());
            if (origin != null) {
                if (data.entryPortalPos() != null) {
                    removePortal(origin, data.entryPortalPos());
                }
                if (data.exitPortalOverworldPos() != null) {
                    removePortal(origin, data.exitPortalOverworldPos());
                }
            }
        }
        ServerLevel subspace = ModDimensions.subspace(server);
        if (subspace != null && data.exitPortalSubspacePos() != null) {
            removePortal(subspace, data.exitPortalSubspacePos());
        }
    }

    public static BlockPos mapToOrigin(SubspaceVoyageData data, BlockPos subspacePos) {
        BlockPos origin = data.originPos();
        BlockPos entry = data.entrySubspacePos();
        if (origin == null || entry == null) {
            return BlockPos.ZERO;
        }
        int dx = subspacePos.getX() - entry.getX();
        int dz = subspacePos.getZ() - entry.getZ();
        return new BlockPos(origin.getX() + dx * SCALE, origin.getY(), origin.getZ() + dz * SCALE);
    }

    public static BlockPos subspaceSpawn(UUID playerId) {
        int hash = playerId.hashCode();
        int x = (hash & 0xFFFF) * 16;
        int z = ((hash >>> 16) & 0xFFFF) * 16;
        if ((hash & 1) == 0) {
            x = -x;
        }
        if ((hash & 2) == 0) {
            z = -z;
        }
        return new BlockPos(x, FLOOR_Y + 1, z);
    }

    @Nullable
    private static ServerLevel resolveOrigin(MinecraftServer server, SubspaceVoyageData data) {
        if (data.originDim() == null) {
            return null;
        }
        return server.getLevel(data.originDim());
    }

    @Nullable
    private static BlockPos placePortalNear(ServerPlayer caster) {
        BlockPos base = caster.blockPosition().relative(caster.getDirection());
        for (BlockPos candidate : new BlockPos[] {
            base, base.above(), caster.blockPosition(), caster.blockPosition().above()
        }) {
            BlockPos placed = placePortalAt(caster.serverLevel(), candidate);
            if (placed != null) {
                return placed;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos placePortalAt(ServerLevel level, BlockPos pos) {
        BlockPos feet = pos;
        if (!level.getBlockState(feet).canBeReplaced() && !level.getBlockState(feet).isAir()) {
            feet = pos.above();
        }
        if (!level.getBlockState(feet).canBeReplaced() && !level.getBlockState(feet).isAir()) {
            return null;
        }
        level.setBlock(feet, ModBlocks.SUBSPACE_PORTAL.get().defaultBlockState(), 3);
        return feet;
    }

    private static void configurePortal(
            ServerLevel level, BlockPos pos, UUID owner, SubspacePortalBlockEntity.Role role, UUID session) {
        if (level.getBlockEntity(pos) instanceof SubspacePortalBlockEntity be) {
            be.configure(owner, role, session);
        }
    }

    private static void removePortal(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            level.removeBlock(pos, false);
        }
    }

    private static void ensureFloor(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        if (level.getBlockState(floor).isAir()) {
            level.setBlock(floor, Blocks.END_STONE.defaultBlockState(), 3);
        }
        if (!level.getBlockState(feet).isAir()) {
            level.setBlock(feet, Blocks.AIR.defaultBlockState(), 3);
        }
        if (!level.getBlockState(feet.above()).isAir()) {
            level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static BlockPos findSafeLanding(ServerLevel level, BlockPos approx) {
        int x = approx.getX();
        int z = approx.getZ();
        for (int r = 0; r <= 16; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    BlockPos found = scanColumn(level, x + dx, z + dz, approx.getY());
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return new BlockPos(x, Math.max(level.getMinBuildHeight() + 2, approx.getY()), z);
    }

    @Nullable
    private static BlockPos scanColumn(ServerLevel level, int x, int z, int preferY) {
        int min = level.getMinBuildHeight() + 1;
        int max = level.getMaxBuildHeight() - 2;
        int start = Math.min(max, Math.max(min, preferY));
        for (int y = start; y >= min; y--) {
            BlockPos candidate = tryStand(level, x, y, z);
            if (candidate != null) {
                return candidate;
            }
        }
        for (int y = start; y <= max; y++) {
            BlockPos candidate = tryStand(level, x, y, z);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos tryStand(ServerLevel level, int x, int floorY, int z) {
        BlockPos floor = new BlockPos(x, floorY, z);
        BlockPos feet = floor.above();
        BlockPos head = feet.above();
        BlockState floorState = level.getBlockState(floor);
        if (!floorState.blocksMotion() || floorState.liquid()) {
            return null;
        }
        if ((level.getBlockState(feet).isAir() || level.getBlockState(feet).canBeReplaced())
                && (level.getBlockState(head).isAir() || level.getBlockState(head).canBeReplaced())) {
            return feet;
        }
        return null;
    }

    private static void placePlatformIfNeeded(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        if (!level.getBlockState(floor).blocksMotion()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlock(floor.offset(dx, 0, dz), Blocks.END_STONE.defaultBlockState(), 3);
                }
            }
        }
        if (!level.getBlockState(feet).isAir() && !level.getBlockState(feet).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            level.setBlock(feet, Blocks.AIR.defaultBlockState(), 3);
        }
        if (!level.getBlockState(feet.above()).isAir()) {
            level.setBlock(feet.above(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void teleport(ServerPlayer player, ServerLevel dest, BlockPos feet) {
        double x = feet.getX() + 0.5;
        double y = feet.getY();
        double z = feet.getZ() + 0.5;
        if (player.level() == dest) {
            player.teleportTo(x, y, z);
            return;
        }
        player.changeDimension(new DimensionTransition(
                dest,
                new Vec3(x, y, z),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING));
    }

    private static void fx(ServerLevel level, Vec3 pos) {
        SpatialEffects.spawnSpatialParticles(level, pos);
        level.playSound(
                null,
                BlockPos.containing(pos),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.85f,
                0.7f);
    }

    private static int horizontalDistance(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        return (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
    }
}
