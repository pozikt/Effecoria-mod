package com.effecoria.block;

import com.effecoria.alchemy.menu.HeartReactorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeartMultiblock;
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
 * Era IV Heart Reactor («Сердце») — 3×3×3 multiblock ambient Φ-power source.
 */
public final class HeartReactorBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PhiPowerProvider {
    public static final int SLOT_CATALYST = 0;
    public static final int SLOT_COUNT = 1;

    public static final int DATA_FORMED = 0;
    public static final int DATA_RUNNING = 1;
    public static final int DATA_PRIMED = 2;
    public static final int DATA_OVERHEAT = 3;
    public static final int DATA_BOOST = 4;
    public static final int DATA_FACTOR_CENTI = 5;
    public static final int DATA_COOLED = 6;
    public static final int DATA_COUNT = 7;

    public static final int POWER_RADIUS = 8;
    public static final int OVERHEAT_RUN_THRESHOLD = 12000;
    public static final int BOOST_TICKS = 1200;
    public static final int OVERHEAT_COOLDOWN = 2400;
    public static final int FLUX_PRIMING_COST = 4;

    private static final int[] CATALYST_SLOTS = {SLOT_CATALYST};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean formed;
    private boolean running;
    private boolean primed;
    private int continuousRunTicks;
    private int overheatCooldown;
    private int boostTicks;
    private boolean thermalOverheat;
    private boolean dismantling;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_RUNNING -> running ? 1 : 0;
                case DATA_PRIMED -> primed ? 1 : 0;
                case DATA_OVERHEAT -> overheatCooldown;
                case DATA_BOOST -> boostTicks;
                case DATA_FACTOR_CENTI -> Math.round(powerFactor() * 100f);
                case DATA_COOLED -> hasCoolant() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FORMED -> formed = value != 0;
                case DATA_RUNNING -> running = value != 0;
                case DATA_PRIMED -> primed = value != 0;
                case DATA_OVERHEAT -> overheatCooldown = value;
                case DATA_BOOST -> boostTicks = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public HeartReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEART_REACTOR_CORE.get(), pos, state);
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

    /** Called after shell parts are restored. */
    public void onDisassembled() {
        running = false;
        primed = false;
        continuousRunTicks = 0;
        thermalOverheat = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPrimed() {
        return primed;
    }

    public int overheatCooldown() {
        return overheatCooldown;
    }

    public int boostTicks() {
        return boostTicks;
    }

    public boolean tryStart() {
        if (!formed || overheatCooldown > 0) {
            return false;
        }
        if (!primed && !tryPrime()) {
            return false;
        }
        running = true;
        setChanged();
        return true;
    }

    public void stop() {
        running = false;
        setChanged();
    }

    public void toggleRunning() {
        if (running) {
            stop();
        } else {
            tryStart();
        }
    }

    private boolean tryPrime() {
        ItemStack stack = items.get(SLOT_CATALYST);
        if (stack.is(ModItems.PURE_ESSONITE.get())) {
            stack.shrink(1);
            primed = true;
            boostTicks = BOOST_TICKS;
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.55f, 1.2f);
            }
            setChanged();
            return true;
        }
        if (stack.is(ModItems.PHI_FLUX_SLUG.get()) && stack.getCount() >= FLUX_PRIMING_COST) {
            stack.shrink(FLUX_PRIMING_COST);
            primed = true;
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.45f, 1.4f);
            }
            setChanged();
            return true;
        }
        return false;
    }

    public static boolean isValidCatalyst(ItemStack stack) {
        return stack.is(ModItems.PURE_ESSONITE.get()) || stack.is(ModItems.PHI_FLUX_SLUG.get());
    }

    private boolean isActivelyPowering() {
        return formed && running && primed && overheatCooldown <= 0;
    }

    /** Client: keep the deep Heart hum while the formed hull is lit. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HeartReactorBlockEntity be) {
        if (state.getValue(HeartReactorBlock.LIT)) {
            com.effecoria.client.sound.ReactorHumClient.ensureHeart(pos);
        }
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
            return 2f;
        }
        if (thermalOverheat) {
            return 0.5f;
        }
        return 1f;
    }

    /**
     * Heart has no discrete fuel meter — load drains boost first, then accelerates thermal run
     * toward overheat (still succeeds while actively powering).
     */
    @Override
    public boolean drainFuel(int ticks) {
        if (!isActivelyPowering() || ticks <= 0) {
            return false;
        }
        int fromBoost = Math.min(boostTicks, ticks);
        boostTicks -= fromBoost;
        int remainder = ticks - fromBoost;
        if (remainder > 0) {
            continuousRunTicks += remainder * 4;
        }
        setChanged();
        return true;
    }

    private boolean hasCoolant() {
        if (level == null) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Check shell exterior neighbors (one block outside the 3×3×3)
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, HeartReactorBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean changed = false;

        if (!be.formed) {
            if (HeartMultiblock.isMaterialShell(level, pos)) {
                HeartMultiblock.assemble(server, pos);
                changed = true;
            }
        } else if (!HeartMultiblock.isAssembled(level, pos)) {
            HeartMultiblock.disassemble(server, pos);
            changed = true;
        }

        if (be.overheatCooldown > 0) {
            be.overheatCooldown--;
            be.running = false;
            changed = true;
            if (be.overheatCooldown == 0) {
                be.thermalOverheat = false;
            }
        }

        if (be.running && be.formed && be.primed && be.overheatCooldown <= 0) {
            be.continuousRunTicks++;
            changed = true;
            if (be.boostTicks > 0) {
                be.boostTicks--;
            }
            boolean cooled = be.hasCoolant();
            if (!cooled && be.continuousRunTicks >= OVERHEAT_RUN_THRESHOLD) {
                be.thermalOverheat = true;
                if (be.continuousRunTicks >= OVERHEAT_RUN_THRESHOLD + 600) {
                    be.running = false;
                    be.overheatCooldown = OVERHEAT_COOLDOWN;
                    be.continuousRunTicks = 0;
                    be.primed = false;
                    server.levelEvent(1501, pos, 0);
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 0.7f);
                }
            } else if (cooled && be.continuousRunTicks > 0 && server.getGameTime() % 5L == 0L) {
                be.continuousRunTicks = Math.max(0, be.continuousRunTicks - 3);
                if (be.thermalOverheat && be.continuousRunTicks < OVERHEAT_RUN_THRESHOLD / 2) {
                    be.thermalOverheat = false;
                }
            }
        } else if (!be.running && be.continuousRunTicks > 0) {
            be.continuousRunTicks = Math.max(0, be.continuousRunTicks - 2);
            changed = true;
        }

        boolean shouldLit = be.isActivelyPowering();
        BlockState current = level.getBlockState(pos);
        if (current.is(ModBlocks.HEART_REACTOR_CORE.get())) {
            boolean needUpdate = false;
            BlockState next = current;
            if (current.getValue(HeartReactorBlock.LIT) != shouldLit) {
                next = next.setValue(HeartReactorBlock.LIT, shouldLit);
                needUpdate = true;
            }
            if (current.getValue(HeartReactorBlock.FORMED) != be.formed) {
                next = next.setValue(HeartReactorBlock.FORMED, be.formed);
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
        return Component.translatable("container.effecoria.heart_reactor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new HeartReactorMenu(id, inv, this, data);
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
        return side == Direction.DOWN ? NO_SLOTS : CATALYST_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == SLOT_CATALYST && isValidCatalyst(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index == SLOT_CATALYST && isValidCatalyst(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Running", running);
        tag.putBoolean("Primed", primed);
        tag.putInt("RunTicks", continuousRunTicks);
        tag.putInt("Overheat", overheatCooldown);
        tag.putInt("Boost", boostTicks);
        tag.putBoolean("Thermal", thermalOverheat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        formed = tag.getBoolean("Formed");
        running = tag.getBoolean("Running");
        primed = tag.getBoolean("Primed");
        continuousRunTicks = tag.getInt("RunTicks");
        overheatCooldown = tag.getInt("Overheat");
        boostTicks = tag.getInt("Boost");
        thermalOverheat = tag.getBoolean("Thermal");
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
