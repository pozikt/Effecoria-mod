package com.effecoria.core.alchemy;

import com.effecoria.block.StarReactorBlock;
import com.effecoria.block.StarReactorBlockEntity;
import com.effecoria.block.StarReactorPartBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;

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
 * Star Reactor 5×5×5: material shell → assembled invisible parts + solid BER hull.
 *
 * <pre>
 * outer corners (max==2, all abs==2) → purified_obsidian
 * outer edges   (max==2, two abs==2) → mithril_block
 * outer faces   (max==2, one abs==2) → phi_glass
 * inner shell   (max==1)             → reactor_casing
 * center                             → star_reactor_core
 * </pre>
 */
public final class StarMultiblock {
    public static final int HALF = 2;

    private StarMultiblock() {}

    public static Block expectedShellBlock(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);
        int max = Math.max(ax, Math.max(ay, az));
        if (max == 2) {
            int count2 = (ax == 2 ? 1 : 0) + (ay == 2 ? 1 : 0) + (az == 2 ? 1 : 0);
            if (count2 == 3) {
                return ModBlocks.PURIFIED_OBSIDIAN.get();
            }
            if (count2 == 2) {
                return ModBlocks.MITHRIL_BLOCK.get();
            }
            return ModBlocks.PHI_GLASS.get();
        }
        return ModBlocks.REACTOR_CASING.get();
    }

    public static boolean isMaterialShell(LevelReader level, BlockPos center) {
        BlockState core = level.getBlockState(center);
        if (!core.is(ModBlocks.STAR_REACTOR_CORE.get())) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -HALF; dy <= HALF; dy++) {
            for (int dx = -HALF; dx <= HALF; dx++) {
                for (int dz = -HALF; dz <= HALF; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.getBlockState(cursor).is(expectedShellBlock(dx, dy, dz))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isAssembled(LevelReader level, BlockPos center) {
        if (!level.getBlockState(center).is(ModBlocks.STAR_REACTOR_CORE.get())) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -HALF; dy <= HALF; dy++) {
            for (int dx = -HALF; dx <= HALF; dx++) {
                for (int dz = -HALF; dz <= HALF; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (!(be instanceof StarReactorPartBlockEntity part) || !part.isOwnedBy(center)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void assemble(ServerLevel level, BlockPos center) {
        if (!(level.getBlockEntity(center) instanceof StarReactorBlockEntity core) || core.isDismantling()) {
            return;
        }
        if (!isMaterialShell(level, center)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -HALF; dy <= HALF; dy++) {
            for (int dx = -HALF; dx <= HALF; dx++) {
                for (int dz = -HALF; dz <= HALF; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState original = level.getBlockState(cursor);
                    CompoundTag saved = NbtUtils.writeBlockState(original);
                    level.setBlock(cursor, ModBlocks.STAR_REACTOR_PART.get().defaultBlockState(), Block.UPDATE_ALL);
                    if (level.getBlockEntity(cursor) instanceof StarReactorPartBlockEntity part) {
                        part.setController(center, saved);
                    }
                }
            }
        }
        BlockState coreState = level.getBlockState(center);
        if (coreState.is(ModBlocks.STAR_REACTOR_CORE.get()) && !coreState.getValue(StarReactorBlock.FORMED)) {
            level.setBlock(center, coreState.setValue(StarReactorBlock.FORMED, true), Block.UPDATE_CLIENTS);
        }
        core.setFormed(true);
        core.setChanged();
        level.sendBlockUpdated(center, coreState, level.getBlockState(center), Block.UPDATE_CLIENTS);
    }

    public static void disassemble(ServerLevel level, BlockPos center) {
        disassemble(level, center, null);
    }

    public static void disassemble(ServerLevel level, BlockPos center, @Nullable BlockPos brokenPart) {
        if (!(level.getBlockEntity(center) instanceof StarReactorBlockEntity core)) {
            return;
        }
        if (core.isDismantling()) {
            return;
        }
        core.setDismantling(true);
        try {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dy = -HALF; dy <= HALF; dy++) {
                for (int dx = -HALF; dx <= HALF; dx++) {
                    for (int dz = -HALF; dz <= HALF; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        if (brokenPart != null && brokenPart.equals(cursor)) {
                            continue;
                        }
                        BlockEntity be = level.getBlockEntity(cursor);
                        if (be instanceof StarReactorPartBlockEntity part && part.isOwnedBy(center)) {
                            BlockState restored = part.readOriginal(level.registryAccess());
                            level.setBlock(cursor, restored, Block.UPDATE_ALL);
                        }
                    }
                }
            }
            BlockState coreState = level.getBlockState(center);
            if (coreState.is(ModBlocks.STAR_REACTOR_CORE.get()) && coreState.getValue(StarReactorBlock.FORMED)) {
                level.setBlock(center, coreState.setValue(StarReactorBlock.FORMED, false), Block.UPDATE_CLIENTS);
            }
            core.setFormed(false);
            core.onDisassembled();
            core.setChanged();
        } finally {
            core.setDismantling(false);
        }
    }

    public static void onPartBroken(ServerLevel level, BlockPos partPos, StarReactorPartBlockEntity part) {
        BlockPos controller = part.getControllerPos();
        if (controller == null) {
            return;
        }
        if (!(level.getBlockEntity(controller) instanceof StarReactorBlockEntity core) || core.isDismantling()) {
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
        if (level.getBlockEntity(partOrCore) instanceof StarReactorPartBlockEntity part) {
            corePos = part.getControllerPos();
            if (corePos == null) {
                return;
            }
        }
        if (level.getBlockEntity(corePos) instanceof StarReactorBlockEntity star) {
            if (!TechnomagicGates.checkOperate(player, TechnomagicEra.VI)) {
                return;
            }
            BlockPos finalCore = corePos;
            player.openMenu(star, buf -> buf.writeBlockPos(finalCore));
        }
    }
}
