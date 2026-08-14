package com.effecoria.alchemy.menu;

import com.effecoria.block.TowerConsoleBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.effecoria.core.tower.TowerBodyType;
import com.effecoria.core.tower.TowerFacility;
import com.effecoria.core.tower.TowerStructureValidator;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public final class TowerConsoleMenu extends MachineMenu {
    public static final int BUTTON_DOME = 0;
    public static final int BUTTON_BODY = 1;
    public static final int BUTTON_PHOENIX = 2;

    private final TowerConsoleBlockEntity blockEntity;
    private final ContainerData data;

    public TowerConsoleMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(TowerConsoleBlockEntity.DATA_COUNT));
    }

    public TowerConsoleMenu(int id, Inventory playerInv, TowerConsoleBlockEntity be, ContainerData data) {
        super(
                ModMenus.TOWER_CONSOLE.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.TOWER_CONSOLE.get());
        this.blockEntity = be;
        this.data = data;
        addDataSlots(data);
    }

    private static TowerConsoleBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof TowerConsoleBlockEntity console) {
            return console;
        }
        throw new IllegalStateException("Tower console block entity missing at client open");
    }

    public TowerConsoleBlockEntity blockEntity() {
        return blockEntity;
    }

    public List<TowerFacility.MonitorEntry> monitors() {
        return blockEntity.monitors();
    }

    public int integrity() {
        return data.get(TowerConsoleBlockEntity.DATA_INTEGRITY);
    }

    public int omega() {
        return data.get(TowerConsoleBlockEntity.DATA_OMEGA);
    }

    public boolean domePowered() {
        return data.get(TowerConsoleBlockEntity.DATA_DOME_POWERED) != 0;
    }

    public boolean domeCombat() {
        return data.get(TowerConsoleBlockEntity.DATA_DOME_COMBAT) != 0;
    }

    public TowerBodyType bodyType() {
        int ord = data.get(TowerConsoleBlockEntity.DATA_BODY);
        TowerBodyType[] values = TowerBodyType.values();
        int i = Math.max(0, Math.min(values.length - 1, ord));
        return values[i];
    }

    public boolean amuletCharged() {
        return data.get(TowerConsoleBlockEntity.DATA_AMULET) != 0;
    }

    public boolean bound() {
        return data.get(TowerConsoleBlockEntity.DATA_BOUND) != 0;
    }

    public boolean consecrated() {
        return data.get(TowerConsoleBlockEntity.DATA_CONSECRATED) != 0;
    }

    public boolean linked() {
        return data.get(TowerConsoleBlockEntity.DATA_LINKED) != 0;
    }

    public int gluedCells() {
        return data.get(TowerConsoleBlockEntity.DATA_CELLS);
    }

    public int presentBlocks() {
        return data.get(TowerConsoleBlockEntity.DATA_PRESENT);
    }

    public double verticality() {
        return data.get(TowerConsoleBlockEntity.DATA_VERTICALITY) / 100.0;
    }

    public double scatter() {
        return data.get(TowerConsoleBlockEntity.DATA_SCATTER) / 100.0;
    }

    public TowerStructureValidator.ReactorClass reactorClass() {
        TowerStructureValidator.ReactorClass[] values = TowerStructureValidator.ReactorClass.values();
        int ord = data.get(TowerConsoleBlockEntity.DATA_REACTOR);
        int i = Math.max(0, Math.min(values.length - 1, ord));
        return values[i];
    }

    public int reviveCount() {
        return data.get(TowerConsoleBlockEntity.DATA_REVIVES);
    }

    public boolean phiPower() {
        return data.get(TowerConsoleBlockEntity.DATA_PHI_POWER) != 0;
    }

    public boolean sonarPresent() {
        return data.get(TowerConsoleBlockEntity.DATA_SONAR_PRESENT) != 0;
    }

    public boolean sonarReady() {
        return data.get(TowerConsoleBlockEntity.DATA_SONAR_READY) != 0;
    }

    public boolean phoenixEdictEnabled() {
        return data.get(TowerConsoleBlockEntity.DATA_PHOENIX) != 0;
    }

    public boolean phoenixWatchdogActive() {
        return data.get(TowerConsoleBlockEntity.DATA_WATCHDOG) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer
                && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.VI)) {
            return false;
        }
        return switch (id) {
            case BUTTON_DOME -> blockEntity.tryToggleDome(player);
            case BUTTON_BODY -> blockEntity.tryCycleBody(player);
            case BUTTON_PHOENIX -> blockEntity.tryTogglePhoenix(player);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
