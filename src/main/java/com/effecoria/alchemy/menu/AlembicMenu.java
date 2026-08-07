package com.effecoria.alchemy.menu;

import com.effecoria.alchemy.recipe.AlembicRecipes;
import com.effecoria.block.EssenceAlembicBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModMenus;
import com.effecoria.core.alchemy.HeatLevel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AlembicMenu extends MachineMenu {
    private final EssenceAlembicBlockEntity blockEntity;
    private final ContainerData data;

    public AlembicMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(EssenceAlembicBlockEntity.DATA_COUNT));
    }

    public AlembicMenu(int id, Inventory playerInv, EssenceAlembicBlockEntity be, ContainerData data) {
        super(
                ModMenus.ALEMBIC.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.ESSENCE_ALEMBIC.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, EssenceAlembicBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, EssenceAlembicBlockEntity.SLOT_WATER, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AlembicRecipes.isWater(stack);
            }
        });
        addSlot(new Slot(be, EssenceAlembicBlockEntity.SLOT_REAGENT1, 62, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AlembicRecipes.isPowerReagent(stack);
            }
        });
        addSlot(new Slot(be, EssenceAlembicBlockEntity.SLOT_REAGENT2, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL);
            }
        });
        addSlot(new Slot(be, EssenceAlembicBlockEntity.SLOT_REAGENT3, 62, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL);
            }
        });
        addSlot(new Slot(be, EssenceAlembicBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static EssenceAlembicBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof EssenceAlembicBlockEntity alembic) {
            return alembic;
        }
        throw new IllegalStateException("Alembic block entity missing at client open");
    }

    public EssenceAlembicBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int progress() {
        return data.get(EssenceAlembicBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        int max = data.get(EssenceAlembicBlockEntity.DATA_MAX);
        return max <= 0 ? 1 : max;
    }

    public HeatLevel heatLevel() {
        return HeatLevel.byId(data.get(EssenceAlembicBlockEntity.DATA_HEAT));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = EssenceAlembicBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (AlembicRecipes.isWater(stack)) {
                if (!moveItemStackTo(stack, EssenceAlembicBlockEntity.SLOT_WATER, EssenceAlembicBlockEntity.SLOT_WATER + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (AlembicRecipes.isPowerReagent(stack)) {
                if (!moveItemStackTo(
                        stack, EssenceAlembicBlockEntity.SLOT_REAGENT1, EssenceAlembicBlockEntity.SLOT_REAGENT1 + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItemTags.ALEMBIC_REAGENT_OPTIONAL)) {
                if (!moveItemStackTo(
                        stack, EssenceAlembicBlockEntity.SLOT_REAGENT2, EssenceAlembicBlockEntity.SLOT_REAGENT3 + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
