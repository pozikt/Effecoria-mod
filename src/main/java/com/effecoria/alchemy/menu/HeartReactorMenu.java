package com.effecoria.alchemy.menu;

import com.effecoria.block.HeartReactorBlockEntity;
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

public final class HeartReactorMenu extends MachineMenu {
    public static final int BUTTON_TOGGLE = 0;

    private final HeartReactorBlockEntity blockEntity;
    private final ContainerData data;

    public HeartReactorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(HeartReactorBlockEntity.DATA_COUNT));
    }

    public HeartReactorMenu(int id, Inventory playerInv, HeartReactorBlockEntity be, ContainerData data) {
        super(
                ModMenus.HEART_REACTOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.HEART_REACTOR_CORE.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, HeartReactorBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, HeartReactorBlockEntity.SLOT_CATALYST, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HeartReactorBlockEntity.isValidCatalyst(stack);
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static HeartReactorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof HeartReactorBlockEntity reactor) {
            return reactor;
        }
        throw new IllegalStateException("Heart reactor block entity missing at client open");
    }

    public boolean formed() {
        return data.get(HeartReactorBlockEntity.DATA_FORMED) != 0;
    }

    public boolean running() {
        return data.get(HeartReactorBlockEntity.DATA_RUNNING) != 0;
    }

    public boolean primed() {
        return data.get(HeartReactorBlockEntity.DATA_PRIMED) != 0;
    }

    public int overheatCooldown() {
        return data.get(HeartReactorBlockEntity.DATA_OVERHEAT);
    }

    public int boostTicks() {
        return data.get(HeartReactorBlockEntity.DATA_BOOST);
    }

    public float powerFactor() {
        return data.get(HeartReactorBlockEntity.DATA_FACTOR_CENTI) / 100f;
    }

    public boolean cooled() {
        return data.get(HeartReactorBlockEntity.DATA_COOLED) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.IV)) {
            return false;
        }
        if (!blockEntity.isRunning()) {
            if (!blockEntity.isFormed()) {
                player.displayClientMessage(Component.translatable("message.effecoria.heart_not_formed"), true);
                return false;
            }
            if (!blockEntity.isPrimed()
                    && !HeartReactorBlockEntity.isValidCatalyst(
                            blockEntity.getItem(HeartReactorBlockEntity.SLOT_CATALYST))) {
                player.displayClientMessage(Component.translatable("message.effecoria.heart_need_catalyst"), true);
                return false;
            }
            if (!blockEntity.tryStart()) {
                player.displayClientMessage(Component.translatable("message.effecoria.heart_need_catalyst"), true);
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
            int machineSlots = HeartReactorBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (HeartReactorBlockEntity.isValidCatalyst(stack)) {
                if (!moveItemStackTo(
                        stack,
                        HeartReactorBlockEntity.SLOT_CATALYST,
                        HeartReactorBlockEntity.SLOT_CATALYST + 1,
                        false)) {
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
