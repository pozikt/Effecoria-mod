package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiIncubatorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.tower.TowerBodyType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Three ingredient wells on {@code textures/gui/phi_incubator.png}. */
public final class PhiIncubatorMenu extends MachineMenu {
    private final PhiIncubatorBlockEntity blockEntity;
    private final ContainerData data;

    public PhiIncubatorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(PhiIncubatorBlockEntity.DATA_COUNT));
    }

    public PhiIncubatorMenu(int id, Inventory playerInv, PhiIncubatorBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_INCUBATOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PHI_INCUBATOR.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, PhiIncubatorBlockEntity.SLOT_COUNT);
        if (be.getLevel() != null && !be.getLevel().isClientSide()) {
            be.refreshLinkCache();
        }
        addSlot(ingredientSlot(be, 0, 26, 35));
        addSlot(ingredientSlot(be, 1, 44, 35));
        addSlot(ingredientSlot(be, 2, 62, 35));
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static Slot ingredientSlot(PhiIncubatorBlockEntity be, int index, int x, int y) {
        return new Slot(be, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PhiIncubatorBlockEntity.isBodyIngredient(stack.getItem());
            }
        };
    }

    private static PhiIncubatorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiIncubatorBlockEntity incubator) {
            return incubator;
        }
        throw new IllegalStateException("Φ-incubator block entity missing at client open");
    }

    public PhiIncubatorBlockEntity blockEntity() {
        return blockEntity;
    }

    public int progress() {
        return data.get(PhiIncubatorBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(PhiIncubatorBlockEntity.DATA_MAX));
    }

    public boolean hasPower() {
        return data.get(PhiIncubatorBlockEntity.DATA_POWER) != 0;
    }

    public boolean linked() {
        return data.get(PhiIncubatorBlockEntity.DATA_LINKED) != 0;
    }

    public TowerBodyType readyBody() {
        int v = data.get(PhiIncubatorBlockEntity.DATA_READY);
        if (v <= 0) {
            return null;
        }
        TowerBodyType[] values = TowerBodyType.values();
        int i = Math.min(values.length - 1, v - 1);
        return values[i];
    }

    public TowerBodyType targetBody() {
        TowerBodyType[] values = TowerBodyType.values();
        int ord = data.get(PhiIncubatorBlockEntity.DATA_TARGET);
        int i = Math.max(0, Math.min(values.length - 1, ord));
        return values[i];
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = PhiIncubatorBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (PhiIncubatorBlockEntity.isBodyIngredient(stack.getItem())) {
                if (!moveItemStackTo(stack, 0, machineSlots, false)) {
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
