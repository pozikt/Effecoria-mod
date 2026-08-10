package com.effecoria.block;

import com.effecoria.alchemy.menu.FormSelectMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeat;
import com.effecoria.core.artifact.ArtifactCatalog;
import com.effecoria.core.artifact.FocusCutDefinition;
import com.effecoria.core.artifact.ModularPartData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public final class FacetCutterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_FORM = 2;
    public static final int DATA_COUNT = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = 120;
    private int formIndex;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_FORM -> formIndex;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX -> maxProgress = value;
                case DATA_FORM -> formIndex = Math.max(0, value);
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FacetCutterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACET_CUTTER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public void setFormIndex(int index) {
        List<FocusCutDefinition> cuts = ArtifactCatalog.focusCuts();
        if (cuts.isEmpty()) {
            formIndex = 0;
        } else {
            formIndex = Math.floorMod(index, cuts.size());
        }
        progress = 0;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.facet_cutter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new FormSelectMenu(id, inv, this, data, FormSelectMenu.Mode.CUTTER);
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
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index == SLOT_IN;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[] {SLOT_OUT} : new int[] {SLOT_IN};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == SLOT_IN;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FacetCutterBlockEntity be) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        HeatLevel heat = PhiHeat.getNeighborHeat(level, pos);
        if (heat.ordinal() < HeatLevel.MEDIUM.ordinal()) {
            be.progress = 0;
            return;
        }
        List<FocusCutDefinition> cuts = ArtifactCatalog.focusCuts();
        if (cuts.isEmpty()) {
            return;
        }
        be.formIndex = Math.floorMod(be.formIndex, cuts.size());
        FocusCutDefinition cut = cuts.get(be.formIndex);
        ItemStack in = be.items.get(SLOT_IN);
        ItemStack out = be.items.get(SLOT_OUT);
        if (in.isEmpty() || !ArtifactCatalog.materialMatchesFocus(in, cut) || !out.isEmpty()) {
            be.progress = 0;
            return;
        }
        be.maxProgress = cut.cookTicks();
        be.progress++;
        if (be.progress >= be.maxProgress) {
            ItemStack result = ModularPartData.createFocus(in.getItem(), cut.id());
            be.items.set(SLOT_OUT, result);
            in.shrink(1);
            be.progress = 0;
            level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.8f, 1.3f);
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("FormIndex", formIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        formIndex = tag.getInt("FormIndex");
    }
}
