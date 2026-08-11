package com.effecoria.alchemy.menu;

import com.effecoria.alchemy.recipe.CrusherRecipes;
import com.effecoria.block.PhiCrusherBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModMenus;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PhiCrusherMenu extends MachineMenu {
    public static final int BUTTON_MODE = 0;

    private final PhiCrusherBlockEntity blockEntity;
    private final ContainerData data;

    public PhiCrusherMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(PhiCrusherBlockEntity.DATA_COUNT));
    }

    public PhiCrusherMenu(int id, Inventory playerInv, PhiCrusherBlockEntity be, ContainerData data) {
        super(
                ModMenus.PHI_CRUSHER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PHI_CRUSHER.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, PhiCrusherBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, PhiCrusherBlockEntity.SLOT_INPUT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CrusherRecipes.isInput(stack);
            }
        });
        addSlot(new Slot(be, PhiCrusherBlockEntity.SLOT_PRIMARY, 116, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(be, PhiCrusherBlockEntity.SLOT_BYPRODUCT, 134, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(be, PhiCrusherBlockEntity.SLOT_WASTE, 116, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(be, PhiCrusherBlockEntity.SLOT_DRIVE, 26, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PHI_CELL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static PhiCrusherBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PhiCrusherBlockEntity crusher) {
            return crusher;
        }
        throw new IllegalStateException("Phi crusher block entity missing at client open");
    }

    public PhiCrusherBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int progress() {
        return data.get(PhiCrusherBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        int max = data.get(PhiCrusherBlockEntity.DATA_MAX);
        return max <= 0 ? 1 : max;
    }

    public boolean fineMode() {
        return data.get(PhiCrusherBlockEntity.DATA_MODE) == 1;
    }

    public int powerCenti() {
        return data.get(PhiCrusherBlockEntity.DATA_POWER);
    }

    public int heat() {
        return data.get(PhiCrusherBlockEntity.DATA_HEAT);
    }

    public int omega() {
        return data.get(PhiCrusherBlockEntity.DATA_OMEGA);
    }

    public int cooldown() {
        return data.get(PhiCrusherBlockEntity.DATA_COOLDOWN);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_MODE) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && !TechnomagicGates.checkOperate(serverPlayer, TechnomagicEra.III)) {
            return false;
        }
        blockEntity.toggleMode();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = PhiCrusherBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (CrusherRecipes.isInput(stack)) {
                if (!moveItemStackTo(
                        stack, PhiCrusherBlockEntity.SLOT_INPUT, PhiCrusherBlockEntity.SLOT_INPUT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.PHI_CELL.get())) {
                if (!moveItemStackTo(
                        stack, PhiCrusherBlockEntity.SLOT_DRIVE, PhiCrusherBlockEntity.SLOT_DRIVE + 1, false)) {
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
