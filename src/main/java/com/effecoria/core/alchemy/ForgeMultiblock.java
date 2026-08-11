package com.effecoria.core.alchemy;

import com.effecoria.block.ForgeReactorBlock;
import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.block.ForgeReactorPartBlockEntity;
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
 * Forge Reactor («Кузница») 3×4×3 — one layer taller than Heart.
 *
 * <pre>
 * dy ∈ {-1,0,1,2}, dx,dz ∈ {-1,0,1}; core at (0,0,0)
 * corner pillars (|dx|==1 &amp;&amp; |dz|==1) → void_obsidian
 * floor/roof (dy==-1 or dy==2), non-corners → lead_block
 * side windows (mid layers, |dx|+|dz|==1) → phi_glass
 * </pre>
 */
public final class ForgeMultiblock {
    public static final int DY_MIN = -1;
    public static final int DY_MAX = 2;

    private ForgeMultiblock() {}

    public static void forEachShell(BlockPos center, ShellConsumer consumer) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = DY_MIN; dy <= DY_MAX; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    consumer.accept(cursor, dx, dy, dz);
                }
            }
        }
    }

    public static boolean expectedMaterial(BlockState state, int dx, int dy, int dz) {
        boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
        if (corner) {
            return state.is(ModBlocks.VOID_OBSIDIAN.get());
        }
        if (dy == DY_MIN || dy == DY_MAX) {
            return state.is(ModBlocks.LEAD_BLOCK.get());
        }
        int manhattan = Math.abs(dx) + Math.abs(dz);
        if (manhattan == 1) {
            return state.is(ModBlocks.PHI_GLASS.get());
        }
        return state.is(ModBlocks.LEAD_BLOCK.get());
    }

    public static boolean isMaterialShell(LevelReader level, BlockPos center) {
        if (!level.getBlockState(center).is(ModBlocks.FORGE_REACTOR_CORE.get())) {
            return false;
        }
        boolean[] ok = {true};
        forEachShell(center, (pos, dx, dy, dz) -> {
            if (!ok[0]) {
                return;
            }
            if (!expectedMaterial(level.getBlockState(pos), dx, dy, dz)) {
                ok[0] = false;
            }
        });
        return ok[0];
    }

    public static boolean isAssembled(LevelReader level, BlockPos center) {
        if (!level.getBlockState(center).is(ModBlocks.FORGE_REACTOR_CORE.get())) {
            return false;
        }
        boolean[] ok = {true};
        forEachShell(center, (pos, dx, dy, dz) -> {
            if (!ok[0]) {
                return;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof ForgeReactorPartBlockEntity part) || !part.isOwnedBy(center)) {
                ok[0] = false;
            }
        });
        return ok[0];
    }

    public static void assemble(ServerLevel level, BlockPos center) {
        if (!(level.getBlockEntity(center) instanceof ForgeReactorBlockEntity core) || core.isDismantling()) {
            return;
        }
        if (!isMaterialShell(level, center)) {
            return;
        }
        forEachShell(center, (pos, dx, dy, dz) -> {
            BlockState original = level.getBlockState(pos);
            CompoundTag saved = NbtUtils.writeBlockState(original);
            level.setBlock(pos, ModBlocks.FORGE_REACTOR_PART.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof ForgeReactorPartBlockEntity part) {
                part.setController(center, saved);
            }
        });
        BlockState coreState = level.getBlockState(center);
        if (coreState.is(ModBlocks.FORGE_REACTOR_CORE.get()) && !coreState.getValue(ForgeReactorBlock.FORMED)) {
            level.setBlock(center, coreState.setValue(ForgeReactorBlock.FORMED, true), Block.UPDATE_CLIENTS);
        }
        core.setFormed(true);
        core.setChanged();
        level.sendBlockUpdated(center, coreState, level.getBlockState(center), Block.UPDATE_CLIENTS);
    }

    public static void disassemble(ServerLevel level, BlockPos center) {
        disassemble(level, center, null);
    }

    public static void disassemble(ServerLevel level, BlockPos center, @Nullable BlockPos brokenPart) {
        if (!(level.getBlockEntity(center) instanceof ForgeReactorBlockEntity core)) {
            return;
        }
        if (core.isDismantling()) {
            return;
        }
        core.setDismantling(true);
        try {
            forEachShell(center, (pos, dx, dy, dz) -> {
                if (brokenPart != null && brokenPart.equals(pos)) {
                    return;
                }
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ForgeReactorPartBlockEntity part && part.isOwnedBy(center)) {
                    BlockState restored = part.readOriginal(level.registryAccess());
                    level.setBlock(pos, restored, Block.UPDATE_ALL);
                }
            });
            BlockState coreState = level.getBlockState(center);
            if (coreState.is(ModBlocks.FORGE_REACTOR_CORE.get()) && coreState.getValue(ForgeReactorBlock.FORMED)) {
                level.setBlock(center, coreState.setValue(ForgeReactorBlock.FORMED, false), Block.UPDATE_CLIENTS);
            }
            core.setFormed(false);
            core.onDisassembled();
            core.setChanged();
        } finally {
            core.setDismantling(false);
        }
    }

    public static void onPartBroken(ServerLevel level, BlockPos partPos, ForgeReactorPartBlockEntity part) {
        BlockPos controller = part.getControllerPos();
        if (controller == null) {
            return;
        }
        if (!(level.getBlockEntity(controller) instanceof ForgeReactorBlockEntity core) || core.isDismantling()) {
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
        if (level.getBlockEntity(partOrCore) instanceof ForgeReactorPartBlockEntity part) {
            corePos = part.getControllerPos();
            if (corePos == null) {
                return;
            }
        }
        if (level.getBlockEntity(corePos) instanceof ForgeReactorBlockEntity reactor) {
            if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                    player, com.effecoria.core.technomagic.TechnomagicEra.IV)) {
                return;
            }
            BlockPos finalCore = corePos;
            player.openMenu(reactor, buf -> buf.writeBlockPos(finalCore));
        }
    }

    @FunctionalInterface
    public interface ShellConsumer {
        void accept(BlockPos.MutableBlockPos pos, int dx, int dy, int dz);
    }
}
