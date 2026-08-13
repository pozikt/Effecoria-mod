package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiWaterPurifierBlockEntity;
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

public final class PhiWaterPurifierMenu extends MachineMenu {
    private final PhiWaterPurifierBlockEntity blockEntity;
    private final ContainerData data;

    public PhiWaterPurifierMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(PhiWaterPurifierBlockEntity.DATA_COUNT));
    }

    public PhiWaterPurifierMenu(int id, Inventory playerInv, PhiWaterPurifierBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_WATER_PURIFIER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PHI_WATER_PURIFIER.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, PhiWaterPurifierBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, PhiWaterPurifierBlockEntity.SLOT_INPUT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PHI_WATER_BUCKET.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(be, PhiWaterPurifierBlockEntity.SLOT_FILTER, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PhiWaterPurifierBlockEntity.isFilter(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(be, PhiWaterPurifierBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static PhiWaterPurifierBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiWaterPurifierBlockEntity purifier) {
            return purifier;
        }
        throw new IllegalStateException("Phi water purifier missing at client open");
    }

    public int progress() {
        return data.get(PhiWaterPurifierBlockEntity.DATA_PROGRESS);
    }

    public boolean powered() {
        return data.get(PhiWaterPurifierBlockEntity.DATA_POWER) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return empty;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = PhiWaterPurifierBlockEntity.SLOT_COUNT;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.PHI_WATER_BUCKET.get())) {
            if (!moveItemStackTo(stack, PhiWaterPurifierBlockEntity.SLOT_INPUT, PhiWaterPurifierBlockEntity.SLOT_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (PhiWaterPurifierBlockEntity.isFilter(stack)) {
            if (!moveItemStackTo(stack, PhiWaterPurifierBlockEntity.SLOT_FILTER, PhiWaterPurifierBlockEntity.SLOT_FILTER + 1, false)) {
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
        return copy;
    }
}
