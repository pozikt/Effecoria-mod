package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiCrusherMenu;
import com.effecoria.alchemy.recipe.CrusherRecipes;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Φ-crusher base — grind + PhiPower / cell drive. */
public final class PhiCrusherBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_PRIMARY = 1;
    public static final int SLOT_BYPRODUCT = 2;
    public static final int SLOT_WASTE = 3;
    public static final int SLOT_DRIVE = 4;
    public static final int SLOT_COUNT = 5;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_MODE = 2;
    public static final int DATA_POWER = 3;
    public static final int DATA_HEAT = 4;
    public static final int DATA_OMEGA = 5;
    public static final int DATA_FORMED = 6;
    public static final int DATA_COOLDOWN = 7;
    public static final int DATA_COUNT = 8;

    public static final int MAX_HEAT = 100;
    public static final int OMEGA_LIMIT = 20;
    public static final float CELL_DRAIN = 0.02f;

    private static final int[] INPUT_SLOTS = {SLOT_INPUT};
    private static final int[] OUTPUT_SLOTS = {SLOT_PRIMARY, SLOT_BYPRODUCT, SLOT_WASTE};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private CrusherRecipes.Mode mode = CrusherRecipes.Mode.COARSE;
    private int heatCycles;
    private int omegaMeter;
    private int coolCooldown;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> mode.ticks;
                case DATA_MODE -> mode.ordinal();
                case DATA_POWER -> powerCenti();
                case DATA_HEAT -> heatCycles;
                case DATA_OMEGA -> omegaMeter;
                case DATA_FORMED -> isFormed() ? 1 : 0;
                case DATA_COOLDOWN -> coolCooldown;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MODE -> mode = value == 1 ? CrusherRecipes.Mode.FINE : CrusherRecipes.Mode.COARSE;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_CRUSHER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public CrusherRecipes.Mode mode() {
        return mode;
    }

    public void toggleMode() {
        mode = mode == CrusherRecipes.Mode.COARSE ? CrusherRecipes.Mode.FINE : CrusherRecipes.Mode.COARSE;
        progress = 0;
        setChanged();
    }

    public boolean isFormed() {
        return level != null && PhiCrusherBlock.isFormed(level, worldPosition);
    }

    public boolean cleanOmega(Player player) {
        if (omegaMeter <= 0) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.LEAD_FOIL.get())) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        omegaMeter = 0;
        ItemStack waste = new ItemStack(ModItems.OMEGA_WASTE.get());
        if (!player.getInventory().add(waste)) {
            player.drop(waste, false);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5f, 0.7f);
        }
        return true;
    }

    /** Clears Ω without consuming foil / dropping waste (used by omega_filter). */
    public boolean clearOmegaMeter() {
        if (omegaMeter <= 0) {
            return false;
        }
        omegaMeter = 0;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.3f);
        }
        return true;
    }

    private int powerCenti() {
        if (level == null) {
            return 0;
        }
        float factor = PhiPower.powerFactor(level, worldPosition);
        if (factor > 0.01f) {
            return Math.round(factor * 100f);
        }
        float charge = PhiHarnessItems.cellCharge(items.get(SLOT_DRIVE));
        return Math.round(charge * 100f);
    }

    private boolean tryConsumePower() {
        if (level == null) {
            return false;
        }
        if (PhiPower.consumeTick(level, worldPosition, mode.powerLoad)) {
            return true;
        }
        ItemStack cell = items.get(SLOT_DRIVE);
        float charge = PhiHarnessItems.cellCharge(cell);
        if (charge > 0.001f) {
            PhiHarnessItems.setCellCharge(cell, charge - CELL_DRAIN * mode.powerLoad);
            setChanged();
            return true;
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiCrusherBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean formed = PhiCrusherBlock.isFormed(level, pos);
        boolean changed = false;

        if (be.coolCooldown > 0) {
            be.coolCooldown--;
            changed = true;
        }

        boolean working = false;
        if (formed && be.coolCooldown <= 0 && be.omegaMeter < OMEGA_LIMIT) {
            ItemStack input = be.items.get(SLOT_INPUT);
            var recipe = CrusherRecipes.crush(input, be.mode, server.getRandom());
            if (recipe.isPresent() && be.canOutput(recipe.get())) {
                if (be.tryConsumePower()) {
                    working = true;
                    be.progress++;
                    if (be.progress >= be.mode.ticks) {
                        be.finishCrush(server, recipe.get());
                        be.progress = 0;
                    }
                    changed = true;
                } else if (be.progress > 0) {
                    be.progress = Math.max(0, be.progress - 1);
                    changed = true;
                }
            } else if (be.progress > 0) {
                be.progress = 0;
                changed = true;
            }
        } else if (be.progress > 0) {
            be.progress = 0;
            changed = true;
        }

        boolean litWanted = working;
        if (state.getValue(PhiCrusherBlock.LIT) != litWanted) {
            level.setBlock(pos, state.setValue(PhiCrusherBlock.LIT, litWanted), Block.UPDATE_CLIENTS);
            BlockPos above = pos.above();
            BlockState hopper = level.getBlockState(above);
            if (hopper.is(com.effecoria.content.ModBlocks.PHI_CRUSHER_HOPPER.get())
                    && hopper.hasProperty(PhiCrusherHopperBlock.LIT)) {
                level.setBlock(above, hopper.setValue(PhiCrusherHopperBlock.LIT, litWanted), Block.UPDATE_CLIENTS);
            }
        }

        if (working && server.getGameTime() % 20L == 0L) {
            server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35f, 0.55f);
        }

        if (changed) {
            be.setChanged();
        }
    }

    private boolean canOutput(CrusherRecipes.Result result) {
        return canMerge(SLOT_PRIMARY, result.primary())
                && canMerge(SLOT_BYPRODUCT, result.byproduct())
                && canMerge(SLOT_WASTE, result.waste());
    }

    private boolean canMerge(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack cur = items.get(slot);
        if (cur.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() + stack.getCount() <= cur.getMaxStackSize();
    }

    private void finishCrush(ServerLevel server, CrusherRecipes.Result result) {
        items.get(SLOT_INPUT).shrink(1);
        merge(SLOT_PRIMARY, result.primary());
        merge(SLOT_BYPRODUCT, result.byproduct());
        merge(SLOT_WASTE, result.waste());
        heatCycles++;
        if (result.omegaWork()) {
            omegaMeter++;
        }
        if (heatCycles >= MAX_HEAT) {
            int over = heatCycles - MAX_HEAT + 50;
            coolCooldown = Math.max(coolCooldown, 20 * 60 * Math.max(1, over / 50));
            heatCycles = MAX_HEAT / 2;
            server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 0.8f);
        }
        setChanged();
    }

    private void merge(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack cur = items.get(slot);
        if (cur.isEmpty()) {
            items.set(slot, stack.copy());
        } else {
            cur.grow(stack.getCount());
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.phi_crusher");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new PhiCrusherMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return INPUT_SLOTS;
        }
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == SLOT_INPUT && CrusherRecipes.isInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_PRIMARY || index == SLOT_BYPRODUCT || index == SLOT_WASTE;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == SLOT_DRIVE) {
            return stack.is(ModItems.PHI_CELL.get());
        }
        if (index == SLOT_INPUT) {
            return CrusherRecipes.isInput(stack);
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Progress", progress);
        tag.putString("Mode", mode.name());
        tag.putInt("Heat", heatCycles);
        tag.putInt("Omega", omegaMeter);
        tag.putInt("Cool", coolCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        progress = tag.getInt("Progress");
        try {
            mode = CrusherRecipes.Mode.valueOf(tag.getString("Mode"));
        } catch (Exception ignored) {
            mode = CrusherRecipes.Mode.COARSE;
        }
        heatCycles = tag.getInt("Heat");
        omegaMeter = tag.getInt("Omega");
        coolCooldown = tag.getInt("Cool");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    public static boolean openGui(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        if (!(level.getBlockEntity(pos) instanceof PhiCrusherBlockEntity crusher)) {
            return false;
        }
        if (!TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.III)) {
            return false;
        }
        if (!crusher.isFormed()) {
            serverPlayer.displayClientMessage(Component.translatable("gui.effecoria.phi_crusher.need_hopper"), true);
            return false;
        }
        serverPlayer.openMenu(crusher, buf -> buf.writeBlockPos(pos));
        return true;
    }
}
