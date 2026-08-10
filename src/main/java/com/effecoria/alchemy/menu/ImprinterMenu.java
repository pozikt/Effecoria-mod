package com.effecoria.alchemy.menu;

import com.effecoria.block.PsiImprinterBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModMenus;
import com.effecoria.core.technomagic.ImprintData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ImprinterMenu extends MachineMenu {
    public static final int BUTTON_CONSTRUCT = 0;
    public static final int BUTTON_TELEGRAPH = 1;

    private final PsiImprinterBlockEntity blockEntity;
    private final ContainerData data;

    public ImprinterMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(PsiImprinterBlockEntity.DATA_COUNT));
    }

    public ImprinterMenu(int id, Inventory playerInv, PsiImprinterBlockEntity be, ContainerData data) {
        super(
                ModMenus.IMPRINTER.get(),
                id,
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                ModBlocks.PSI_IMPRINTER.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, PsiImprinterBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, PsiImprinterBlockEntity.SLOT_DRIVE, 26, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.PHI_CELL.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(be, PsiImprinterBlockEntity.SLOT_FOCUS, 26, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.RESONANCE_FOCUS.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(be, PsiImprinterBlockEntity.SLOT_BLANK, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ImprintData.isBlankChassis(stack) || ImprintData.isBlankTelegraphModule(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(be, PsiImprinterBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static PsiImprinterBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof PsiImprinterBlockEntity imprinter) {
            return imprinter;
        }
        throw new IllegalStateException("Imprinter block entity missing at client open");
    }

    public int progress() {
        return data.get(PsiImprinterBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(PsiImprinterBlockEntity.DATA_MAX));
    }

    public int mode() {
        return data.get(PsiImprinterBlockEntity.DATA_MODE);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CONSTRUCT) {
            blockEntity.setMode(PsiImprinterBlockEntity.MODE_CONSTRUCT);
            return true;
        }
        if (id == BUTTON_TELEGRAPH) {
            blockEntity.setMode(PsiImprinterBlockEntity.MODE_TELEGRAPH);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = PsiImprinterBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.PHI_CELL.get())) {
                if (!moveItemStackTo(
                        stack, PsiImprinterBlockEntity.SLOT_DRIVE, PsiImprinterBlockEntity.SLOT_DRIVE + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.RESONANCE_FOCUS.get())) {
                if (!moveItemStackTo(
                        stack, PsiImprinterBlockEntity.SLOT_FOCUS, PsiImprinterBlockEntity.SLOT_FOCUS + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (ImprintData.isBlankChassis(stack) || ImprintData.isBlankTelegraphModule(stack)) {
                if (!moveItemStackTo(
                        stack, PsiImprinterBlockEntity.SLOT_BLANK, PsiImprinterBlockEntity.SLOT_BLANK + 1, false)) {
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
