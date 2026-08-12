package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OmegaDamperBlockEntity extends BlockEntity {
    public OmegaDamperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMEGA_DAMPER.get(), pos, state);
    }
}
