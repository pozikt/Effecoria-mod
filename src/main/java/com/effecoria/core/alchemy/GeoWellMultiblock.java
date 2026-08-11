package com.effecoria.core.alchemy;

import com.effecoria.block.GeoWellBlock;
import com.effecoria.block.GeoWellBlockEntity;
import com.effecoria.block.GeoWellPartBlockEntity;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Geo Well 3×3×3: material shell → assembled invisible parts + solid BER hull.
 *
 * <pre>
 * corners (|dx|+|dy|+|dz|==3) → purified_obsidian
 * edges   (|dx|+|dy|+|dz|==2) → geo_casing
 * faces   (|dx|+|dy|+|dz|==1) → phi_concrete
 * center  → geo_well_core
 * </pre>
 */
public final class GeoWellMultiblock {
    private GeoWellMultiblock() {}

    public static boolean isMaterialShell(LevelReader level, BlockPos center) {
        BlockState core = level.getBlockState(center);
        if (!core.is(ModBlocks.GEO_WELL_CORE.get())) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (manhattan == 3) {
                        if (!state.is(ModBlocks.PURIFIED_OBSIDIAN.get())) {
                            return false;
                        }
                    } else if (manhattan == 2) {
                        if (!state.is(ModBlocks.GEO_CASING.get())) {
                            return false;
                        }
                    } else if (manhattan == 1) {
                        if (!state.is(ModBlocks.PHI_CONCRETE.get())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Assembled: every shell cell is a part owned by this core. */
    public static boolean isAssembled(LevelReader level, BlockPos center) {
        if (!level.getBlockState(center).is(ModBlocks.GEO_WELL_CORE.get())) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (!(be instanceof GeoWellPartBlockEntity part) || !part.isOwnedBy(center)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** @deprecated use {@link #isMaterialShell} or {@link #isAssembled} */
    @Deprecated
    public static boolean isValid(LevelReader level, BlockPos center) {
        return isMaterialShell(level, center) || isAssembled(level, center);
    }

    public static void assemble(ServerLevel level, BlockPos center) {
        if (!(level.getBlockEntity(center) instanceof GeoWellBlockEntity core) || core.isDismantling()) {
            return;
        }
        if (!isMaterialShell(level, center)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState original = level.getBlockState(cursor);
                    CompoundTag saved = NbtUtils.writeBlockState(original);
                    level.setBlock(cursor, ModBlocks.GEO_WELL_PART.get().defaultBlockState(), Block.UPDATE_ALL);
                    if (level.getBlockEntity(cursor) instanceof GeoWellPartBlockEntity part) {
                        part.setController(center, saved);
                    }
                }
            }
        }
        BlockState coreState = level.getBlockState(center);
        if (coreState.is(ModBlocks.GEO_WELL_CORE.get()) && !coreState.getValue(GeoWellBlock.FORMED)) {
            level.setBlock(center, coreState.setValue(GeoWellBlock.FORMED, true), Block.UPDATE_CLIENTS);
        }
        core.setFormed(true);
        core.setChanged();
        level.sendBlockUpdated(center, coreState, level.getBlockState(center), Block.UPDATE_CLIENTS);
    }

    public static void disassemble(ServerLevel level, BlockPos center) {
        disassemble(level, center, null);
    }

    /**
     * Restores shell materials. If {@code brokenPart} is set (player mined a cell), that cell is
     * skipped and its original is dropped as an item — the break already cleared the block.
     */
    public static void disassemble(ServerLevel level, BlockPos center, @Nullable BlockPos brokenPart) {
        if (!(level.getBlockEntity(center) instanceof GeoWellBlockEntity core)) {
            return;
        }
        if (core.isDismantling()) {
            return;
        }
        core.setDismantling(true);
        try {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (brokenPart != null && brokenPart.equals(cursor)) {
                            continue;
                        }
                        BlockEntity be = level.getBlockEntity(cursor);
                        if (be instanceof GeoWellPartBlockEntity part && part.isOwnedBy(center)) {
                            BlockState restored = part.readOriginal(level.registryAccess());
                            level.setBlock(cursor, restored, Block.UPDATE_ALL);
                        }
                    }
                }
            }
            BlockState coreState = level.getBlockState(center);
            if (coreState.is(ModBlocks.GEO_WELL_CORE.get()) && coreState.getValue(GeoWellBlock.FORMED)) {
                level.setBlock(center, coreState.setValue(GeoWellBlock.FORMED, false), Block.UPDATE_CLIENTS);
            }
            core.setFormed(false);
            core.onDisassembled();
            core.setChanged();
        } finally {
            core.setDismantling(false);
        }
    }

    /** Called from a part's {@code onRemove} before the cell is gone. */
    public static void onPartBroken(ServerLevel level, BlockPos partPos, GeoWellPartBlockEntity part) {
        BlockPos controller = part.getControllerPos();
        if (controller == null) {
            return;
        }
        if (!(level.getBlockEntity(controller) instanceof GeoWellBlockEntity core) || core.isDismantling()) {
            return;
        }
        BlockState original = part.readOriginal(level.registryAccess());
        if (!original.isAir()) {
            Block.popResource(level, partPos, new ItemStack(original.getBlock()));
        }
        disassemble(level, controller, partPos);
    }

    public static void openCore(Level level, BlockPos partOrCore, ServerPlayer player) {
        BlockPos corePos = partOrCore;
        if (level.getBlockEntity(partOrCore) instanceof GeoWellPartBlockEntity part) {
            corePos = part.getControllerPos();
            if (corePos == null) {
                return;
            }
        }
        if (level.getBlockEntity(corePos) instanceof GeoWellBlockEntity well) {
            if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                    player, com.effecoria.core.technomagic.TechnomagicEra.V)) {
                return;
            }
            BlockPos finalCore = corePos;
            player.openMenu(well, buf -> buf.writeBlockPos(finalCore));
        }
    }
}
