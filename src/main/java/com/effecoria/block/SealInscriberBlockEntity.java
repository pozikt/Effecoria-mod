package com.effecoria.block;

import com.effecoria.alchemy.menu.SealInscriberMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.artifact.AssembledGearData;
import com.effecoria.core.artifact.ItemSealCatalog;
import com.effecoria.core.artifact.ItemSealDefinition;
import com.effecoria.core.artifact.StaffStats;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SealInscriberBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_TARGET = 0;
    public static final int SLOT_COUNT = 1;
    public static final int DATA_SEAL = 0;
    public static final int DATA_LEVEL = 1;
    public static final int DATA_COUNT = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int sealIndex;
    private int sealLevel = 1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_SEAL -> sealIndex;
                case DATA_LEVEL -> sealLevel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_SEAL -> sealIndex = Math.max(0, value);
                case DATA_LEVEL -> sealLevel = Math.max(1, value);
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SealInscriberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEAL_INSCRIBER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public void setSealIndex(int index) {
        sealIndex = Math.max(0, index);
        setChanged();
    }

    public void setSealLevel(int level) {
        sealLevel = Math.max(1, level);
        setChanged();
    }

    public boolean tryInscribe(Player player) {
        ItemStack target = items.get(SLOT_TARGET);
        if (target.isEmpty()) {
            return false;
        }
        PlayerPsiData psi = PsiHelper.get(player);
        if (psi.school() != MagicSchool.SEALS && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.effecoria.item_seal_need_seals"), true);
            return false;
        }
        List<ResourceLocation> known = new ArrayList<>(psi.knownItemSeals());
        if (known.isEmpty()) {
            known.addAll(ItemSealCatalog.starterIds());
        }
        if (known.isEmpty()) {
            return false;
        }
        sealIndex = Math.floorMod(sealIndex, known.size());
        ResourceLocation sealId = known.get(sealIndex);
        ItemSealDefinition def = ItemSealCatalog.get(sealId).orElse(null);
        if (def == null || !ItemSealCatalog.appliesTo(def, target)) {
            player.displayClientMessage(Component.translatable("message.effecoria.item_seal_incompatible"), true);
            return false;
        }
        if (!player.getAbilities().instabuild && !psi.knowsItemSeal(sealId)) {
            player.displayClientMessage(Component.translatable("message.effecoria.item_seal_unknown"), true);
            return false;
        }
        int level = Math.min(def.maxLevel(), Math.max(1, sealLevel));
        Map<ResourceLocation, Integer> seals = new LinkedHashMap<>(AssembledGearData.seals(target));
        int capacity = AssembledGearData.isStaff(target)
                ? StaffStats.of(target).sealCapacity()
                : 1 + AssembledGearData.sealLevel(
                        target, ResourceLocation.fromNamespaceAndPath("effecoria", "ward_bind"));
        if (!seals.containsKey(sealId) && seals.size() >= capacity) {
            player.displayClientMessage(Component.translatable("message.effecoria.item_seal_full"), true);
            return false;
        }
        seals.put(sealId, level);
        ensureGearRoot(target);
        AssembledGearData.setSeals(target, seals);
        if (this.level != null) {
            this.level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.1f);
        }
        setChanged();
        return true;
    }

    public boolean tryStrip(Player player) {
        ItemStack target = items.get(SLOT_TARGET);
        if (target.isEmpty()) {
            return false;
        }
        AssembledGearData.clearSeals(target);
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 1f);
        }
        setChanged();
        return true;
    }

    private static void ensureGearRoot(ItemStack stack) {
        if (AssembledGearData.template(stack).isEmpty()) {
            AssembledGearData.setSeals(stack, Map.of());
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.seal_inscriber");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new SealInscriberMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {SLOT_TARGET};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("SealIndex", sealIndex);
        tag.putInt("SealLevel", sealLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        sealIndex = tag.getInt("SealIndex");
        sealLevel = Math.max(1, tag.getInt("SealLevel"));
    }
}
