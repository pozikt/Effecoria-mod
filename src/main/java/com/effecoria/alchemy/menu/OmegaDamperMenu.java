package com.effecoria.alchemy.menu;

import com.effecoria.block.OmegaDamperBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.content.OmegaRodItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Three Ω-rod sockets. */
public final class OmegaDamperMenu extends MachineMenu {
    private final OmegaDamperBlockEntity blockEntity;
    private final ContainerData data;

    public OmegaDamperMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(OmegaDamperBlockEntity.DATA_COUNT));
    }

    public OmegaDamperMenu(int id, Inventory playerInv, OmegaDamperBlockEntity be, ContainerData data) {
        super(
                ModMenus.OMEGA_DAMPER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.OMEGA_DAMPER.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, OmegaDamperBlockEntity.SLOT_COUNT);
        addSlot(rodSlot(be, 0, 53, 35));
        addSlot(rodSlot(be, 1, 80, 35));
        addSlot(rodSlot(be, 2, 107, 35));
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static Slot rodSlot(OmegaDamperBlockEntity be, int index, int x, int y) {
        return new Slot(be, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OmegaRodItem.isOmegaRod(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
    }

    private static OmegaDamperBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof OmegaDamperBlockEntity damper) {
            return damper;
        }
        throw new IllegalStateException("Ω-damper block entity missing at client open");
    }

    public OmegaDamperBlockEntity blockEntity() {
        return blockEntity;
    }

    public boolean scrubbing() {
        return data.get(OmegaDamperBlockEntity.DATA_SCRUBBING) != 0;
    }

    public int status() {
        return data.get(OmegaDamperBlockEntity.DATA_STATUS);
    }

    public int towerOmegaPercent() {
        return data.get(OmegaDamperBlockEntity.DATA_TOWER_OMEGA);
    }

    public int forgeOmegaPercent() {
        return data.get(OmegaDamperBlockEntity.DATA_FORGE_OMEGA);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = OmegaDamperBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (OmegaRodItem.isOmegaRod(stack)) {
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
