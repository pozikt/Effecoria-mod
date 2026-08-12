package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiBeaconBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhiBeaconMenu extends MachineMenu {
    private final PhiBeaconBlockEntity blockEntity;

    public PhiBeaconMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf));
    }

    public PhiBeaconMenu(int id, Inventory playerInv, PhiBeaconBlockEntity be) {
        super(
                ModMenus.PHI_BEACON.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PHI_BEACON.get());
        this.blockEntity = be;
        addPlayerInventory(playerInv);
    }

    private static PhiBeaconBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiBeaconBlockEntity beacon) {
            return beacon;
        }
        throw new IllegalStateException("Phi beacon missing");
    }

    public PhiBeaconBlockEntity blockEntity() {
        return blockEntity;
    }

    public String beaconName() {
        return blockEntity.beaconName();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
