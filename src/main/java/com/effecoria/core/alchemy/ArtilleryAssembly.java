package com.effecoria.core.alchemy;

import com.effecoria.block.PhiArtilleryBaseBlock;
import com.effecoria.block.PhiArtilleryBlockEntity;
import com.effecoria.block.PhiBeamLensBlock;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Floor base + lens on UP face → formed artillery. */
public final class ArtilleryAssembly {
    private ArtilleryAssembly() {}

    public static boolean tryForm(Level level, BlockPos basePos) {
        BlockState base = level.getBlockState(basePos);
        if (!base.is(ModBlocks.PHI_ARTILLERY_BASE.get())) {
            return false;
        }
        BlockPos lensPos = basePos.above();
        BlockState lens = level.getBlockState(lensPos);
        if (!lens.is(ModBlocks.PHI_BEAM_LENS.get())) {
            return false;
        }
        if (base.getValue(PhiArtilleryBaseBlock.FORMED) && lens.getValue(PhiBeamLensBlock.FORMED)) {
            return true;
        }
        level.setBlock(basePos, base.setValue(PhiArtilleryBaseBlock.FORMED, true), 3);
        level.setBlock(lensPos, lens.setValue(PhiBeamLensBlock.FORMED, true), 3);
        if (level.getBlockEntity(basePos) instanceof PhiArtilleryBlockEntity be) {
            be.onFormed();
        }
        return true;
    }

    public static void breakForm(Level level, BlockPos any) {
        BlockPos basePos = any;
        BlockState state = level.getBlockState(any);
        if (state.is(ModBlocks.PHI_BEAM_LENS.get())) {
            basePos = any.below();
        }
        BlockState base = level.getBlockState(basePos);
        BlockPos lensPos = basePos.above();
        BlockState lens = level.getBlockState(lensPos);
        if (base.is(ModBlocks.PHI_ARTILLERY_BASE.get()) && base.getValue(PhiArtilleryBaseBlock.FORMED)) {
            level.setBlock(basePos, base.setValue(PhiArtilleryBaseBlock.FORMED, false), 3);
        }
        if (lens.is(ModBlocks.PHI_BEAM_LENS.get()) && lens.getValue(PhiBeamLensBlock.FORMED)) {
            level.setBlock(lensPos, lens.setValue(PhiBeamLensBlock.FORMED, false), 3);
        }
        if (level.getBlockEntity(basePos) instanceof PhiArtilleryBlockEntity be) {
            be.onBroken();
        }
    }

    public static boolean isFormed(Level level, BlockPos basePos) {
        BlockState base = level.getBlockState(basePos);
        BlockState lens = level.getBlockState(basePos.above());
        return base.is(ModBlocks.PHI_ARTILLERY_BASE.get())
                && base.getValue(PhiArtilleryBaseBlock.FORMED)
                && lens.is(ModBlocks.PHI_BEAM_LENS.get())
                && lens.getValue(PhiBeamLensBlock.FORMED);
    }

    public static Direction facing(Level level, BlockPos basePos) {
        BlockState base = level.getBlockState(basePos);
        if (base.hasProperty(PhiArtilleryBaseBlock.FACING)) {
            return base.getValue(PhiArtilleryBaseBlock.FACING);
        }
        return Direction.NORTH;
    }
}
