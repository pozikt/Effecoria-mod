package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marker BE for facility scans; LIT lives on the blockstate. Optional Lex label. */
public final class PhiSignalBlockEntity extends BlockEntity implements NamedFacilityDevice {
    private String facilityName = "";

    public PhiSignalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_SIGNAL.get(), pos, state);
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
