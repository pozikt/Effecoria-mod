package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiFabricatorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.fabricator.MemoryCrystalData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Program + scan + 3 inputs + output. */
public final class PhiFabricatorMenu extends MachineMenu {
    public static final int BUTTON_WRITE = 0;

    private final PhiFabricatorBlockEntity blockEntity;
    private final ContainerData data;

    public PhiFabricatorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBe(playerInv, buf), new SimpleContainerData(PhiFabricatorBlockEntity.DATA_COUNT));
    }

    public PhiFabricatorMenu(int id, Inventory playerInv, PhiFabricatorBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_FABRICATOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                expectedBlock(be));
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, PhiFabricatorBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, PhiFabricatorBlockEntity.SLOT_PROGRAM, 23, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MemoryCrystalData.isCrystal(stack);
            }
        });
        addSlot(new Slot(be, PhiFabricatorBlockEntity.SLOT_SCAN, 23, 58));
        addSlot(new Slot(be, PhiFabricatorBlockEntity.SLOT_A, 66, 35));
        addSlot(new Slot(be, PhiFabricatorBlockEntity.SLOT_B, 84, 35));
        addSlot(new Slot(be, PhiFabricatorBlockEntity.SLOT_C, 102, 35));
        addSlot(new Slot(be, PhiFabricatorBlockEntity.SLOT_OUT, 152, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static Block expectedBlock(PhiFabricatorBlockEntity be) {
        return switch (be.fabricatorClass()) {
            case I -> ModBlocks.PHI_FABRICATOR.get();
            case II -> ModBlocks.PHI_FABRICATOR_II.get();
            case III -> ModBlocks.PHI_FABRICATOR_III.get();
        };
    }

    private static PhiFabricatorBlockEntity getBe(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiFabricatorBlockEntity fabricator) {
            return fabricator;
        }
        throw new IllegalStateException("Φ-fabricator missing at client open");
    }

    public PhiFabricatorBlockEntity blockEntity() {
        return blockEntity;
    }

    public int progress() {
        return data.get(PhiFabricatorBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(PhiFabricatorBlockEntity.DATA_MAX));
    }

    public boolean hasPower() {
        return data.get(PhiFabricatorBlockEntity.DATA_POWER) != 0;
    }

    public int fabricatorClass() {
        return data.get(PhiFabricatorBlockEntity.DATA_CLASS);
    }

    public int writeStatus() {
        return data.get(PhiFabricatorBlockEntity.DATA_WRITE);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_WRITE && player instanceof ServerPlayer serverPlayer) {
            return blockEntity.tryWriteCrystal(serverPlayer);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machine = PhiFabricatorBlockEntity.SLOT_COUNT;
            if (index < machine) {
                if (!moveItemStackTo(stack, machine, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (MemoryCrystalData.isCrystal(stack)) {
                if (!moveItemStackTo(stack, PhiFabricatorBlockEntity.SLOT_PROGRAM, PhiFabricatorBlockEntity.SLOT_PROGRAM + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PhiFabricatorBlockEntity.SLOT_A, PhiFabricatorBlockEntity.SLOT_OUT, false)
                    && !moveItemStackTo(stack, PhiFabricatorBlockEntity.SLOT_SCAN, PhiFabricatorBlockEntity.SLOT_SCAN + 1, false)) {
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
