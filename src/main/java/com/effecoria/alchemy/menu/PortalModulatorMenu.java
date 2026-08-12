package com.effecoria.alchemy.menu;

import com.effecoria.block.PortalModulatorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.alchemy.PhiBeaconIndex;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PortalModulatorMenu extends MachineMenu {
    public static final int BUTTON_OPEN = 0;
    public static final int BUTTON_CLOSE = 1;
    public static final int BUTTON_MODE_COORDS = 2;
    public static final int BUTTON_MODE_BEACON = 3;

    public static final int PANEL_WIDTH = 220;
    public static final int PANEL_HEIGHT = 200;
    public static final int PLAYER_INV_X = 29;
    public static final int PLAYER_INV_Y = 118;

    private final PortalModulatorBlockEntity blockEntity;
    private final ContainerData data;

    public PortalModulatorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(
                id,
                playerInv,
                getBlockEntity(playerInv, buf),
                new SimpleContainerData(PortalModulatorBlockEntity.DATA_COUNT));
    }

    public PortalModulatorMenu(int id, Inventory playerInv, PortalModulatorBlockEntity be, ContainerData data) {
        super(
                ModMenus.PORTAL_MODULATOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PORTAL_MODULATOR.get());
        this.blockEntity = be;
        this.data = data;
        addPlayerInventory(playerInv, PLAYER_INV_X, PLAYER_INV_Y);
        addDataSlots(data);
    }

    private static PortalModulatorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PortalModulatorBlockEntity mod) {
            return mod;
        }
        throw new IllegalStateException("Portal modulator missing");
    }

    public PortalModulatorBlockEntity blockEntity() {
        return blockEntity;
    }

    public boolean open() {
        return data.get(PortalModulatorBlockEntity.DATA_OPEN) != 0;
    }

    public int mode() {
        return data.get(PortalModulatorBlockEntity.DATA_MODE);
    }

    public int targetX() {
        return data.get(PortalModulatorBlockEntity.DATA_TARGET_X);
    }

    public int targetY() {
        return data.get(PortalModulatorBlockEntity.DATA_TARGET_Y);
    }

    public int targetZ() {
        return data.get(PortalModulatorBlockEntity.DATA_TARGET_Z);
    }

    public boolean frameOk() {
        return data.get(PortalModulatorBlockEntity.DATA_FRAME_OK) != 0;
    }

    public int powerCenti() {
        return data.get(PortalModulatorBlockEntity.DATA_POWER_CENTI);
    }

    public boolean cooled() {
        return data.get(PortalModulatorBlockEntity.DATA_COOLED) != 0;
    }

    public int overheat() {
        return data.get(PortalModulatorBlockEntity.DATA_OVERHEAT);
    }

    public List<String> beaconNames() {
        if (blockEntity.getLevel() == null) {
            return List.of();
        }
        Map<String, BlockPos> map = PhiBeaconIndex.allIn(blockEntity.getLevel().dimension());
        List<String> names = new ArrayList<>(map.keySet());
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public String selectedBeacon() {
        return blockEntity.beaconName();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer
                && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.V)) {
            return false;
        }
        return switch (id) {
            case BUTTON_OPEN -> blockEntity.tryOpen(player);
            case BUTTON_CLOSE -> {
                blockEntity.forceClose();
                yield true;
            }
            case BUTTON_MODE_COORDS -> {
                blockEntity.setMode(0);
                yield true;
            }
            case BUTTON_MODE_BEACON -> {
                blockEntity.setMode(1);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
