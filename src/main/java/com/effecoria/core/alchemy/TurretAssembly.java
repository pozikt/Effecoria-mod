package com.effecoria.core.alchemy;

import com.effecoria.block.TurretMountBlock;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import javax.annotation.Nullable;

/** Helpers for mount + barrel 2-block Φ-turrets. */
public final class TurretAssembly {
    private TurretAssembly() {}

    /** Direction from mount toward the barrel cell. */
    public static Direction barrelDirection(BlockState mountState) {
        AttachFace face = mountState.getValue(TurretMountBlock.FACE);
        return switch (face) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> mountState.getValue(TurretMountBlock.FACING);
        };
    }

    public static BlockPos barrelPos(BlockPos mountPos, BlockState mountState) {
        return mountPos.relative(barrelDirection(mountState));
    }

    @Nullable
    public static BlockPos findMountForBarrel(LevelReader level, BlockPos barrelPos) {
        for (Direction dir : Direction.values()) {
            BlockPos mountPos = barrelPos.relative(dir);
            BlockState mount = level.getBlockState(mountPos);
            if (!mount.is(ModBlocks.TURRET_MOUNT.get())) {
                continue;
            }
            if (barrelPos.equals(barrelPos(mountPos, mount))) {
                return mountPos.immutable();
            }
        }
        return null;
    }

    public static boolean isValidBarrelNeighbor(LevelReader level, BlockPos mountPos, BlockState mountState) {
        BlockPos barrel = barrelPos(mountPos, mountState);
        BlockState state = level.getBlockState(barrel);
        return state.getBlock() instanceof com.effecoria.block.PhiTurretBlock;
    }

    @Nullable
    public static TurretKind barrelKindAt(LevelReader level, BlockPos mountPos, BlockState mountState) {
        BlockState state = level.getBlockState(barrelPos(mountPos, mountState));
        if (state.getBlock() instanceof com.effecoria.block.PhiTurretBlock turret) {
            return turret.kind();
        }
        return null;
    }

    public static void syncFormed(LevelAccessor level, BlockPos mountPos) {
        BlockState mount = level.getBlockState(mountPos);
        if (!mount.is(ModBlocks.TURRET_MOUNT.get())) {
            return;
        }
        TurretKind kind = barrelKindAt(level, mountPos, mount);
        boolean formed = kind != null && kind.isEmitter();
        BlockState next = mount
                .setValue(TurretMountBlock.FORMED, formed)
                .setValue(TurretMountBlock.KIND, formed ? kind : TurretKind.NONE);
        if (!next.equals(mount)) {
            level.setBlock(mountPos, next, 3);
        }
        BlockPos barrelPos = barrelPos(mountPos, mount);
        BlockState barrel = level.getBlockState(barrelPos);
        if (barrel.getBlock() instanceof com.effecoria.block.PhiTurretBlock) {
            boolean barrelFormed = formed;
            if (barrel.getValue(com.effecoria.block.PhiTurretBlock.FORMED) != barrelFormed) {
                level.setBlock(barrelPos, barrel.setValue(com.effecoria.block.PhiTurretBlock.FORMED, barrelFormed), 3);
            }
        }
        if (level.getBlockEntity(mountPos) instanceof com.effecoria.block.PhiTurretBlockEntity be) {
            be.setAssembledKind(formed ? kind : TurretKind.NONE);
        }
    }
}
