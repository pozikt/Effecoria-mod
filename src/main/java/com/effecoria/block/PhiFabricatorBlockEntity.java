package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiFabricatorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.fabricator.FabricatorCatalog;
import com.effecoria.core.fabricator.FabricatorClass;
import com.effecoria.core.fabricator.FabricatorIngredient;
import com.effecoria.core.fabricator.FabricatorRecipeDefinition;
import com.effecoria.core.fabricator.MemoryCrystalData;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import java.util.Optional;

/**
 * Φ-fabricator — rebuilds matter from a scanned memory crystal program.
 * Tier comes from {@link PhiFabricatorBlock#fabricatorClass()}.
 */
public final class PhiFabricatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_PROGRAM = 0;
    public static final int SLOT_SCAN = 1;
    public static final int SLOT_A = 2;
    public static final int SLOT_B = 3;
    public static final int SLOT_C = 4;
    public static final int SLOT_OUT = 5;
    public static final int SLOT_COUNT = 6;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_POWER = 2;
    public static final int DATA_CLASS = 3;
    public static final int DATA_WRITE = 4;
    public static final int DATA_COUNT = 5;

    public static final int WRITE_IDLE = 0;
    public static final int WRITE_NEED = 1;
    public static final int WRITE_FAIL = 2;
    public static final int WRITE_OK = 3;
    public static final int WRITE_NO_POWER = 4;

    private static final int[] INPUT_SLOTS = {SLOT_A, SLOT_B, SLOT_C};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUT};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int writeStatus = WRITE_IDLE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> activeRecipe().map(FabricatorRecipeDefinition::cookTicks).orElse(1);
                case DATA_POWER ->
                        level != null && PhiPower.hasPower(level, worldPosition) ? 1 : 0;
                case DATA_CLASS -> fabricatorClass().level();
                case DATA_WRITE -> writeStatus;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            } else if (index == DATA_WRITE) {
                writeStatus = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_FABRICATOR.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public FabricatorClass fabricatorClass() {
        if (getBlockState().getBlock() instanceof PhiFabricatorBlock block) {
            return block.fabricatorClass();
        }
        return FabricatorClass.I;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiFabricatorBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.tickServer(level, pos, state);
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        Optional<FabricatorRecipeDefinition> recipeOpt = activeRecipe();
        boolean working = false;
        if (recipeOpt.isPresent()) {
            FabricatorRecipeDefinition recipe = recipeOpt.get();
            if (fabricatorClass().supports(recipe.minClass())
                    && canOutput(recipe)
                    && hasIngredients(recipe)
                    && PhiPower.consumeTick(level, pos, recipe.powerPerTick())) {
                progress++;
                working = true;
                if (progress >= recipe.cookTicks()) {
                    finishCraft(level, pos, recipe);
                    progress = 0;
                }
            } else if (progress > 0) {
                progress = Math.max(0, progress - 1);
            }
        } else {
            progress = 0;
        }
        boolean lit = working;
        if (state.getValue(PhiFabricatorBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(PhiFabricatorBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    private Optional<FabricatorRecipeDefinition> activeRecipe() {
        ItemStack program = items.get(SLOT_PROGRAM);
        return MemoryCrystalData.recipeId(program).flatMap(FabricatorCatalog::byId);
    }

    /** Write blank crystal from scan sample. */
    public boolean tryWriteCrystal(ServerPlayer player) {
        ItemStack program = items.get(SLOT_PROGRAM);
        ItemStack sample = items.get(SLOT_SCAN);
        if (!MemoryCrystalData.isBlank(program) || sample.isEmpty()) {
            writeStatus = WRITE_NEED;
            setChanged();
            player.displayClientMessage(Component.translatable("message.effecoria.fabricator.scan_need"), false);
            return false;
        }
        Optional<FabricatorRecipeDefinition> match =
                FabricatorCatalog.findForScan(sample, fabricatorClass().level());
        if (match.isEmpty()) {
            writeStatus = WRITE_FAIL;
            setChanged();
            player.displayClientMessage(Component.translatable("message.effecoria.fabricator.scan_fail"), false);
            return false;
        }
        FabricatorRecipeDefinition recipe = match.get();
        if (!PhiPower.consumeTick(level, worldPosition, Math.max(1, recipe.powerPerTick()))) {
            writeStatus = WRITE_NO_POWER;
            setChanged();
            player.displayClientMessage(Component.translatable("message.effecoria.fabricator.no_power"), false);
            return false;
        }
        MemoryCrystalData.writeRecipe(program, recipe.id());
        items.set(SLOT_PROGRAM, program);
        writeStatus = WRITE_OK;
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.3f);
        }
        player.displayClientMessage(
                Component.translatable("message.effecoria.fabricator.scan_ok", recipe.id().toString()), false);
        return true;
    }

    private boolean canOutput(FabricatorRecipeDefinition recipe) {
        ItemStack out = items.get(SLOT_OUT);
        ItemStack result = FabricatorCatalog.resultStack(recipe);
        if (result.isEmpty()) {
            return false;
        }
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private boolean hasIngredients(FabricatorRecipeDefinition recipe) {
        int[] counts = slotCounts();
        for (FabricatorIngredient need : recipe.ingredients()) {
            int remaining = need.count();
            for (int slot : INPUT_SLOTS) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack stack = items.get(slot);
                if (need.matches(stack)) {
                    int take = Math.min(remaining, counts[slot - SLOT_A]);
                    remaining -= take;
                    counts[slot - SLOT_A] -= take;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private int[] slotCounts() {
        return new int[] {
            items.get(SLOT_A).getCount(), items.get(SLOT_B).getCount(), items.get(SLOT_C).getCount()
        };
    }

    private void finishCraft(Level level, BlockPos pos, FabricatorRecipeDefinition recipe) {
        for (FabricatorIngredient need : recipe.ingredients()) {
            int remaining = need.count();
            for (int slot : INPUT_SLOTS) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack stack = items.get(slot);
                if (!need.matches(stack)) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        ItemStack result = FabricatorCatalog.resultStack(recipe);
        ItemStack out = items.get(SLOT_OUT);
        if (out.isEmpty()) {
            items.set(SLOT_OUT, result);
        } else {
            out.grow(result.getCount());
        }
        if (level instanceof ServerLevel server && recipe.omegaPercent() > 0) {
            TowerFacility.findComputer(server, pos)
                    .ifPresent(anchor -> anchor.addOmegaPercent(recipe.omegaPercent()));
        }
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.35f, 1.4f);
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.phi_fabricator." + fabricatorClass().name().toLowerCase());
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new PhiFabricatorMenu(id, inv, this, data);
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
            case SLOT_PROGRAM -> MemoryCrystalData.isCrystal(stack);
            case SLOT_SCAN -> true;
            case SLOT_A, SLOT_B, SLOT_C -> true;
            case SLOT_OUT -> false;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        if (side == Direction.UP) {
            return new int[] {SLOT_PROGRAM, SLOT_SCAN};
        }
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUT;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, provider);
        progress = tag.getInt("Progress");
    }
}
