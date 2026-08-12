package com.effecoria.core.tower;

import com.effecoria.block.RegenChamberBlock;
import com.effecoria.block.RegenChamberBlockEntity;
import com.effecoria.block.RegenChamberPartBlockEntity;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Regen Capsule 4×3×4 open-top: material shell → invisible parts + BER hull + fillable Φ-bath.
 *
 * <pre>
 * Relative to core at floor cavity corner (0,0,0):
 *   AABB: dx∈[-1,2], dy∈[0,2], dz∈[-1,2]
 *   Open air: dx∈{0,1}, dz∈{0,1}, dy∈{1,2}  (no roof)
 *   Floor cavity (shell): dx∈{0,1}, dz∈{0,1}, dy=0  (includes core)
 *
 * Materials:
 *   outer corners → purified_obsidian
 *   cavity floor (non-core) → phi_concrete
 *   upper perimeter walls → phi_glass
 *   remaining shell → reactor_casing
 * </pre>
 */
public final class RegenChamberMultiblock {
    public static final int CAPACITY = 12;

    private RegenChamberMultiblock() {}

    public static boolean inAabb(int dx, int dy, int dz) {
        return dx >= -1 && dx <= 2 && dy >= 0 && dy <= 2 && dz >= -1 && dz <= 2;
    }

    /** Open air / fluid column above the floor (not shell). */
    public static boolean isAirCavity(int dx, int dy, int dz) {
        return dy >= 1 && dy <= 2 && dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
    }

    public static boolean isShellCell(int dx, int dy, int dz) {
        return inAabb(dx, dy, dz) && !isAirCavity(dx, dy, dz);
    }

    public static boolean isCoreOffset(int dx, int dy, int dz) {
        return dx == 0 && dy == 0 && dz == 0;
    }

    private static boolean isOuterCorner(int dx, int dz) {
        return (dx == -1 || dx == 2) && (dz == -1 || dz == 2);
    }

    private static boolean isCavityFloor(int dx, int dy, int dz) {
        return dy == 0 && dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
    }

    private static boolean isPerimeter(int dx, int dz) {
        return dx == -1 || dx == 2 || dz == -1 || dz == 2;
    }

    /** Expected material for a shell cell (not core). */
    public static boolean matchesMaterial(BlockState state, int dx, int dy, int dz) {
        if (isOuterCorner(dx, dz)) {
            return state.is(ModBlocks.PURIFIED_OBSIDIAN.get());
        }
        if (isCavityFloor(dx, dy, dz)) {
            return state.is(ModBlocks.PHI_CONCRETE.get());
        }
        if (isPerimeter(dx, dz) && dy >= 1) {
            return state.is(ModBlocks.PHI_GLASS.get());
        }
        return state.is(ModBlocks.REACTOR_CASING.get());
    }

    public static boolean isMaterialShell(LevelReader level, BlockPos core) {
        if (!level.getBlockState(core).is(ModBlocks.REGEN_CHAMBER.get())) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    if (isCoreOffset(dx, dy, dz)) {
                        continue;
                    }
                    if (isAirCavity(dx, dy, dz)) {
                        cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                        if (!level.getBlockState(cursor).isAir()) {
                            return false;
                        }
                        continue;
                    }
                    if (!isShellCell(dx, dy, dz)) {
                        continue;
                    }
                    cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                    if (!matchesMaterial(level.getBlockState(cursor), dx, dy, dz)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isAssembled(LevelReader level, BlockPos core) {
        if (!level.getBlockState(core).is(ModBlocks.REGEN_CHAMBER.get())) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    if (isCoreOffset(dx, dy, dz)) {
                        continue;
                    }
                    if (isAirCavity(dx, dy, dz)) {
                        continue;
                    }
                    if (!isShellCell(dx, dy, dz)) {
                        continue;
                    }
                    cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (!(be instanceof RegenChamberPartBlockEntity part) || !part.isOwnedBy(core)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void assemble(ServerLevel level, BlockPos core) {
        if (!(level.getBlockEntity(core) instanceof RegenChamberBlockEntity chamber) || chamber.isDismantling()) {
            return;
        }
        if (!isMaterialShell(level, core)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    if (isCoreOffset(dx, dy, dz) || isAirCavity(dx, dy, dz) || !isShellCell(dx, dy, dz)) {
                        continue;
                    }
                    cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                    BlockState original = level.getBlockState(cursor);
                    CompoundTag saved = NbtUtils.writeBlockState(original);
                    level.setBlock(cursor, ModBlocks.REGEN_CHAMBER_PART.get().defaultBlockState(), Block.UPDATE_ALL);
                    if (level.getBlockEntity(cursor) instanceof RegenChamberPartBlockEntity part) {
                        part.setController(core, saved);
                    }
                }
            }
        }
        BlockState coreState = level.getBlockState(core);
        if (coreState.is(ModBlocks.REGEN_CHAMBER.get()) && !coreState.getValue(RegenChamberBlock.FORMED)) {
            level.setBlock(core, coreState.setValue(RegenChamberBlock.FORMED, true), Block.UPDATE_CLIENTS);
        }
        chamber.setFormed(true);
        chamber.setChanged();
        level.sendBlockUpdated(core, coreState, level.getBlockState(core), Block.UPDATE_CLIENTS);
    }

    public static void disassemble(ServerLevel level, BlockPos core) {
        disassemble(level, core, null);
    }

    public static void disassemble(ServerLevel level, BlockPos core, @Nullable BlockPos brokenPart) {
        if (!(level.getBlockEntity(core) instanceof RegenChamberBlockEntity chamber)) {
            return;
        }
        if (chamber.isDismantling()) {
            return;
        }
        chamber.setDismantling(true);
        try {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dy = 0; dy <= 2; dy++) {
                for (int dx = -1; dx <= 2; dx++) {
                    for (int dz = -1; dz <= 2; dz++) {
                        if (isCoreOffset(dx, dy, dz) || isAirCavity(dx, dy, dz) || !isShellCell(dx, dy, dz)) {
                            continue;
                        }
                        cursor.set(core.getX() + dx, core.getY() + dy, core.getZ() + dz);
                        if (brokenPart != null && brokenPart.equals(cursor)) {
                            continue;
                        }
                        BlockEntity be = level.getBlockEntity(cursor);
                        if (be instanceof RegenChamberPartBlockEntity part && part.isOwnedBy(core)) {
                            BlockState restored = part.readOriginal(level.registryAccess());
                            level.setBlock(cursor, restored, Block.UPDATE_ALL);
                        }
                    }
                }
            }
            BlockState coreState = level.getBlockState(core);
            if (coreState.is(ModBlocks.REGEN_CHAMBER.get()) && coreState.getValue(RegenChamberBlock.FORMED)) {
                level.setBlock(core, coreState.setValue(RegenChamberBlock.FORMED, false), Block.UPDATE_CLIENTS);
            }
            chamber.setFormed(false);
            chamber.onDisassembled();
            chamber.setChanged();
        } finally {
            chamber.setDismantling(false);
        }
    }

    public static void onPartBroken(ServerLevel level, BlockPos partPos, RegenChamberPartBlockEntity part) {
        BlockPos controller = part.getControllerPos();
        if (controller == null) {
            return;
        }
        if (!(level.getBlockEntity(controller) instanceof RegenChamberBlockEntity chamber) || chamber.isDismantling()) {
            return;
        }
        BlockState original = part.readOriginal(level.registryAccess());
        if (!original.isAir()) {
            Block.popResource(level, partPos, new ItemStack(original.getBlock()));
        }
        disassemble(level, controller, partPos);
    }

    /** Interior volume where standing players receive healing (above floor). */
    public static AABB interiorAabb(BlockPos core) {
        return new AABB(
                core.getX(),
                core.getY() + 1.0,
                core.getZ(),
                core.getX() + 2.0,
                core.getY() + 3.0,
                core.getZ() + 2.0);
    }

    public static void forEachShellOffset(ShellConsumer consumer) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 2; dz++) {
                    if (isCoreOffset(dx, dy, dz) || isAirCavity(dx, dy, dz) || !isShellCell(dx, dy, dz)) {
                        continue;
                    }
                    consumer.accept(dx, dy, dz);
                }
            }
        }
    }

    @FunctionalInterface
    public interface ShellConsumer {
        void accept(int dx, int dy, int dz);
    }
}
