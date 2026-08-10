package com.effecoria.block;

import com.effecoria.alchemy.menu.SparkReactorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
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

import javax.annotation.Nullable;

/**
 * Era IV Spark Reactor («Искра») — personal Φ-power source with fuel, overheat, boost, and cell charge.
 */
public final class SparkReactorBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PhiPowerProvider {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_CHARGE = 1;
    public static final int SLOT_COUNT = 2;

    public static final int DATA_FUEL = 0;
    public static final int DATA_FUEL_MAX = 1;
    public static final int DATA_RUNNING = 2;
    public static final int DATA_OVERHEAT = 3;
    public static final int DATA_BOOST = 4;
    public static final int DATA_FACTOR_CENTI = 5;
    public static final int DATA_CHARGE_PROGRESS = 6;
    public static final int DATA_COUNT = 7;

    public static final int DUST_FUEL_TICKS = 1200;
    public static final int FLUX_FUEL_TICKS = 4800;
    public static final int CELL_FUEL_TICKS = 12000;
    public static final int BOOST_TICKS = 600;
    public static final int BOOST_SHUTDOWN_TICKS = 2400;
    public static final int OVERHEAT_RUN_THRESHOLD = 6000;
    public static final int OVERHEAT_COOLDOWN_PER_CYCLE = 1200;
    public static final int CHARGE_TICKS = 1200;
    public static final int POWER_RADIUS = 3;

    private static final int[] FUEL_SLOTS = {SLOT_FUEL};
    private static final int[] CHARGE_SLOTS = {SLOT_CHARGE};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int fuelTicks;
    private int fuelMax;
    private boolean running;
    private int continuousRunTicks;
    private int overheatCooldown;
    private int boostTicks;
    private int chargeProgress;
    private boolean thermalOverheat;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> fuelTicks;
                case DATA_FUEL_MAX -> Math.max(1, fuelMax);
                case DATA_RUNNING -> running && isActivelyPowering() ? 1 : (running ? 1 : 0);
                case DATA_OVERHEAT -> overheatCooldown;
                case DATA_BOOST -> boostTicks;
                case DATA_FACTOR_CENTI -> Math.round(powerFactor() * 100f);
                case DATA_CHARGE_PROGRESS -> chargeProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuelTicks = value;
                case DATA_FUEL_MAX -> fuelMax = value;
                case DATA_RUNNING -> running = value != 0;
                case DATA_OVERHEAT -> overheatCooldown = value;
                case DATA_BOOST -> boostTicks = value;
                case DATA_CHARGE_PROGRESS -> chargeProgress = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SparkReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPARK_REACTOR.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public int fuelTicks() {
        return fuelTicks;
    }

    public int fuelMax() {
        return Math.max(1, fuelMax);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean value) {
        running = value;
        setChanged();
    }

    public void toggleRunning() {
        setRunning(!running);
    }

    public int overheatCooldown() {
        return overheatCooldown;
    }

    public int boostTicks() {
        return boostTicks;
    }

    public int chargeProgress() {
        return chargeProgress;
    }

    private boolean isActivelyPowering() {
        return running && fuelTicks > 0 && overheatCooldown <= 0;
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
        if (!isActivelyPowering()) {
            return 0f;
        }
        if (boostTicks > 0) {
            return 3f;
        }
        if (thermalOverheat) {
            return 0.5f;
        }
        return 1f;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SparkReactorBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean changed = false;

        if (be.overheatCooldown > 0) {
            be.overheatCooldown--;
            be.running = false;
            changed = true;
            if (be.overheatCooldown == 0) {
                be.thermalOverheat = false;
                be.continuousRunTicks = 0;
            }
        }

        if (be.running && be.overheatCooldown <= 0) {
            if (be.fuelTicks <= 0) {
                be.tryConsumeFuel();
            }
            if (be.fuelTicks > 0) {
                be.fuelTicks--;
                be.continuousRunTicks++;
                changed = true;

                if (be.boostTicks > 0) {
                    be.boostTicks--;
                    if (be.boostTicks == 0) {
                        be.triggerBoostShutdown();
                        changed = true;
                    }
                }

                if (be.boostTicks <= 0
                        && !be.thermalOverheat
                        && be.continuousRunTicks >= OVERHEAT_RUN_THRESHOLD) {
                    be.thermalOverheat = true;
                    server.levelEvent(1501, pos, 0);
                }

                be.tickChargeSlot();
                if (server.getGameTime() % 20L == 0L) {
                    be.meltNearby(server);
                }
            } else {
                be.running = false;
                changed = true;
            }
        } else if (!be.running && be.continuousRunTicks > 0 && be.overheatCooldown <= 0) {
            // Cool continuous-run debt while idle (1 min cool per 5 min run)
            int cool = Math.max(1, be.continuousRunTicks / OVERHEAT_RUN_THRESHOLD);
            be.continuousRunTicks = Math.max(0, be.continuousRunTicks - cool);
            if (be.thermalOverheat && be.continuousRunTicks == 0) {
                be.thermalOverheat = false;
            }
            changed = true;
        }

        boolean shouldLit = be.isActivelyPowering();
        if (state.getValue(SparkReactorBlock.LIT) != shouldLit) {
            level.setBlock(pos, state.setValue(SparkReactorBlock.LIT, shouldLit), Block.UPDATE_CLIENTS);
        }
        if (changed) {
            be.setChanged();
        }
    }

    private void tryConsumeFuel() {
        ItemStack fuel = items.get(SLOT_FUEL);
        if (fuel.is(ModItems.PURE_ESSONITE.get())) {
            fuel.shrink(1);
            boostTicks = BOOST_TICKS;
            fuelTicks = Math.max(fuelTicks, BOOST_TICKS);
            fuelMax = Math.max(fuelMax, BOOST_TICKS);
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.45f, 1.6f);
            }
            setChanged();
            return;
        }
        if (fuel.is(ModItems.PHI_FLUX_SLUG.get())) {
            fuel.shrink(1);
            fuelTicks = FLUX_FUEL_TICKS;
            fuelMax = FLUX_FUEL_TICKS;
            setChanged();
            return;
        }
        if (fuel.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(fuel) > 0.001f) {
            PhiHarnessItems.setCellCharge(fuel, 0f);
            fuelTicks = CELL_FUEL_TICKS;
            fuelMax = CELL_FUEL_TICKS;
            setChanged();
        }
    }

    private void triggerBoostShutdown() {
        overheatCooldown = BOOST_SHUTDOWN_TICKS;
        thermalOverheat = true;
        running = false;
        continuousRunTicks = 0;
        if (level != null) {
            level.levelEvent(1501, worldPosition, 0);
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.8f);
        }
    }

    private void tickChargeSlot() {
        ItemStack cell = items.get(SLOT_CHARGE);
        if (!cell.is(ModItems.PHI_CELL.get())) {
            if (chargeProgress != 0) {
                chargeProgress = 0;
            }
            return;
        }
        float charge = PhiHarnessItems.cellCharge(cell);
        if (charge >= 0.999f) {
            chargeProgress = 0;
            return;
        }
        chargeProgress++;
        if (chargeProgress >= CHARGE_TICKS) {
            PhiHarnessItems.setCellCharge(cell, 1f);
            chargeProgress = 0;
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.35f, 1.4f);
            }
        }
    }

    private void meltNearby(ServerLevel level) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.PACKED_ICE)) {
                        level.setBlockAndUpdate(cursor, Blocks.WATER.defaultBlockState());
                    } else if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.SNOW)) {
                        level.removeBlock(cursor, false);
                    }
                }
            }
        }
    }

    public static boolean isValidFuel(ItemStack stack) {
        if (stack.is(ModItems.PHI_FLUX_SLUG.get()) || stack.is(ModItems.PURE_ESSONITE.get())) {
            return true;
        }
        return stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) > 0.001f;
    }

    /** True if the reactor already has burn time or a valid fuel stack to consume. */
    public static boolean hasUsableFuel(SparkReactorBlockEntity be) {
        if (be.fuelTicks > 0) {
            return true;
        }
        return isValidFuel(be.items.get(SLOT_FUEL));
    }

    public static boolean isChargeCell(ItemStack stack) {
        return stack.is(ModItems.PHI_CELL.get());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.spark_reactor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new SparkReactorMenu(id, inv, this, data);
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> isValidFuel(stack);
            case SLOT_CHARGE -> isChargeCell(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return CHARGE_SLOTS;
        }
        if (side == Direction.UP) {
            return FUEL_SLOTS;
        }
        return FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_CHARGE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Fuel", fuelTicks);
        tag.putInt("FuelMax", fuelMax);
        tag.putBoolean("Running", running);
        tag.putInt("RunTicks", continuousRunTicks);
        tag.putInt("Overheat", overheatCooldown);
        tag.putInt("Boost", boostTicks);
        tag.putInt("ChargeProg", chargeProgress);
        tag.putBoolean("ThermalOH", thermalOverheat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        fuelTicks = tag.getInt("Fuel");
        fuelMax = tag.getInt("FuelMax");
        running = tag.getBoolean("Running");
        continuousRunTicks = tag.getInt("RunTicks");
        overheatCooldown = tag.getInt("Overheat");
        boostTicks = tag.getInt("Boost");
        chargeProgress = tag.getInt("ChargeProg");
        thermalOverheat = tag.getBoolean("ThermalOH");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
