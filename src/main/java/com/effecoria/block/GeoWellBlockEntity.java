package com.effecoria.block;

import com.effecoria.alchemy.menu.GeoWellMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.alchemy.GeoWellMultiblock;
import com.effecoria.core.alchemy.PhiPowerHubs;
import com.effecoria.core.alchemy.PhiPowerProvider;

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
 * Era V Geo Well — 3×3×3 multiblock ambient Φ-power source with mild Ω creep and hybrid catalyst drip.
 */
public final class GeoWellBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PhiPowerProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    public static final int DATA_FORMED = 0;
    public static final int DATA_RUNNING = 1;
    public static final int DATA_FUEL_TICKS = 2;
    public static final int DATA_OMEGA_CENTI = 3;
    public static final int DATA_COOLED = 4;
    public static final int DATA_FACTOR_CENTI = 5;
    public static final int DATA_COUNT = 6;

    public static final int POWER_RADIUS = 12;
    public static final int FUEL_PER_UNIT = 600;
    public static final int HYBRID_INTERVAL = 200;
    public static final float HYBRID_CHANCE = 0.15f;
    public static final int OMEGA_CREEP_PER_TICK = 2;
    public static final int OMEGA_PAUSE_THRESHOLD = 2500; // 25% — pauses Φ supply until filtered

    private static final int[] FUEL_SLOTS = {SLOT_FUEL};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT};
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

    public GeoWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEO_WELL_CORE.get(), pos, state);
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
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.2f);
        }
        return true;
    }

    public static boolean isValidFuel(ItemStack stack) {
        if (stack.is(ModItems.PHI_FLUX_SLUG.get())) {
            return true;
        }
        return stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) > 0.001f;
    }

    private boolean tryConsumeFuel() {
        ItemStack stack = items.get(SLOT_FUEL);
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(ModItems.PHI_FLUX_SLUG.get())) {
            stack.shrink(1);
            fuelTicks += FUEL_PER_UNIT;
            setChanged();
            return true;
        }
        if (stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) > 0.001f) {
            PhiHarnessItems.setCellCharge(stack, 0f);
            fuelTicks += FUEL_PER_UNIT;
            setChanged();
            return true;
        }
        return false;
    }

    private boolean isActivelyPowering() {
        return formed && running && fuelTicks > 0 && hasCoolant() && omegaCentis < OMEGA_PAUSE_THRESHOLD;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GeoWellBlockEntity be) {
        // optional: no dedicated hum
    }

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
        return isActivelyPowering() ? 2.0f : 0f;
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
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int cheb = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (cheb != 2) {
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

    private void maybeProduceHybrid(ServerLevel server) {
        if (server.getGameTime() % HYBRID_INTERVAL != 0L) {
            return;
        }
        if (server.random.nextFloat() >= HYBRID_CHANCE) {
            return;
        }
        ItemStack out = items.get(SLOT_OUTPUT);
        ItemStack catalyst = new ItemStack(ModItems.DEEP_PHI_CATALYST.get());
        if (out.isEmpty()) {
            items.set(SLOT_OUTPUT, catalyst);
            setChanged();
        } else if (ItemStack.isSameItemSameComponents(out, catalyst) && out.getCount() < out.getMaxStackSize()) {
            out.grow(1);
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeoWellBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean changed = false;

        if (!be.formed) {
            if (GeoWellMultiblock.isMaterialShell(level, pos)) {
                GeoWellMultiblock.assemble(server, pos);
                changed = true;
            }
        } else if (!GeoWellMultiblock.isAssembled(level, pos)) {
            GeoWellMultiblock.disassemble(server, pos);
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
                }
                if (be.isActivelyPowering()) {
                    be.maybeProduceHybrid(server);
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
        if (current.is(ModBlocks.GEO_WELL_CORE.get())) {
            boolean needUpdate = false;
            BlockState next = current;
            if (current.getValue(GeoWellBlock.LIT) != shouldLit) {
                next = next.setValue(GeoWellBlock.LIT, shouldLit);
                needUpdate = true;
            }
            if (current.getValue(GeoWellBlock.FORMED) != be.formed) {
                next = next.setValue(GeoWellBlock.FORMED, be.formed);
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
        return Component.translatable("container.effecoria.geo_well");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new GeoWellMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.items.set(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        if (side == Direction.UP) {
            return FUEL_SLOTS;
        }
        return FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == SLOT_FUEL && isValidFuel(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUTPUT;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == SLOT_FUEL) {
            return isValidFuel(stack);
        }
        return false;
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
