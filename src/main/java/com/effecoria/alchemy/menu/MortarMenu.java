package com.effecoria.alchemy.menu;

import com.effecoria.alchemy.recipe.MortarRecipes;
import com.effecoria.block.MortarBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MortarMenu extends MachineMenu {
    private final MortarBlockEntity blockEntity;
    private final ContainerData data;

    public MortarMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(MortarBlockEntity.DATA_COUNT));
    }

    public MortarMenu(int id, Inventory playerInv, MortarBlockEntity be, ContainerData data) {
        super(ModMenus.MORTAR.get(), id, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), ModBlocks.MORTAR_AND_PESTLE.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, MortarBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, MortarBlockEntity.SLOT_INPUT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MortarRecipes.isInput(stack);
            }
        });
        addSlot(new Slot(be, MortarBlockEntity.SLOT_PRIMARY, 116, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(be, MortarBlockEntity.SLOT_BYPRODUCT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(be, MortarBlockEntity.SLOT_WASTE, 116, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(be, MortarBlockEntity.SLOT_DRIVE, 26, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PHI_CELL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static MortarBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof MortarBlockEntity mortar) {
            return mortar;
        }
        throw new IllegalStateException("Mortar block entity missing at client open");
    }

    public MortarBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int progress() {
        return data.get(MortarBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        int max = data.get(MortarBlockEntity.DATA_MAX);
        return max <= 0 ? 1 : max;
    }

    public boolean autoMode() {
        return data.get(MortarBlockEntity.DATA_AUTO) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = MortarBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (MortarRecipes.isInput(stack)) {
                if (!moveItemStackTo(stack, MortarBlockEntity.SLOT_INPUT, MortarBlockEntity.SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.PHI_CELL.get())) {
                if (!moveItemStackTo(stack, MortarBlockEntity.SLOT_DRIVE, MortarBlockEntity.SLOT_DRIVE + 1, false)) {
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
