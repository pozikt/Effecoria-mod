package com.effecoria.alchemy.menu;

import com.effecoria.block.ClimateArrayBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.effecoria.world.weather.PhiWeatherKind;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClimateArrayMenu extends MachineMenu {
    public static final int BUTTON_CYCLE = 0;
    public static final int BUTTON_ACTIVATE = 1;

    private static final PhiWeatherKind[] MODES = {
        PhiWeatherKind.ESSENCE_DEW, PhiWeatherKind.ESSENCE_MIST, PhiWeatherKind.ESSENCE_RAIN
    };

    private final ClimateArrayBlockEntity blockEntity;
    private final ContainerData data;

    public ClimateArrayMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(
                id,
                playerInv,
                getBlockEntity(playerInv, buf),
                new SimpleContainerData(ClimateArrayBlockEntity.DATA_COUNT));
    }

    public ClimateArrayMenu(int id, Inventory playerInv, ClimateArrayBlockEntity be, ContainerData data) {
        super(
                ModMenus.CLIMATE_ARRAY.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.CLIMATE_ARRAY.get());
        this.blockEntity = be;
        this.data = data;
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static ClimateArrayBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ClimateArrayBlockEntity array) {
            return array;
        }
        throw new IllegalStateException("Climate array block entity missing at client open");
    }

    public int modeIndex() {
        return data.get(ClimateArrayBlockEntity.DATA_MODE);
    }

    public PhiWeatherKind mode() {
        return MODES[Mth.clamp(modeIndex(), 0, MODES.length - 1)];
    }

    public int cooldown() {
        return data.get(ClimateArrayBlockEntity.DATA_COOLDOWN);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof ServerPlayer serverPlayer
                && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.V)) {
            return false;
        }
        return switch (id) {
            case BUTTON_CYCLE -> {
                blockEntity.cycleMode();
                yield true;
            }
            case BUTTON_ACTIVATE -> blockEntity.tryActivate(player);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
