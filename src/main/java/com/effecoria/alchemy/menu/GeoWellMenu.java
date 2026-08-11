package com.effecoria.alchemy.menu;

import com.effecoria.block.GeoWellBlockEntity;
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

public final class GeoWellMenu extends MachineMenu {
    public static final int BUTTON_TOGGLE = 0;

    private final GeoWellBlockEntity blockEntity;
    private final ContainerData data;

    public GeoWellMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(GeoWellBlockEntity.DATA_COUNT));
    }

    public GeoWellMenu(int id, Inventory playerInv, GeoWellBlockEntity be, ContainerData data) {
        super(
                ModMenus.GEO_WELL.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.GEO_WELL_CORE.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, GeoWellBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, GeoWellBlockEntity.SLOT_FUEL, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GeoWellBlockEntity.isValidFuel(stack);
            }
        });
        addSlot(new Slot(be, GeoWellBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static GeoWellBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof GeoWellBlockEntity well) {
            return well;
        }
        throw new IllegalStateException("Geo well block entity missing at client open");
    }

    public boolean formed() {
        return data.get(GeoWellBlockEntity.DATA_FORMED) != 0;
    }

    public boolean running() {
        return data.get(GeoWellBlockEntity.DATA_RUNNING) != 0;
    }

    public int fuelTicks() {
        return data.get(GeoWellBlockEntity.DATA_FUEL_TICKS);
    }

    public int omegaCentis() {
        return data.get(GeoWellBlockEntity.DATA_OMEGA_CENTI);
    }

    public boolean cooled() {
        return data.get(GeoWellBlockEntity.DATA_COOLED) != 0;
    }

    public float powerFactor() {
        return data.get(GeoWellBlockEntity.DATA_FACTOR_CENTI) / 100f;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer
                && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.V)) {
            return false;
        }
        if (!blockEntity.isRunning()) {
            if (!blockEntity.isFormed()) {
                player.displayClientMessage(Component.translatable("message.effecoria.geo_well_not_formed"), true);
                return false;
            }
            if (!blockEntity.tryStart()) {
                player.displayClientMessage(Component.translatable("message.effecoria.geo_well_need_fuel"), true);
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
            int machineSlots = GeoWellBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (GeoWellBlockEntity.isValidFuel(stack)) {
                if (!moveItemStackTo(
                        stack, GeoWellBlockEntity.SLOT_FUEL, GeoWellBlockEntity.SLOT_FUEL + 1, false)) {
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
