package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/** Persistent soul anchor for a tower owner. */
public final class FoundationAmuletBlockEntity extends BlockEntity {
    private boolean charged;
    @Nullable private UUID ownerUuid;

    public FoundationAmuletBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDATION_AMULET.get(), pos, state);
    }

    public boolean charged() { return charged; }
    @Nullable public UUID ownerUuid() { return ownerUuid; }

    public void charge(UUID owner) {
        charged = true;
        ownerUuid = owner;
        setChanged();
    }

    public void clearCharge() {
        charged = false;
        ownerUuid = null;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Charged", charged);
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        charged = tag.getBoolean("Charged");
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
