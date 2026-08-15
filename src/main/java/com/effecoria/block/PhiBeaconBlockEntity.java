package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiBeaconMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBeaconIndex;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class PhiBeaconBlockEntity extends BlockEntity implements MenuProvider, NamedFacilityDevice {
    private String beaconName = "";

    public PhiBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_BEACON.get(), pos, state);
    }

    public String beaconName() {
        return beaconName;
    }

    @Override
    public String facilityName() {
        return beaconName;
    }

    @Override
    public boolean setFacilityName(String name) {
        return setBeaconName(name);
    }

    public boolean setBeaconName(String name) {
        if (level == null) {
            return false;
        }
        String trimmed = FacilityNames.sanitize(name);
        if (!trimmed.isEmpty()
                && PhiBeaconIndex.isNameTaken(level.dimension(), trimmed, worldPosition)) {
            return false;
        }
        if (!beaconName.isEmpty()) {
            PhiBeaconIndex.unregisterName(level.dimension(), beaconName);
        }
        beaconName = trimmed;
        if (!beaconName.isEmpty()) {
            PhiBeaconIndex.register(level.dimension(), worldPosition, beaconName);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public void onRemoved() {
        if (level != null && !beaconName.isEmpty()) {
            PhiBeaconIndex.unregister(level.dimension(), worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && !beaconName.isEmpty()) {
            PhiBeaconIndex.register(level.dimension(), worldPosition, beaconName);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.effecoria.phi_beacon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PhiBeaconMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putString("BeaconName", beaconName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        beaconName = tag.getString("BeaconName");
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
