package com.effecoria.effect.spatial;

import com.effecoria.block.SubspacePortalBlock;
import com.effecoria.block.SubspacePortalBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.world.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Subspace voyage — walk a foggy flat world where 1 block ≈ 100 overworld blocks.
 *
 * <p>Entry/exit portals stay open until replaced by their creating Spatial mage (exit move)
 * or cleaned up on death of the session owner. Any player may walk through an open portal.
 */
public final class SubspaceVoyageService {
    public static final int SCALE = 100;
    /** Flat gen places one Φ-veil layer at {@code min_y} (0); stand / portal feet are one above. */
    private static final int FLOOR_Y = 0;

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
        // Stale voyage flags (e.g. after crash) must not block a new independent gate.
        if (data.active() || data.pendingEntry()) {
            if (!hasLivingEntryPortal(caster.server, data)) {
                data.clear();
                set(caster, data);
            } else {
                caster.displayClientMessage(Component.translatable("message.effecoria.subspace.already_active"), true);
                return;
            }
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
        // Each voyage gets its own hyperspace yard — never reuse the caster's permanent hash.
        BlockPos subspaceEntry = subspaceAnchor(session);
        ResourceKey<Level> originDim = caster.level().dimension();
        BlockPos originPos = caster.blockPosition().immutable();

        configurePortal(
                caster.serverLevel(),
                portalPos,
                caster.getUUID(),
                SubspacePortalBlockEntity.Role.ENTRY,
                session,
                originDim,
                originPos,
                subspaceEntry,
                null);

        data.beginPending(session, caster.getUUID(), originDim, originPos, portalPos, subspaceEntry);
        set(caster, data);

        // Caster must walk in — arm a short grace so placement never sucks them in.
        armPortalGrace(caster.serverLevel(), portalPos, caster.getUUID(), 40L);

        // Twin return puncture at the hyperspace yard (was missing — arrivals had no gate).
        ServerLevel subspace = ModDimensions.subspace(caster.server);
        if (subspace != null) {
            ensureYardReturnPortal(
                    subspace,
                    caster.serverLevel(),
                    portalPos,
                    caster.getUUID(),
                    session,
                    originDim,
                    originPos,
                    subspaceEntry,
                    null);
        }

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

        // Host may relocate their exit; other Spatial mages on the same trip place an extra exit
        // without deleting the host's gate (async).
        boolean host = data.sessionOwner() == null || caster.getUUID().equals(data.sessionOwner());
        if (host) {
            if (data.exitPortalSubspacePos() != null) {
                removePortal(caster.serverLevel(), data.exitPortalSubspacePos());
            }
            if (data.exitPortalOverworldPos() != null) {
                removePortal(originLevel, data.exitPortalOverworldPos());
            }
        }

        BlockPos subspaceExit = placePortalNear(caster);
        if (subspaceExit == null) {
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.no_space"), true);
            return;
        }

        BlockPos mapped = mapToOrigin(data, caster.blockPosition());
        BlockPos overworldExit = findSafeLanding(originLevel, mapped);
        placePlatformIfNeeded(originLevel, overworldExit);
        // Keep overworld exit one block beside the landing pad, not under the traveler's feet.
        Direction exitFacing = caster.getDirection();
        BlockPos overworldPlaced = placePortalAt(
                originLevel, overworldExit.relative(exitFacing), exitFacing);
        if (overworldPlaced == null) {
            overworldPlaced = placePortalAt(originLevel, overworldExit.relative(exitFacing.getClockWise()), exitFacing);
        }
        if (overworldPlaced == null) {
            removePortal(caster.serverLevel(), subspaceExit);
            caster.displayClientMessage(Component.translatable("message.effecoria.subspace.no_space"), true);
            return;
        }
        overworldExit = overworldPlaced;

        configurePortal(
                caster.serverLevel(),
                subspaceExit,
                caster.getUUID(),
                SubspacePortalBlockEntity.Role.EXIT,
                data.sessionId(),
                data.originDim(),
                data.originPos(),
                data.entrySubspacePos(),
                overworldExit);
        configurePortal(
                originLevel,
                overworldExit,
                caster.getUUID(),
                SubspacePortalBlockEntity.Role.EXIT,
                data.sessionId(),
                data.originDim(),
                data.originPos(),
                data.entrySubspacePos(),
                overworldExit);

        data.setExitPortals(subspaceExit, overworldExit);
        set(caster, data);

        armPortalGrace(caster.serverLevel(), subspaceExit, caster.getUUID(), 40L);

        if (host) {
            syncSessionExitForNearbyPlayers(caster, data);
        }

        fx(caster.serverLevel(), Vec3.atCenterOf(subspaceExit));
        fx(originLevel, Vec3.atCenterOf(overworldExit));
        int dist = horizontalDistance(data.originPos(), overworldExit);
        caster.displayClientMessage(Component.translatable("message.effecoria.subspace.exit_opened", dist), true);
    }

    public static void onPortalTouch(ServerPlayer player, SubspacePortalBlockEntity portal) {
        if (portal.sessionId() == null) {
            return;
        }

        if (portal.role() == SubspacePortalBlockEntity.Role.ENTRY && !ModDimensions.isSubspace(player.level())) {
            enterViaPortal(player, portal);
            return;
        }

        if (portal.role() == SubspacePortalBlockEntity.Role.EXIT && ModDimensions.isSubspace(player.level())) {
            exitViaPortal(player, portal);
        }
    }

    /**
     * Walk living non-players through an open hyperspace gate (mobs only; no voyage attachment).
     */
    public static void transportNonPlayer(Entity entity, SubspacePortalBlockEntity portal) {
        if (!(entity instanceof LivingEntity) || entity instanceof ServerPlayer || portal.sessionId() == null) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (portal.role() == SubspacePortalBlockEntity.Role.ENTRY && !ModDimensions.isSubspace(level)) {
            ServerLevel subspace = ModDimensions.subspace(level.getServer());
            if (subspace == null || portal.entrySubspacePos() == null) {
                return;
            }
            ensureYardReturnPortal(
                    subspace,
                    level,
                    portal.getBlockPos(),
                    portal.owner() != null ? portal.owner() : entity.getUUID(),
                    portal.sessionId(),
                    portal.originDim(),
                    portal.originPos(),
                    portal.entrySubspacePos(),
                    null);
            BlockPos spawn = safeLandingBeside(portal.entrySubspacePos());
            ensureFloor(subspace, spawn);
            SubspaceTerrain.sanitizeAround(subspace, spawn, 8);
            teleportEntity(entity, subspace, spawn);
            fx(subspace, Vec3.atCenterOf(spawn));
            return;
        }

        if (portal.role() == SubspacePortalBlockEntity.Role.EXIT && ModDimensions.isSubspace(level)) {
            BlockPos landing = portal.exitOverworldPos();
            ResourceKey<Level> destDim = portal.originDim();
            if (landing == null || destDim == null) {
                return;
            }
            ServerLevel originLevel = level.getServer().getLevel(destDim);
            if (originLevel == null) {
                return;
            }
            placePlatformIfNeeded(originLevel, landing);
            teleportEntity(entity, originLevel, landing);
            fx(originLevel, Vec3.atCenterOf(landing));
        }
    }

    private static void enterViaPortal(ServerPlayer player, SubspacePortalBlockEntity portal) {
        ServerLevel subspace = ModDimensions.subspace(player.server);
        if (subspace == null
                || portal.sessionId() == null
                || portal.originDim() == null
                || portal.originPos() == null
                || portal.entrySubspacePos() == null) {
            return;
        }

        SubspaceVoyageData data = get(player);
        // Already inside this voyage — ignore re-entry spam.
        if (data.active()
                && portal.sessionId().equals(data.sessionId())
                && ModDimensions.isSubspace(player.level())) {
            return;
        }

        data.joinSession(
                portal.sessionId(),
                portal.owner(),
                portal.originDim(),
                portal.originPos(),
                portal.getBlockPos(),
                portal.entrySubspacePos());
        set(player, data);

        ensureYardReturnPortal(
                subspace,
                player.serverLevel(),
                portal.getBlockPos(),
                portal.owner() != null ? portal.owner() : player.getUUID(),
                portal.sessionId(),
                portal.originDim(),
                portal.originPos(),
                portal.entrySubspacePos(),
                player.getUUID());

        BlockPos spawn = safeLandingBeside(portal.entrySubspacePos());
        ensureFloor(subspace, spawn);
        SubspaceTerrain.sanitizeAround(subspace, spawn, 12);
        SubspaceTerrain.sanitizeAround(subspace, portal.entrySubspacePos(), 8);
        teleport(player, subspace, spawn);
        // Don't bounce straight into the yard return puncture.
        armPortalGrace(subspace, spawn, player.getUUID(), 45L);
        armPortalGrace(subspace, portal.entrySubspacePos(), player.getUUID(), 45L);
        fx(subspace, Vec3.atCenterOf(spawn));
        player.displayClientMessage(Component.translatable("message.effecoria.subspace.entered"), true);
    }

    private static void exitViaPortal(ServerPlayer player, SubspacePortalBlockEntity portal) {
        BlockPos landing = portal.exitOverworldPos();
        ResourceKey<Level> destDim = portal.originDim();
        if (landing == null || destDim == null) {
            SubspaceVoyageData data = get(player);
            landing = data.exitPortalOverworldPos();
            destDim = data.originDim();
        }
        if (landing == null || destDim == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.subspace.no_exit"), true);
            return;
        }

        ServerLevel originLevel = player.server.getLevel(destDim);
        if (originLevel == null) {
            return;
        }

        BlockPos stand = landing.immutable();
        SubspaceVoyageData data = get(player);
        UUID session = portal.sessionId() != null ? portal.sessionId() : data.sessionId();
        boolean ownerLeaving = (portal.owner() != null && portal.owner().equals(player.getUUID()))
                || (data.sessionOwner() != null && data.sessionOwner().equals(player.getUUID()));

        if (ownerLeaving) {
            collapseSessionGates(player.server, session, data);
            ejectSessionPassengers(player.server, session, player);
        }

        data.leaveVoyageKeepPortals();
        set(player, data);

        // Stand beside the overworld exit puncture — never inside it / never half-delete it here.
        BlockPos standBeside = stand.relative(Direction.NORTH);
        if (originLevel.getBlockState(stand).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            Direction face = originLevel.getBlockState(stand).getValue(SubspacePortalBlock.FACING);
            standBeside = SubspacePortalBlock.basePos(originLevel.getBlockState(stand), stand)
                    .relative(face.getOpposite());
        }
        placePlatformIfNeeded(originLevel, standBeside);
        if (ownerLeaving) {
            // Gates already collapsed above; landing pad only.
        }
        teleport(player, originLevel, standBeside);
        armPortalGrace(originLevel, standBeside, player.getUUID(), 45L);
        fx(originLevel, Vec3.atCenterOf(standBeside));
        player.displayClientMessage(
                Component.translatable(
                        ownerLeaving
                                ? "message.effecoria.subspace.exited_host"
                                : "message.effecoria.subspace.exited"),
                true);
    }

    /** Remove entry + all exit portals for this voyage session. */
    private static void collapseSessionGates(
            MinecraftServer server, @Nullable UUID session, SubspaceVoyageData ownerData) {
        collapsePortals(server, ownerData);
        if (session == null) {
            return;
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            SubspaceVoyageData otherData = get(other);
            if (session.equals(otherData.sessionId())) {
                collapsePortals(server, otherData);
            }
        }
    }

    /** When the host closes the gates, pull remaining travelers back to the origin point. */
    private static void ejectSessionPassengers(MinecraftServer server, @Nullable UUID session, ServerPlayer owner) {
        if (session == null) {
            return;
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == owner) {
                continue;
            }
            SubspaceVoyageData otherData = get(other);
            if (!session.equals(otherData.sessionId())) {
                continue;
            }
            ResourceKey<Level> originDim = otherData.originDim();
            BlockPos originPos = otherData.originPos();
            otherData.leaveVoyageKeepPortals();
            set(other, otherData);
            if (ModDimensions.isSubspace(other.level()) && originDim != null && originPos != null) {
                ServerLevel origin = server.getLevel(originDim);
                if (origin != null) {
                    placePlatformIfNeeded(origin, originPos);
                    teleport(other, origin, originPos);
                    fx(origin, Vec3.atCenterOf(originPos));
                }
            }
            other.displayClientMessage(Component.translatable("message.effecoria.subspace.gates_closed"), true);
        }
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
        // Do not tear down portals — other players / the Spatial mage may still need them.
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
        if (subspace != null) {
            if (data.exitPortalSubspacePos() != null) {
                removePortal(subspace, data.exitPortalSubspacePos());
            }
            // Yard twin of the overworld entry — always at the voyage anchor.
            if (data.entrySubspacePos() != null) {
                removePortal(subspace, data.entrySubspacePos());
            }
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

    /**
     * Hyperspace rendezvous for one voyage session (party meets here).
     * Seeded by {@code sessionId} so consecutive voyages do not stack on the same yard.
     */
    public static BlockPos subspaceAnchor(UUID sessionId) {
        int hash = sessionId.hashCode();
        int x = (hash & 0xFFFF) % 4000;
        int z = ((hash >>> 16) & 0xFFFF) % 4000;
        if ((hash & 1) == 0) {
            x = -x;
        }
        if ((hash & 2) == 0) {
            z = -z;
        }
        return new BlockPos(x, FLOOR_Y + 1, z);
    }

    /** @deprecated use {@link #subspaceAnchor(UUID)} */
    @Deprecated
    public static BlockPos subspaceSpawn(UUID playerId) {
        return subspaceAnchor(playerId);
    }

    private static boolean hasLivingEntryPortal(MinecraftServer server, SubspaceVoyageData data) {
        if (data.originDim() == null || data.entryPortalPos() == null) {
            return false;
        }
        ServerLevel origin = server.getLevel(data.originDim());
        return origin != null && origin.getBlockState(data.entryPortalPos()).is(ModBlocks.SUBSPACE_PORTAL.get());
    }

    private static void syncSessionExitForNearbyPlayers(ServerPlayer caster, SubspaceVoyageData ownerData) {
        UUID session = ownerData.sessionId();
        if (session == null) {
            return;
        }
        for (ServerPlayer other : caster.server.getPlayerList().getPlayers()) {
            if (other == caster) {
                continue;
            }
            SubspaceVoyageData otherData = get(other);
            if (!session.equals(otherData.sessionId()) || !otherData.active()) {
                continue;
            }
            otherData.setExitPortals(ownerData.exitPortalSubspacePos(), ownerData.exitPortalOverworldPos());
            set(other, otherData);
        }
    }

    @Nullable
    private static ServerLevel resolveOrigin(MinecraftServer server, SubspaceVoyageData data) {
        if (data.originDim() == null) {
            return null;
        }
        return server.getLevel(data.originDim());
    }

    /**
     * Place (or refresh) the hyperspace-side twin of the overworld ENTRY puncture.
     * Linked as EXIT back to that entry so voyagers can walk home without casting again.
     * Not tracked as {@code exitPortal*} — those remain the optional 1∶100 mapped far-exit.
     */
    private static void ensureYardReturnPortal(
            ServerLevel subspace,
            ServerLevel originLevel,
            BlockPos overworldPortalPos,
            UUID owner,
            UUID session,
            @Nullable ResourceKey<Level> originDim,
            @Nullable BlockPos originPos,
            BlockPos yard,
            @Nullable UUID gracePlayer) {
        if (yard == null || session == null) {
            return;
        }
        // Touch chunk so setBlock cannot no-op on an unloaded section.
        subspace.getChunkAt(yard);

        BlockState owState = originLevel.getBlockState(overworldPortalPos);
        BlockPos overworldBase = overworldPortalPos;
        Direction face = Direction.NORTH;
        if (owState.is(ModBlocks.SUBSPACE_PORTAL.get())) {
            overworldBase = SubspacePortalBlock.basePos(owState, overworldPortalPos);
            face = owState.getValue(SubspacePortalBlock.FACING);
        }

        preparePortalColumn(subspace, yard);
        BlockPos placed = yard;
        if (!subspace.getBlockState(yard).is(ModBlocks.SUBSPACE_PORTAL.get())
                && !subspace.getBlockState(yard.above()).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            BlockPos result = placePortalAt(subspace, yard, face);
            if (result == null) {
                return;
            }
            placed = result;
        } else if (subspace.getBlockState(yard).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            placed = SubspacePortalBlock.basePos(subspace.getBlockState(yard), yard);
        } else if (subspace.getBlockState(yard.above()).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            placed = SubspacePortalBlock.basePos(subspace.getBlockState(yard.above()), yard.above());
        }

        configurePortal(
                subspace,
                placed,
                owner,
                SubspacePortalBlockEntity.Role.EXIT,
                session,
                originDim,
                originPos,
                placed,
                overworldBase);

        if (gracePlayer != null) {
            armPortalGrace(subspace, placed, gracePlayer, 45L);
        }
        fx(subspace, Vec3.atCenterOf(placed));
    }

    /** Clear the two-block portal volume only — never place floor under a hyperspace gate. */
    private static void preparePortalColumn(ServerLevel level, BlockPos feet) {
        for (BlockPos at : new BlockPos[] {feet, feet.above()}) {
            BlockState state = level.getBlockState(at);
            if (state.is(ModBlocks.SUBSPACE_PORTAL.get()) || state.isAir()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, at);
            if (state.canBeReplaced() || (hardness >= 0f && hardness < 50f)) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void configurePortal(
            ServerLevel level,
            BlockPos pos,
            UUID owner,
            SubspacePortalBlockEntity.Role role,
            UUID session,
            @Nullable ResourceKey<Level> originDim,
            @Nullable BlockPos originPos,
            @Nullable BlockPos entrySubspacePos,
            @Nullable BlockPos exitOverworldPos) {
        BlockPos base = pos;
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.SUBSPACE_PORTAL.get())) {
            base = SubspacePortalBlock.basePos(state, pos);
        }
        for (BlockPos at : new BlockPos[] {base, base.above()}) {
            if (level.getBlockEntity(at) instanceof SubspacePortalBlockEntity be) {
                be.configure(owner, role, session, originDim, originPos, entrySubspacePos, exitOverworldPos);
            }
        }
    }

    @Nullable
    private static BlockPos placePortalNear(ServerPlayer caster) {
        Direction facing = caster.getDirection();
        ServerLevel level = caster.serverLevel();
        BlockPos feet = caster.blockPosition();
        // Prefer 1–2 blocks in front — never the caster's own column (instant teleport).
        BlockPos[] candidates = new BlockPos[] {
            feet.relative(facing, 2),
            feet.relative(facing, 1),
            feet.relative(facing, 2).relative(facing.getClockWise()),
            feet.relative(facing, 2).relative(facing.getCounterClockWise()),
            feet.relative(facing, 1).relative(facing.getClockWise()),
            feet.relative(facing, 1).relative(facing.getCounterClockWise()),
            feet.relative(facing, 3)
        };
        for (BlockPos candidate : candidates) {
            BlockPos placed = placePortalAt(level, candidate, facing);
            if (placed != null && !overlapsPlayerColumn(placed, feet)) {
                return placed;
            }
        }
        return null;
    }

    private static boolean overlapsPlayerColumn(BlockPos portalFeet, BlockPos playerFeet) {
        return portalFeet.getX() == playerFeet.getX()
                && portalFeet.getZ() == playerFeet.getZ()
                && Math.abs(portalFeet.getY() - playerFeet.getY()) <= 1;
    }

    @Nullable
    private static BlockPos placePortalAt(ServerLevel level, BlockPos pos, Direction facing) {
        BlockPos feet = pos;
        if (!isPortalReplaceable(level, feet) || !isPortalReplaceable(level, feet.above())) {
            // Try one up if standing on a slab-like obstruction at the aim cell.
            feet = pos.above();
        }
        BlockPos head = feet.above();
        if (!isPortalReplaceable(level, feet) || !isPortalReplaceable(level, head)) {
            return null;
        }
        // Overworld gates need footing; hyperspace portals may float (no veil pads under gates).
        if (!ModDimensions.isSubspace(level)) {
            BlockState below = level.getBlockState(feet.below());
            if (!below.blocksMotion() && !below.is(ModBlocks.SUBSPACE_PORTAL.get())) {
                return null;
            }
        }
        Direction horizontal = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        BlockState lower = ModBlocks.SUBSPACE_PORTAL
                .get()
                .defaultBlockState()
                .setValue(SubspacePortalBlock.FACING, horizontal)
                .setValue(SubspacePortalBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.setValue(SubspacePortalBlock.HALF, DoubleBlockHalf.UPPER);
        level.setBlock(feet, lower, 3);
        level.setBlock(head, upper, 3);
        return feet;
    }

    private static boolean isPortalReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static void removePortal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.SUBSPACE_PORTAL.get())) {
            BlockState above = level.getBlockState(pos.above());
            BlockState below = level.getBlockState(pos.below());
            if (above.is(ModBlocks.SUBSPACE_PORTAL.get())) {
                state = above;
                pos = pos.above();
            } else if (below.is(ModBlocks.SUBSPACE_PORTAL.get())) {
                state = below;
                pos = pos.below();
            } else {
                return;
            }
        }
        BlockPos base = SubspacePortalBlock.basePos(state, pos);
        // Remove upper first so neighbor updates don't fight half-state.
        level.removeBlock(base.above(), false);
        level.removeBlock(base, false);
    }

    /** Clear air for standing — never punch out an existing subspace puncture. */
    private static void ensureFloor(ServerLevel level, BlockPos feet) {
        SubspaceTerrain.ensureFloorCell(level, feet);
        clearStandCell(level, feet);
        clearStandCell(level, feet.above());
    }

    private static void clearStandCell(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.SUBSPACE_PORTAL.get())) {
            return;
        }
        if (!state.isAir() && (state.canBeReplaced() || !state.blocksMotion())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0f && state.getDestroySpeed(level, pos) < 50f) {
            // Soft debris only — don't chew through bedrock/obsidian pads.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static BlockPos safeLandingBeside(BlockPos entry) {
        // Offset from the voyage anchor so arrivals don't stand inside an exit puncture.
        return entry.offset(-2, 0, -2);
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
                && (level.getBlockState(head).isAir() || level.getBlockState(head).canBeReplaced())
                && !level.getBlockState(feet).is(ModBlocks.SUBSPACE_PORTAL.get())
                && !level.getBlockState(head).is(ModBlocks.SUBSPACE_PORTAL.get())) {
            return feet;
        }
        return null;
    }

    private static void armPortalGrace(ServerLevel level, BlockPos near, UUID traveler, long ticks) {
        long until = level.getGameTime() + ticks;
        for (BlockPos at : new BlockPos[] {near, near.above(), near.below(), near.north(), near.south(), near.east(), near.west()}) {
            if (level.getBlockEntity(at) instanceof SubspacePortalBlockEntity portal) {
                portal.armCooldown(traveler, until);
            }
        }
        BlockPos base = near;
        BlockState state = level.getBlockState(near);
        if (state.is(ModBlocks.SUBSPACE_PORTAL.get())) {
            base = SubspacePortalBlock.basePos(state, near);
            if (level.getBlockEntity(base) instanceof SubspacePortalBlockEntity portal) {
                portal.armCooldown(traveler, until);
            }
            if (level.getBlockEntity(base.above()) instanceof SubspacePortalBlockEntity portal) {
                portal.armCooldown(traveler, until);
            }
        }
    }

    private static void placePlatformIfNeeded(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        if (!level.getBlockState(floor).blocksMotion() || level.getBlockState(floor).is(Blocks.END_STONE)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos at = floor.offset(dx, 0, dz);
                    if (!level.getBlockState(at).is(ModBlocks.SUBSPACE_PORTAL.get())) {
                        level.setBlock(at, SubspaceTerrain.floorState(), 3);
                    }
                }
            }
        }
        clearStandCell(level, feet);
        clearStandCell(level, feet.above());
    }

    private static void teleport(ServerPlayer player, ServerLevel dest, BlockPos feet) {
        teleportEntity(player, dest, feet);
    }

    private static void teleportEntity(Entity entity, ServerLevel dest, BlockPos feet) {
        double x = feet.getX() + 0.5;
        double y = feet.getY();
        double z = feet.getZ() + 0.5;
        if (entity.level() == dest) {
            entity.teleportTo(x, y, z);
            return;
        }
        entity.changeDimension(new DimensionTransition(
                dest,
                new Vec3(x, y, z),
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
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
