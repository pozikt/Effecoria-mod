package com.effecoria.block;

import com.effecoria.alchemy.menu.AssemblerMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeat;
import com.effecoria.core.artifact.ArtifactCatalog;
import com.effecoria.core.artifact.AssembleRecipeDefinition;
import com.effecoria.core.artifact.AssembledGearData;
import com.effecoria.core.artifact.ModularPartData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class ArtifactAssemblerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_A = 0;
    public static final int SLOT_B = 1;
    public static final int SLOT_OUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_TEMPLATE = 2;
    public static final int DATA_COUNT = 3;

    public static final int TEMPLATE_STAFF = 0;
    public static final int TEMPLATE_RING = 1;
    public static final int TEMPLATE_AMULET = 2;
    public static final int TEMPLATE_CHARM = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = 80;
    private int template = TEMPLATE_STAFF;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_TEMPLATE -> template;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX -> maxProgress = value;
                case DATA_TEMPLATE -> template = Math.floorMod(value, 4);
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ArtifactAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARTIFACT_ASSEMBLER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public void setTemplate(int next) {
        template = Math.floorMod(next, 4);
        progress = 0;
        setChanged();
    }

    private String templateId() {
        return switch (template) {
            case TEMPLATE_RING -> AssembledGearData.TEMPLATE_RING;
            case TEMPLATE_AMULET -> AssembledGearData.TEMPLATE_AMULET;
            case TEMPLATE_CHARM -> AssembledGearData.TEMPLATE_CHARM;
            default -> AssembledGearData.TEMPLATE_STAFF;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.artifact_assembler");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new AssemblerMenu(id, inv, this, data);
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
        return index != SLOT_OUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[] {SLOT_OUT} : new int[] {SLOT_A, SLOT_B};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index != SLOT_OUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUT;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArtifactAssemblerBlockEntity be) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) {
            return;
        }
        HeatLevel heat = PhiHeat.getNeighborHeat(level, pos);
        if (heat.ordinal() < HeatLevel.MEDIUM.ordinal()) {
            be.progress = 0;
            return;
        }
        String tmpl = be.templateId();
        AssembleRecipeDefinition recipe = ArtifactCatalog.assembleFor(tmpl).orElse(null);
        if (recipe == null) {
            be.progress = 0;
            return;
        }
        ItemStack a = be.items.get(SLOT_A);
        ItemStack b = be.items.get(SLOT_B);
        ItemStack out = be.items.get(SLOT_OUT);
        if (!matches(a, recipe.inputAKind()) || !matches(b, recipe.inputBKind()) || !out.isEmpty()) {
            be.progress = 0;
            return;
        }
        be.maxProgress = recipe.cookTicks();
        be.progress++;
        if (be.progress >= be.maxProgress) {
            ItemStack result;
            if (AssembledGearData.TEMPLATE_STAFF.equals(tmpl)) {
                result = AssembledGearData.assembleStaff(a, b);
            } else {
                Item item = BuiltInRegistries.ITEM.get(recipe.resultItem());
                result = AssembledGearData.assembleJewelry(tmpl, item, a, b, level.getRandom());
            }
            be.items.set(SLOT_OUT, result);
            a.shrink(1);
            b.shrink(1);
            be.progress = 0;
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.45f, 1.4f);
            be.setChanged();
        }
    }

    private static boolean matches(ItemStack stack, String kind) {
        return switch (kind) {
            case "shaft" -> ModularPartData.isShaft(stack);
            case "focus" -> ModularPartData.isFocus(stack);
            case "band" -> ModularPartData.isBand(stack);
            case "gem" -> ModularPartData.isGem(stack);
            default -> false;
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Template", template);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        template = tag.getInt("Template");
    }
}
