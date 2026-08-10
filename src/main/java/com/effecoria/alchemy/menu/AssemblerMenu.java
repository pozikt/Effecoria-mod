package com.effecoria.alchemy.menu;

import com.effecoria.block.ArtifactAssemblerBlockEntity;
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

public final class AssemblerMenu extends MachineMenu {
    public static final int BUTTON_STAFF = 0;
    public static final int BUTTON_RING = 1;
    public static final int BUTTON_AMULET = 2;
    public static final int BUTTON_CHARM = 3;

    private final ArtifactAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public AssemblerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBe(playerInv, buf), null);
    }

    public AssemblerMenu(int id, Inventory playerInv, ArtifactAssemblerBlockEntity be, ContainerData data) {
        super(
                ModMenus.ARTIFACT_ASSEMBLER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.ARTIFACT_ASSEMBLER.get());
        this.blockEntity = be;
        this.data = data == null ? be.getData() : data;
        checkContainerSize(be, ArtifactAssemblerBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, ArtifactAssemblerBlockEntity.SLOT_A, 44, 35));
        addSlot(new Slot(be, ArtifactAssemblerBlockEntity.SLOT_B, 80, 35));
        addSlot(new Slot(be, ArtifactAssemblerBlockEntity.SLOT_OUT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(this.data);
    }

    private static ArtifactAssemblerBlockEntity getBe(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ArtifactAssemblerBlockEntity assembler) {
            return assembler;
        }
        throw new IllegalStateException("Assembler missing");
    }

    public int progress() {
        return data.get(ArtifactAssemblerBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(ArtifactAssemblerBlockEntity.DATA_MAX));
    }

    public int template() {
        return data.get(ArtifactAssemblerBlockEntity.DATA_TEMPLATE);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_STAFF && id <= BUTTON_CHARM) {
            blockEntity.setTemplate(id);
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
            int machine = ArtifactAssemblerBlockEntity.SLOT_COUNT;
            if (index < machine) {
                if (!moveItemStackTo(stack, machine, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 2, false)) {
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
