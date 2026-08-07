package com.effecoria.alchemy.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

/** Shared helpers for Φ-alchemy machine menus. */
public abstract class MachineMenu extends AbstractContainerMenu {
    protected final ContainerLevelAccess access;
    protected final Block expectedBlock;

    protected MachineMenu(
            @Nullable MenuType<?> type, int containerId, ContainerLevelAccess access, Block expectedBlock) {
        super(type, containerId);
        this.access = access;
        this.expectedBlock = expectedBlock;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, expectedBlock);
    }

    protected void addPlayerInventory(net.minecraft.world.entity.player.Inventory inv) {
        // Vanilla furnace / hopper layout: inv @ y=84, hotbar @ y=142
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }
}
