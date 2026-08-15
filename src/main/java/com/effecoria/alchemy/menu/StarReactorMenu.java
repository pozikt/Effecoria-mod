package com.effecoria.alchemy.menu;

import com.effecoria.block.StarReactorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class StarReactorMenu extends MachineMenu {
    public static final int BUTTON_TOGGLE = 0;

    private final StarReactorBlockEntity blockEntity;
    private final ContainerData data;

    public StarReactorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(StarReactorBlockEntity.DATA_COUNT));
    }

    public StarReactorMenu(int id, Inventory playerInv, StarReactorBlockEntity be, ContainerData data) {
        super(
                ModMenus.STAR_REACTOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.STAR_REACTOR_CORE.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, StarReactorBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, StarReactorBlockEntity.SLOT_FUEL, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StarReactorBlockEntity.isValidFuel(stack);
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static StarReactorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof StarReactorBlockEntity star) {
            return star;
        }
        throw new IllegalStateException("Star reactor missing at client open");
    }

    public boolean formed() {
        return data.get(StarReactorBlockEntity.DATA_FORMED) != 0;
    }

    public boolean running() {
        return data.get(StarReactorBlockEntity.DATA_RUNNING) != 0;
    }

    public int fuelTicks() {
        return data.get(StarReactorBlockEntity.DATA_FUEL_TICKS);
    }

    public int omegaCentis() {
        return data.get(StarReactorBlockEntity.DATA_OMEGA_CENTI);
    }

    public boolean cooled() {
        return data.get(StarReactorBlockEntity.DATA_COOLED) != 0;
    }

    public float powerFactor() {
        return data.get(StarReactorBlockEntity.DATA_FACTOR_CENTI) / 100f;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer
                && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.VI)) {
            return false;
        }
        if (!blockEntity.isRunning()) {
            if (!blockEntity.isFormed()) {
                player.displayClientMessage(Component.translatable("message.effecoria.star_reactor_not_formed"), true);
                return false;
            }
            if (!blockEntity.tryStart()) {
                player.displayClientMessage(Component.translatable("message.effecoria.star_reactor_need_fuel"), true);
                return false;
            }
            return true;
        }
        blockEntity.stop();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = StarReactorBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (StarReactorBlockEntity.isValidFuel(stack)) {
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
