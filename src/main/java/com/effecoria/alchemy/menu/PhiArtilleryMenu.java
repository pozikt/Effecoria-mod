package com.effecoria.alchemy.menu;

import com.effecoria.block.PhiArtilleryBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhiArtilleryMenu extends MachineMenu {
    public static final int BUTTON_YAW_LEFT = 0;
    public static final int BUTTON_YAW_RIGHT = 1;
    public static final int BUTTON_PITCH_UP = 2;
    public static final int BUTTON_PITCH_DOWN = 3;
    public static final int BUTTON_FIRE = 4;
    public static final int BUTTON_HOLD = 5;

    private final PhiArtilleryBlockEntity blockEntity;
    private final ContainerData data;

    public PhiArtilleryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBe(playerInv, buf), new SimpleContainerData(PhiArtilleryBlockEntity.DATA_COUNT));
    }

    public PhiArtilleryMenu(int id, Inventory playerInv, PhiArtilleryBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_ARTILLERY.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PHI_ARTILLERY_BASE.get());
        this.blockEntity = be;
        this.data = data;
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static PhiArtilleryBlockEntity getBe(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiArtilleryBlockEntity artillery) {
            return artillery;
        }
        throw new IllegalStateException("Φ-artillery missing");
    }

    public boolean formed() {
        return data.get(PhiArtilleryBlockEntity.DATA_FORMED) != 0;
    }

    public int yaw() {
        return data.get(PhiArtilleryBlockEntity.DATA_YAW);
    }

    public int pitch() {
        return data.get(PhiArtilleryBlockEntity.DATA_PITCH);
    }

    public int heat() {
        return data.get(PhiArtilleryBlockEntity.DATA_HEAT);
    }

    public boolean hold() {
        return data.get(PhiArtilleryBlockEntity.DATA_HOLD) != 0;
    }

    public float powerFactor() {
        return data.get(PhiArtilleryBlockEntity.DATA_POWER) / 100f;
    }

    public boolean firing() {
        return data.get(PhiArtilleryBlockEntity.DATA_FIRING) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer
                && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.VI)) {
            return false;
        }
        return switch (id) {
            case BUTTON_YAW_LEFT -> {
                blockEntity.nudgeYaw(-PhiArtilleryBlockEntity.YAW_STEP);
                yield true;
            }
            case BUTTON_YAW_RIGHT -> {
                blockEntity.nudgeYaw(PhiArtilleryBlockEntity.YAW_STEP);
                yield true;
            }
            case BUTTON_PITCH_UP -> {
                blockEntity.nudgePitch(PhiArtilleryBlockEntity.YAW_STEP);
                yield true;
            }
            case BUTTON_PITCH_DOWN -> {
                blockEntity.nudgePitch(-PhiArtilleryBlockEntity.YAW_STEP);
                yield true;
            }
            case BUTTON_FIRE -> {
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    yield false;
                }
                PhiArtilleryBlockEntity.FireResult result = blockEntity.tryFirePulse();
                yield switch (result) {
                    case OK -> true;
                    case NOT_FORMED -> {
                        serverPlayer.displayClientMessage(
                                Component.translatable("message.effecoria.phi_artillery.need_lens"), true);
                        yield false;
                    }
                    case OVERHEAT -> {
                        serverPlayer.displayClientMessage(
                                Component.translatable("message.effecoria.phi_artillery.overheat"), true);
                        yield false;
                    }
                    case LOW_FACTOR -> {
                        serverPlayer.displayClientMessage(
                                Component.translatable(
                                        "message.effecoria.phi_artillery.low_factor",
                                        String.format("%.1f", blockEntity.powerFactorNow()),
                                        String.format("%.1f", PhiArtilleryBlockEntity.MIN_FACTOR)),
                                true);
                        yield false;
                    }
                    case NO_FUEL -> {
                        serverPlayer.displayClientMessage(
                                Component.translatable("message.effecoria.phi_artillery.no_fuel"), true);
                        yield false;
                    }
                    case FAILED -> false;
                };
            }
            case BUTTON_HOLD -> {
                blockEntity.toggleHold();
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
