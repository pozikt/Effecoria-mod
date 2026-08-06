package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EssenceBurnerBlockEntity extends BlockEntity {
    public static final int DUST_FUEL_TICKS = 400;

    private int fuelTicks;

    public EssenceBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_BURNER.get(), pos, state);
    }

    public int fuelTicks() {
        return fuelTicks;
    }

    public void addFuel(int ticks) {
        fuelTicks = Math.min(fuelTicks + ticks, 8000);
        setChanged();
    }

    /** Consume one tick of fuel while cooking. */
    public boolean consumeFuelTick() {
        if (fuelTicks <= 0) {
            return false;
        }
        fuelTicks--;
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceBurnerBlockEntity be) {
        if (be.fuelTicks <= 0) {
            if (state.getValue(EssenceBurnerBlock.LIT)) {
                level.setBlock(pos, state.setValue(EssenceBurnerBlock.LIT, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        // Idle burn: slow drain so fuel isn't permanent when unused
        if (level.getGameTime() % 20 == 0) {
            be.fuelTicks = Math.max(0, be.fuelTicks - 1);
            be.setChanged();
            if (be.fuelTicks <= 0) {
                level.setBlock(pos, state.setValue(EssenceBurnerBlock.LIT, false), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuelTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelTicks = tag.getInt("Fuel");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
