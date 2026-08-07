package com.effecoria.alchemy.menu;

import com.effecoria.block.EssenceBurnerBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItemTags;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModMenus;
import com.effecoria.core.alchemy.HeatLevel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BurnerMenu extends MachineMenu {
    public static final int BUTTON_TEMP_LOW = 0;
    public static final int BUTTON_TEMP_MED = 1;
    public static final int BUTTON_TEMP_HIGH = 2;

    private final EssenceBurnerBlockEntity blockEntity;
    private final ContainerData data;

    public BurnerMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBlockEntity(playerInv, buf), new SimpleContainerData(EssenceBurnerBlockEntity.DATA_COUNT));
    }

    public BurnerMenu(int id, Inventory playerInv, EssenceBurnerBlockEntity be, ContainerData data) {
        super(ModMenus.BURNER.get(), id, ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), ModBlocks.ESSENCE_BURNER.get());
        this.blockEntity = be;
        this.data = data;
        checkContainerSize(be, EssenceBurnerBlockEntity.SLOT_COUNT);
        addSlot(new Slot(be, EssenceBurnerBlockEntity.SLOT_FUEL, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ESSENITE_DUST.get());
            }
        });
        addSlot(new Slot(be, EssenceBurnerBlockEntity.SLOT_CATALYST, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItemTags.BURNER_CATALYSTS);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static EssenceBurnerBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof EssenceBurnerBlockEntity burner) {
            return burner;
        }
        throw new IllegalStateException("Burner block entity missing at client open");
    }

    public EssenceBurnerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int fuelTicks() {
        return data.get(EssenceBurnerBlockEntity.DATA_FUEL);
    }

    public HeatLevel selectedTemp() {
        return HeatLevel.byId(data.get(EssenceBurnerBlockEntity.DATA_TEMP));
    }

    public int overheatCooldown() {
        return data.get(EssenceBurnerBlockEntity.DATA_OVERHEAT);
    }

    public boolean lit() {
        return data.get(EssenceBurnerBlockEntity.DATA_LIT) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        HeatLevel temp = switch (id) {
            case BUTTON_TEMP_LOW -> HeatLevel.LOW;
            case BUTTON_TEMP_MED -> HeatLevel.MEDIUM;
            case BUTTON_TEMP_HIGH -> HeatLevel.HIGH;
            default -> null;
        };
        if (temp == null) {
            return false;
        }
        blockEntity.setSelectedTemp(temp);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int machineSlots = EssenceBurnerBlockEntity.SLOT_COUNT;
            if (index < machineSlots) {
                if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItems.ESSENITE_DUST.get())) {
                if (!moveItemStackTo(stack, EssenceBurnerBlockEntity.SLOT_FUEL, EssenceBurnerBlockEntity.SLOT_FUEL + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(ModItemTags.BURNER_CATALYSTS)) {
                if (!moveItemStackTo(
                        stack, EssenceBurnerBlockEntity.SLOT_CATALYST, EssenceBurnerBlockEntity.SLOT_CATALYST + 1, false)) {
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
