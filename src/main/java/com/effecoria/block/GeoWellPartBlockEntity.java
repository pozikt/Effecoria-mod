package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Stores controller position and the original shell block to restore on disassemble. */
public final class GeoWellPartBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos controller;
    private CompoundTag originalState = new CompoundTag();

    public GeoWellPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEO_WELL_PART.get(), pos, state);
    }

    public void setController(BlockPos controller, CompoundTag originalState) {
        this.controller = controller.immutable();
        this.originalState = originalState.copy();
        setChanged();
    }

    public boolean isOwnedBy(BlockPos core) {
        return controller != null && controller.equals(core);
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controller;
    }

    public BlockState readOriginal(HolderLookup.Provider lookup) {
        if (originalState.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        return NbtUtils.readBlockState(lookup.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), originalState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (controller != null) {
            tag.putLong("Controller", controller.asLong());
        }
        tag.put("Original", originalState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Controller")) {
            controller = BlockPos.of(tag.getLong("Controller"));
        }
        if (tag.contains("Original")) {
            originalState = tag.getCompound("Original");
        }
    }
}
