package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;
import com.effecoria.core.tower.PhoenixShedService;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Facility watchdog: every {@link TowerAnchorBlockEntity#PHOENIX_WATCHDOG_INTERVAL} ticks,
 * reenforce Phoenix topology when the bound computer is present; otherwise silence alarm.
 */
public final class PhiWatchdogBlockEntity extends BlockEntity implements NamedFacilityDevice {
    private String facilityName = "";

    public PhiWatchdogBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_WATCHDOG.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiWatchdogBlockEntity be) {
        if (!(level instanceof ServerLevel server)
                || level.getGameTime() % TowerAnchorBlockEntity.PHOENIX_WATCHDOG_INTERVAL != 0L) {
            return;
        }
        TowerAnchorBlockEntity computer =
                TowerFacility.findComputer(server, pos).filter(TowerAnchorBlockEntity::bound).orElse(null);
        if (computer == null) {
            PhiWatchdogBlock.setLit(level, pos, true);
            return;
        }
        PhiWatchdogBlock.setLit(level, pos, false);
        if (computer.hasPhoenixSnapshot() && computer.phoenixEdictEnabled()) {
            PhoenixShedService.reenforceIfNeeded(server, computer.getBlockPos());
        }
    }

    @Override
    public String facilityName() {
        return facilityName;
    }

    @Override
    public boolean setFacilityName(String name) {
        String next = FacilityNames.sanitize(name);
        if (next.equals(facilityName)) {
            return true;
        }
        facilityName = next;
        FacilityNames.markNamed(this);
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        FacilityNames.save(tag, facilityName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        facilityName = FacilityNames.load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
