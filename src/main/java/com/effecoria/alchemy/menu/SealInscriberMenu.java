package com.effecoria.alchemy.menu;

import com.effecoria.block.SealInscriberBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SealInscriberMenu extends MachineMenu {
    public static final int BUTTON_PREV_SEAL = 0;
    public static final int BUTTON_NEXT_SEAL = 1;
    public static final int BUTTON_LEVEL_DOWN = 2;
    public static final int BUTTON_LEVEL_UP = 3;
    public static final int BUTTON_APPLY = 4;
    public static final int BUTTON_STRIP = 5;
    /** {@code id = SELECT_INDEX_BASE + sealIndex} picks that seal in the catalog list. */
    public static final int SELECT_INDEX_BASE = 100;

    private final SealInscriberBlockEntity blockEntity;
    private final ContainerData data;

    public SealInscriberMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBe(playerInv, buf), null);
    }

    public SealInscriberMenu(int id, Inventory playerInv, SealInscriberBlockEntity be, ContainerData data) {
        super(
                ModMenus.SEAL_INSCRIBER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.SEAL_INSCRIBER.get());
        this.blockEntity = be;
        this.data = data == null ? be.getData() : data;
        checkContainerSize(be, SealInscriberBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, SealInscriberBlockEntity.SLOT_TARGET, StonecutterMenuLayout.SLOT_INPUT_X, StonecutterMenuLayout.SLOT_INPUT_Y));
        addPlayerInventory(playerInv);
        addDataSlots(this.data);
    }

    private static SealInscriberBlockEntity getBe(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof SealInscriberBlockEntity inscriber) {
            return inscriber;
        }
        throw new IllegalStateException("Seal inscriber missing");
    }

    public int sealIndex() {
        return data.get(SealInscriberBlockEntity.DATA_SEAL);
    }

    public int sealLevel() {
        return data.get(SealInscriberBlockEntity.DATA_LEVEL);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BUTTON_PREV_SEAL -> {
                blockEntity.setSealIndex(sealIndex() - 1);
                yield true;
            }
            case BUTTON_NEXT_SEAL -> {
                blockEntity.setSealIndex(sealIndex() + 1);
                yield true;
            }
            case BUTTON_LEVEL_DOWN -> {
                blockEntity.setSealLevel(Math.max(1, sealLevel() - 1));
                yield true;
            }
            case BUTTON_LEVEL_UP -> {
                blockEntity.setSealLevel(sealLevel() + 1);
                yield true;
            }
            case BUTTON_APPLY -> blockEntity.tryInscribe(player);
            case BUTTON_STRIP -> blockEntity.tryStrip(player);
            default -> {
                if (id >= SELECT_INDEX_BASE) {
                    blockEntity.setSealIndex(id - SELECT_INDEX_BASE);
                    yield true;
                }
                yield false;
            }
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
