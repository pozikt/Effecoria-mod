package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiBusNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marks the contactor for network refresh; no extra state. */
public final class PhiContactorBlockEntity extends BlockEntity {
    private int refreshCooldown;

    public PhiContactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_CONTACTOR.get(), pos, state);
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
}
