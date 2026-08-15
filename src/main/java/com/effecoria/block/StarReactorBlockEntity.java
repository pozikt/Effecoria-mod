package com.effecoria.block;

import com.effecoria.alchemy.menu.StarReactorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.alchemy.PhiPowerHubs;
import com.effecoria.core.alchemy.PhiPowerProvider;
import com.effecoria.core.alchemy.StarMultiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

/**
 * Era VI Star Reactor — 5×5×5 ambient Φ hub (radius 40, factor 3.0) with Forge-style fuels and Ω.
 */
public final class StarReactorBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PhiPowerProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_COUNT = 1;

    public static final int DATA_FORMED = 0;
    public static final int DATA_RUNNING = 1;
    public static final int DATA_FUEL_TICKS = 2;
    public static final int DATA_OMEGA_CENTI = 3;
    public static final int DATA_COOLED = 4;
    public static final int DATA_FACTOR_CENTI = 5;
    public static final int DATA_COUNT = 6;

    public static final int POWER_RADIUS = 40;
    public static final float POWER_FACTOR = 3.0f;
    public static final int FUEL_STAR = 24000;
    public static final int FUEL_PURE = 12000;
    public static final int FUEL_CELL = 6000;
    public static final int OMEGA_CREEP_PER_TICK = 3;
    public static final int OMEGA_PAUSE_THRESHOLD = 2500;
    public static final int OMEGA_SCRAM_THRESHOLD = 5000;

    private static final int[] FUEL_SLOTS = {SLOT_FUEL};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean formed;
    private boolean running;
    private int fuelTicks;
    private int omegaCentis;
    private boolean dismantling;
    private boolean hubRegistered;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_RUNNING -> running ? 1 : 0;
                case DATA_FUEL_TICKS -> fuelTicks;
                case DATA_OMEGA_CENTI -> omegaCentis;
                case DATA_COOLED -> hasCoolant() ? 1 : 0;
                case DATA_FACTOR_CENTI -> Math.round(powerFactor() * 100f);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FORMED -> formed = value != 0;
                case DATA_RUNNING -> running = value != 0;
                case DATA_FUEL_TICKS -> fuelTicks = value;
                case DATA_OMEGA_CENTI -> omegaCentis = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public StarReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAR_REACTOR_CORE.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean value) {
        formed = value;
    }

    public boolean isDismantling() {
        return dismantling;
    }

    public void setDismantling(boolean value) {
        dismantling = value;
    }

    public void onDisassembled() {
        running = false;
        unregisterHub();
    }

    public boolean isRunning() {
        return running;
    }

    public int fuelTicks() {
        return fuelTicks;
    }

    public int omegaCentis() {
        return omegaCentis;
    }

    public boolean tryStart() {
        if (!formed) {
            return false;
        }
        if (fuelTicks <= 0 && !tryConsumeFuel()) {
            return false;
        }
        running = true;
        setChanged();
        return true;
    }

    public void stop() {
        running = false;
        unregisterHub();
        setChanged();
    }

    public void toggleRunning() {
        if (running) {
            stop();
        } else {
            tryStart();
        }
    }

    public boolean clearOmegaMeter() {
        if (omegaCentis <= 0) {
            return false;
        }
        omegaCentis = 0;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.35f);
        }
        return true;
    }

    public static boolean isValidFuel(ItemStack stack) {
        if (stack.is(ModItems.STAR_ESSONITE.get()) || stack.is(ModItems.PURE_ESSONITE.get())) {
            return true;
        }
        return stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) >= 0.85f;
    }

    private boolean tryConsumeFuel() {
        ItemStack stack = items.get(SLOT_FUEL);
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.STAR_ESSONITE.get())) {
            stack.shrink(1);
            fuelTicks += FUEL_STAR;
            setChanged();
            return true;
        }
        if (stack.is(ModItems.PURE_ESSONITE.get())) {
            stack.shrink(1);
            fuelTicks += FUEL_PURE;
            setChanged();
            return true;
        }
        if (stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) >= 0.85f) {
            PhiHarnessItems.setCellCharge(stack, 0f);
            fuelTicks += FUEL_CELL;
            setChanged();
            return true;
        }
        return false;
    }

    private boolean isActivelyPowering() {
        return formed
                && running
                && fuelTicks > 0
                && hasCoolant()
                && omegaCentis < OMEGA_PAUSE_THRESHOLD;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, StarReactorBlockEntity be) {}

    @Override
    public boolean supplying() {
        return isActivelyPowering();
    }

    @Override
    public int radius() {
        return POWER_RADIUS;
    }

    @Override
    public float powerFactor() {
        return isActivelyPowering() ? POWER_FACTOR : 0f;
    }

    @Override
    public boolean drainFuel(int ticks) {
        if (!isActivelyPowering() || ticks <= 0 || fuelTicks < ticks) {
            return false;
        }
        fuelTicks -= ticks;
        setChanged();
        return true;
    }

    private boolean hasCoolant() {
        if (level == null) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int cheb = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (cheb != 3) {
                        continue;
                    }
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    FluidState fluid = level.getFluidState(cursor);
                    if (state.is(Blocks.ICE)
                            || state.is(Blocks.PACKED_ICE)
                            || state.is(Blocks.BLUE_ICE)
                            || state.is(Blocks.WATER)
                            || state.is(BlockTags.ICE)
                            || fluid.is(FluidTags.WATER)
                            || state.is(ModBlocks.PHI_WATER.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void registerHub() {
        if (level != null && !hubRegistered && isActivelyPowering()) {
            PhiPowerHubs.setActive(level, worldPosition, true);
            hubRegistered = true;
        }
    }

    private void unregisterHub() {
        if (level != null && hubRegistered) {
            PhiPowerHubs.setActive(level, worldPosition, false);
            hubRegistered = false;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StarReactorBlockEntity be) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        boolean changed = false;

        if (!be.formed) {
            if (StarMultiblock.isMaterialShell(level, pos)) {
                StarMultiblock.assemble((ServerLevel) level, pos);
                changed = true;
            }
        } else if (!StarMultiblock.isAssembled(level, pos)) {
            StarMultiblock.disassemble((ServerLevel) level, pos);
            changed = true;
        }

        if (be.running && be.formed) {
            if (be.fuelTicks <= 0 && !be.tryConsumeFuel()) {
                be.stop();
                changed = true;
            } else if (be.fuelTicks > 0) {
                be.fuelTicks--;
                changed = true;
                boolean cooled = be.hasCoolant();
                if (!cooled) {
                    be.omegaCentis = Math.min(10000, be.omegaCentis + OMEGA_CREEP_PER_TICK);
                } else {
                    be.omegaCentis = Math.min(10000, be.omegaCentis + 1);
                }
                if (be.omegaCentis >= OMEGA_SCRAM_THRESHOLD) {
                    be.stop();
                    changed = true;
                }
            }
        }

        if (be.isActivelyPowering()) {
            be.registerHub();
        } else {
            be.unregisterHub();
        }

        boolean shouldLit = be.isActivelyPowering();
        BlockState current = level.getBlockState(pos);
        if (current.is(ModBlocks.STAR_REACTOR_CORE.get())) {
            boolean needUpdate = false;
            BlockState next = current;
            if (current.getValue(StarReactorBlock.LIT) != shouldLit) {
                next = next.setValue(StarReactorBlock.LIT, shouldLit);
                needUpdate = true;
            }
            if (current.getValue(StarReactorBlock.FORMED) != be.formed) {
                next = next.setValue(StarReactorBlock.FORMED, be.formed);
                needUpdate = true;
            }
            if (needUpdate) {
                level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            }
        }
        if (changed) {
            be.setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.star_reactor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new StarReactorMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? NO_SLOTS : FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == SLOT_FUEL && isValidFuel(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index == SLOT_FUEL && isValidFuel(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Running", running);
        tag.putInt("Fuel", fuelTicks);
        tag.putInt("Omega", omegaCentis);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        formed = tag.getBoolean("Formed");
        running = tag.getBoolean("Running");
        fuelTicks = tag.getInt("Fuel");
        omegaCentis = tag.getInt("Omega");
    }

    @Override
    public void setRemoved() {
        unregisterHub();
        super.setRemoved();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
