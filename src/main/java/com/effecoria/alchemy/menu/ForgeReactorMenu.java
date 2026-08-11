package com.effecoria.alchemy.menu;

import com.effecoria.block.ForgeReactorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModMenus;
import com.effecoria.core.alchemy.ForgeRecipes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Forge GUI layout (220×166): left forge bay (fuel / catalyst / IO), right reactor controls.
 * Player inventory offset x=29 to center under the wider panel.
 */
public final class ForgeReactorMenu extends MachineMenu {
    public static final int BUTTON_TOGGLE = 0;
    public static final int BUTTON_MODE = 1;
    public static final int BUTTON_SCRAM = 2;

    public static final int PANEL_WIDTH = 220;
    public static final int PLAYER_INV_X = 29;
    public static final int PLAYER_INV_Y = 84;

    private final ForgeReactorBlockEntity blockEntity;
    private final ContainerData data;

    public ForgeReactorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(ForgeReactorBlockEntity.DATA_COUNT));
    }

    public ForgeReactorMenu(int id, Inventory playerInv, ForgeReactorBlockEntity be, ContainerData data) {
        super(
                ModMenus.FORGE_REACTOR.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.FORGE_REACTOR_CORE.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, ForgeReactorBlockEntity.SLOT_COUNT);

        // Left bay — fuel ×2, catalyst
        addSlot(new Slot(be, ForgeReactorBlockEntity.SLOT_FUEL_1, 17, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeReactorBlockEntity.isValidFuel(stack);
            }
        });
        addSlot(new Slot(be, ForgeReactorBlockEntity.SLOT_FUEL_2, 37, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeReactorBlockEntity.isValidFuel(stack);
            }
        });
        addSlot(new Slot(be, ForgeReactorBlockEntity.SLOT_CATALYST, 65, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ForgeReactorBlockEntity.isValidCatalyst(stack);
            }
        });
        // Left bay — forge IO
        addSlot(new Slot(be, ForgeReactorBlockEntity.SLOT_IN_1, 21, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });
        addSlot(new Slot(be, ForgeReactorBlockEntity.SLOT_IN_2, 41, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });
        addSlot(new Slot(be, ForgeReactorBlockEntity.SLOT_OUT, 83, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInv, PLAYER_INV_X, PLAYER_INV_Y);
        addDataSlots(data);
    }

    private static ForgeReactorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ForgeReactorBlockEntity reactor) {
            return reactor;
        }
        throw new IllegalStateException("Forge reactor block entity missing at client open");
    }

    public boolean formed() {
        return data.get(ForgeReactorBlockEntity.DATA_FORMED) != 0;
    }

    public boolean running() {
        return data.get(ForgeReactorBlockEntity.DATA_RUNNING) != 0;
    }

    public ForgeRecipes.Mode mode() {
        int i = data.get(ForgeReactorBlockEntity.DATA_MODE);
        ForgeRecipes.Mode[] values = ForgeRecipes.Mode.values();
        return values[Math.max(0, Math.min(values.length - 1, i))];
    }

    public int powerPercent() {
        return data.get(ForgeReactorBlockEntity.DATA_POWER);
    }

    public int temperature() {
        return data.get(ForgeReactorBlockEntity.DATA_TEMP);
    }

    public int omegaPercent() {
        return data.get(ForgeReactorBlockEntity.DATA_OMEGA);
    }

    public boolean cooled() {
        return data.get(ForgeReactorBlockEntity.DATA_COOLED) != 0;
    }

    public int fuelTicks() {
        return data.get(ForgeReactorBlockEntity.DATA_FUEL);
    }

    public int fuelMax() {
        return Math.max(1, data.get(ForgeReactorBlockEntity.DATA_FUEL_MAX));
    }

    public float progressRatio() {
        return data.get(ForgeReactorBlockEntity.DATA_PROGRESS)
                / (float) Math.max(1, data.get(ForgeReactorBlockEntity.DATA_PROGRESS_MAX));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && !com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                        serverPlayer, com.effecoria.core.technomagic.TechnomagicEra.IV)) {
            return false;
        }
        return switch (id) {
            case BUTTON_TOGGLE -> {
                if (!formed()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("gui.effecoria.forge_reactor.not_formed"),
                            true);
                    yield false;
                }
                blockEntity.toggleRunning();
                yield true;
            }
            case BUTTON_MODE -> {
                blockEntity.cycleMode();
                yield true;
            }
            case BUTTON_SCRAM -> {
                blockEntity.emergencyScram();
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = ForgeReactorBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ForgeReactorBlockEntity.isValidFuel(stack)) {
                if (!moveItemStackTo(stack, ForgeReactorBlockEntity.SLOT_FUEL_1, ForgeReactorBlockEntity.SLOT_FUEL_2 + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (ForgeReactorBlockEntity.isValidCatalyst(stack)) {
                if (!moveItemStackTo(
                        stack, ForgeReactorBlockEntity.SLOT_CATALYST, ForgeReactorBlockEntity.SLOT_CATALYST + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, ForgeReactorBlockEntity.SLOT_IN_1, ForgeReactorBlockEntity.SLOT_IN_2 + 1, false)) {
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
