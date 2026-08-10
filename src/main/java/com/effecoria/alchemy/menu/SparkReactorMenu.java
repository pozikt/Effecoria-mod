package com.effecoria.alchemy.menu;

import com.effecoria.block.SparkReactorBlockEntity;
import com.effecoria.content.ModBlocks;
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

public final class SparkReactorMenu extends MachineMenu {
    public static final int BUTTON_TOGGLE = 0;

    private final SparkReactorBlockEntity blockEntity;
    private final ContainerData data;

    public SparkReactorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(SparkReactorBlockEntity.DATA_COUNT));
    }

    public SparkReactorMenu(int id, Inventory playerInv, SparkReactorBlockEntity be, ContainerData data) {
        super(
                ModMenus.SPARK_REACTOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.SPARK_REACTOR.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, SparkReactorBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, SparkReactorBlockEntity.SLOT_FUEL, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SparkReactorBlockEntity.isValidFuel(stack);
            }
        });
        addSlot(new Slot(be, SparkReactorBlockEntity.SLOT_CHARGE, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SparkReactorBlockEntity.isChargeCell(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static SparkReactorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof SparkReactorBlockEntity reactor) {
            return reactor;
        }
        throw new IllegalStateException("Spark reactor block entity missing at client open");
    }

    public SparkReactorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int fuelTicks() {
        return data.get(SparkReactorBlockEntity.DATA_FUEL);
    }

    public int fuelMax() {
        return Math.max(1, data.get(SparkReactorBlockEntity.DATA_FUEL_MAX));
    }

    public boolean running() {
        return data.get(SparkReactorBlockEntity.DATA_RUNNING) != 0;
    }

    public int overheatCooldown() {
        return data.get(SparkReactorBlockEntity.DATA_OVERHEAT);
    }

    public int boostTicks() {
        return data.get(SparkReactorBlockEntity.DATA_BOOST);
    }

    public float powerFactor() {
        return data.get(SparkReactorBlockEntity.DATA_FACTOR_CENTI) / 100f;
    }

    public int chargeProgress() {
        return data.get(SparkReactorBlockEntity.DATA_CHARGE_PROGRESS);
    }

    public float fuelRatio() {
        return Math.min(1f, fuelTicks() / (float) fuelMax());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE) {
            return false;
        }
        blockEntity.toggleRunning();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = SparkReactorBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (SparkReactorBlockEntity.isValidFuel(stack)) {
                if (!moveItemStackTo(
                        stack, SparkReactorBlockEntity.SLOT_FUEL, SparkReactorBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (SparkReactorBlockEntity.isChargeCell(stack)) {
                if (!moveItemStackTo(
                        stack, SparkReactorBlockEntity.SLOT_CHARGE, SparkReactorBlockEntity.SLOT_CHARGE + 1, false)) {
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
