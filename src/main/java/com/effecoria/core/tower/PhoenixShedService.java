package com.effecoria.core.tower;

import com.effecoria.block.PhiContactorBlock;
import com.effecoria.block.PhiCouplerBlock;
import com.effecoria.block.PhiSignalBlock;
import com.effecoria.block.PhiTurretBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.content.ModBlockTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.glue.EssenceGlueData;
import com.effecoria.core.loci.LexLociCompiler;
import com.effecoria.core.loci.LociActuator;
import com.effecoria.core.loci.LociEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Hardware Phoenix shed: on owner death, open non-{@link PhiChannel#LIFE} contactors in the
 * glued facility so industry loads drop while life-path contactors stay closed.
 * Contactor CLOSED states are snapshotted on the Ψ-anchor and restored on rematerialize.
 * While the snapshot is held, {@link #reenforceIfNeeded} re-applies the compiled Phoenix
 * word program (shed / turrets / signal).
 */
public final class PhoenixShedService {
    private PhoenixShedService() {}

    /** Snapshot (if none) and shed non-life contactors for the owner's bound tower. */
    public static void onSoulDead(ServerLevel level, BlockPos anchorPos) {
        if (!(level.getBlockEntity(anchorPos) instanceof TowerAnchorBlockEntity anchor)
                || !anchor.bound()
                || !anchor.phoenixEdictEnabled()) {
            return;
        }
        if (!anchor.hasPhoenixSnapshot()) {
            anchor.storePhoenixSnapshot(snapshotContactors(level, anchorPos));
        }
        dispatch(level, anchorPos, true);
    }

    /** Restore pre-death contactor states after successful revive / materialize. */
    public static void onSoulAlive(ServerLevel level, BlockPos anchorPos) {
        if (!(level.getBlockEntity(anchorPos) instanceof TowerAnchorBlockEntity anchor)) {
            return;
        }
        clearTurretAutonomy(level, anchorPos);
        setSignals(level, anchorPos, false);
        ListTag snap = anchor.takePhoenixSnapshot();
        if (snap == null || snap.isEmpty()) {
            return;
        }
        restoreSnapshot(level, snap);
    }

    /**
     * Watchdog: while a phoenix snapshot is held and the edict is enabled, re-open non-life
     * contactors, keep facility turrets autonomous, and pulse the signal lamps.
     */
    public static void reenforceIfNeeded(ServerLevel level, BlockPos anchorPos) {
        if (!(level.getBlockEntity(anchorPos) instanceof TowerAnchorBlockEntity anchor)
                || !anchor.bound()
                || !anchor.phoenixEdictEnabled()
                || !anchor.hasPhoenixSnapshot()) {
            return;
        }
        dispatch(level, anchorPos, false);
    }

    /**
     * Run the compiled Phoenix word program (empty tokens = built-in edict).
     * {@code notifyAlarm} is true only on the rising death edge, not watchdog ticks.
     */
    private static void dispatch(ServerLevel level, BlockPos anchorPos, boolean notifyAlarm) {
        if (!(level.getBlockEntity(anchorPos) instanceof TowerAnchorBlockEntity anchor)) {
            return;
        }
        LexLociCompiler.CompileResult program = LexLociCompiler.compile(anchor.lociTokens());
        if (!program.ok()) {
            program = LexLociCompiler.defaultProgram();
        }
        var dead = program.actuatorsFor(LociEvent.SOUL_DEAD);
        if (dead.contains(LociActuator.SHED)) {
            applyShed(level, anchorPos);
        }
        if (dead.contains(LociActuator.AUTONOM)) {
            armTurretsAutonomous(level, anchorPos);
        } else {
            clearTurretAutonomy(level, anchorPos);
        }
        boolean wantSignal = dead.contains(LociActuator.SIGNAL);
        setSignals(level, anchorPos, wantSignal);
        if (notifyAlarm && wantSignal) {
            notifyOwnerAlarm(level, anchor);
        }
    }

    /** Extinguish facility lamps when the edict is disarmed while a snapshot is still held. */
    public static void clearSignalsIfDisarmed(ServerLevel level, BlockPos anchorPos) {
        if (!(level.getBlockEntity(anchorPos) instanceof TowerAnchorBlockEntity anchor)
                || anchor.phoenixEdictEnabled()) {
            return;
        }
        setSignals(level, anchorPos, false);
    }

    private static ListTag snapshotContactors(ServerLevel level, BlockPos anchorPos) {
        ListTag list = new ListTag();
        for (BlockPos pos : EssenceGlueData.get(level).component(anchorPos)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.PHI_CONTACTOR.get())) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", pos.getX());
            entry.putInt("Y", pos.getY());
            entry.putInt("Z", pos.getZ());
            entry.putBoolean("Closed", state.getValue(PhiContactorBlock.CLOSED));
            list.add(entry);
        }
        return list;
    }

    private static void applyShed(ServerLevel level, BlockPos anchorPos) {
        for (BlockPos pos : EssenceGlueData.get(level).component(anchorPos)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.PHI_CONTACTOR.get())) {
                continue;
            }
            boolean keepLife = protectsLife(level, pos);
            PhiContactorBlock.setClosed(level, pos, keepLife);
        }
    }

    private static void restoreSnapshot(ServerLevel level, ListTag snap) {
        for (int i = 0; i < snap.size(); i++) {
            CompoundTag entry = snap.getCompound(i);
            BlockPos pos = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            if (!level.getBlockState(pos).is(ModBlocks.PHI_CONTACTOR.get())) {
                continue;
            }
            PhiContactorBlock.setClosed(level, pos, entry.getBoolean("Closed"));
        }
    }

    private static void armTurretsAutonomous(ServerLevel level, BlockPos anchorPos) {
        for (BlockPos pos : EssenceGlueData.get(level).component(anchorPos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhiTurretBlockEntity turret) {
                turret.setAutonomous(true);
            }
        }
    }

    private static void clearTurretAutonomy(ServerLevel level, BlockPos anchorPos) {
        for (BlockPos pos : EssenceGlueData.get(level).component(anchorPos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhiTurretBlockEntity turret) {
                turret.clearAutonomy();
            }
        }
    }

    private static void setSignals(ServerLevel level, BlockPos anchorPos, boolean lit) {
        BlockPos soundAt = null;
        for (BlockPos pos : EssenceGlueData.get(level).component(anchorPos)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.PHI_SIGNAL.get())) {
                continue;
            }
            PhiSignalBlock.setLit(level, pos, lit);
            if (soundAt == null) {
                soundAt = pos.immutable();
            }
        }
        if (lit && soundAt != null) {
            level.playSound(null, soundAt, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.85f, 0.55f);
        }
    }

    private static void notifyOwnerAlarm(ServerLevel level, TowerAnchorBlockEntity anchor) {
        UUID owner = anchor.ownerUuid();
        if (owner == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.effecoria.tower.signal_alarm"), true);
    }

    /**
     * Life branch if any adjacent conductor island is stamped {@code life}, or any neighbor is a
     * life-priority load (regen / imprinter / damper).
     */
    static boolean protectsLife(ServerLevel level, BlockPos contactorPos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = contactorPos.relative(dir);
            BlockState adjState = level.getBlockState(adj);
            if (adjState.is(ModBlockTags.PHI_LIFE_LOADS)) {
                return true;
            }
            if (adjState.is(ModBlocks.PHI_COUPLER.get())
                    && adjState.getValue(PhiCouplerBlock.CHANNEL) == PhiChannel.LIFE) {
                return true;
            }
            if (PhiBusNetwork.isConductor(adjState)
                    || (adjState.is(ModBlocks.PHI_CONTACTOR.get())
                            && adjState.getValue(PhiContactorBlock.CLOSED))) {
                if (PhiBusNetwork.channelAt(level, adj) == PhiChannel.LIFE) {
                    return true;
                }
            }
        }
        // Contactor itself may be closed and stamped via a coupler further on the island.
        BlockState self = level.getBlockState(contactorPos);
        if (self.is(ModBlocks.PHI_CONTACTOR.get()) && self.getValue(PhiContactorBlock.CLOSED)) {
            return PhiBusNetwork.channelAt(level, contactorPos) == PhiChannel.LIFE;
        }
        return false;
    }
}
