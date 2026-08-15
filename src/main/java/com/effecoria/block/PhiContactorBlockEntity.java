package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marks the contactor for network refresh; optional Lex label. */
public final class PhiContactorBlockEntity extends BlockEntity implements NamedFacilityDevice {
    private int refreshCooldown;
    private String facilityName = "";

    public PhiContactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_CONTACTOR.get(), pos, state);
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

    public void markDirtyNetwork() {
        refreshCooldown = 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiContactorBlockEntity be) {
        if (be.refreshCooldown > 0) {
            be.refreshCooldown--;
            return;
        }
        be.refreshCooldown = 10;
        boolean energized = state.getValue(PhiContactorBlock.CLOSED) && PhiBusNetwork.findSource(level, pos) != null;
        if (state.getValue(PhiContactorBlock.POWERED) != energized) {
            level.setBlock(pos, state.setValue(PhiContactorBlock.POWERED, energized), 3);
        }
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
