package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiTurretBlockEntity;
import com.effecoria.content.ModMenus;
import com.effecoria.core.alchemy.TurretKind;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhiTurretMenu extends MachineMenu {
    public static final int BUTTON_ARM = 0;

    private final PhiTurretBlockEntity blockEntity;
    private final ContainerData data;

    public PhiTurretMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBe(playerInv, buf), new SimpleContainerData(PhiTurretBlockEntity.DATA_COUNT));
    }

    public PhiTurretMenu(int id, Inventory playerInv, PhiTurretBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_TURRET.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                be.getBlockState().getBlock());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, PhiTurretBlockEntity.SLOT_COUNT);

        addSlot(new Slot(be, PhiTurretBlockEntity.SLOT_AMMO, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return be.kind().needsAmmo() && be.kind().isValidAmmo(stack);
            }

            @Override
            public boolean isActive() {
                return be.kind().needsAmmo();
            }
        });

        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static PhiTurretBlockEntity getBe(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiTurretBlockEntity turret) {
            return turret;
        }
        throw new IllegalStateException("Phi turret missing");
    }

    public boolean armed() {
        return data.get(PhiTurretBlockEntity.DATA_ARMED) != 0;
    }

    public int heat() {
        return data.get(PhiTurretBlockEntity.DATA_HEAT);
    }

    public int cooldown() {
        return data.get(PhiTurretBlockEntity.DATA_COOLDOWN);
    }

    public int powerCenti() {
        return data.get(PhiTurretBlockEntity.DATA_POWER);
    }

    public TurretKind kind() {
        int i = data.get(PhiTurretBlockEntity.DATA_KIND);
        TurretKind[] values = TurretKind.values();
        return values[Math.max(0, Math.min(values.length - 1, i))];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && !com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                        serverPlayer, com.effecoria.core.technomagic.TechnomagicEra.IV)) {
            return false;
        }
        if (id == BUTTON_ARM) {
            blockEntity.toggleArmed();
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
            int machine = PhiTurretBlockEntity.SLOT_COUNT;
            if (index < machine) {
                if (!moveItemStackTo(stack, machine, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (blockEntity.kind().needsAmmo() && blockEntity.kind().isValidAmmo(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
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
