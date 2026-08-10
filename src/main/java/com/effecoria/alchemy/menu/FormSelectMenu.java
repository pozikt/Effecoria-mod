package com.effecoria.alchemy.menu;

import com.effecoria.block.FacetCutterBlockEntity;
import com.effecoria.block.ShaftLatheBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FormSelectMenu extends MachineMenu {
    public enum Mode {
        LATHE,
        CUTTER
    }

    public static final int BUTTON_PREV = 0;
    public static final int BUTTON_NEXT = 1;

    private final BaseContainerBlockEntity blockEntity;
    private final ContainerData data;
    private final Mode mode;

    public static FormSelectMenu lathe(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (!(be instanceof ShaftLatheBlockEntity lathe)) {
            throw new IllegalStateException("Shaft lathe missing");
        }
        return new FormSelectMenu(id, inv, lathe, lathe.getData(), Mode.LATHE);
    }

    public static FormSelectMenu cutter(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (!(be instanceof FacetCutterBlockEntity cutter)) {
            throw new IllegalStateException("Facet cutter missing");
        }
        return new FormSelectMenu(id, inv, cutter, cutter.getData(), Mode.CUTTER);
    }

    public FormSelectMenu(int id, Inventory playerInv, BaseContainerBlockEntity be, ContainerData data, Mode mode) {
        super(
                mode == Mode.LATHE ? ModMenus.SHAFT_LATHE.get() : ModMenus.FACET_CUTTER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                mode == Mode.LATHE ? ModBlocks.SHAFT_LATHE.get() : ModBlocks.FACET_CUTTER.get());
        this.blockEntity = be;
        this.data = data;
        this.mode = mode;
        checkContainerSize(be, 2);
        addSlot(new Slot(be, 0, 56, 35));
        addSlot(new Slot(be, 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public int formIndex() {
        return data.get(2);
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int current = formIndex();
        if (id == BUTTON_PREV) {
            current--;
        } else if (id == BUTTON_NEXT) {
            current++;
        } else {
            return false;
        }
        if (blockEntity instanceof ShaftLatheBlockEntity lathe) {
            lathe.setFormIndex(current);
            return true;
        }
        if (blockEntity instanceof FacetCutterBlockEntity cutter) {
            cutter.setFormIndex(current);
            return true;
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
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) {
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
