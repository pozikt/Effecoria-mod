package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiCartographyTableBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Full-screen Φ-sonar cartography desk (no player inventory). */
public final class PhiCartographyMenu extends MachineMenu {
    private final PhiCartographyTableBlockEntity blockEntity;
    private final ContainerData data;

    public PhiCartographyMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(PhiCartographyTableBlockEntity.DATA_COUNT));
    }

    public PhiCartographyMenu(int id, Inventory playerInv, PhiCartographyTableBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_CARTOGRAPHY_TABLE.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PHI_CARTOGRAPHY_TABLE.get());
        this.blockEntity = be;
        this.data = data;
        addDataSlots(data);
    }

    private static PhiCartographyTableBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiCartographyTableBlockEntity table) {
            return table;
        }
        throw new IllegalStateException("Φ-cartography table missing");
    }

    public PhiCartographyTableBlockEntity blockEntity() {
        return blockEntity;
    }

    public boolean linked() {
        return data.get(PhiCartographyTableBlockEntity.DATA_LINKED) != 0;
    }

    public boolean sonarPresent() {
        return data.get(PhiCartographyTableBlockEntity.DATA_SONAR_PRESENT) != 0;
    }

    public boolean sonarReady() {
        return data.get(PhiCartographyTableBlockEntity.DATA_SONAR_READY) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
