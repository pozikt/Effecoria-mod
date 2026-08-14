package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiIncubatorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.tower.TowerBodyType;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.Optional;

/**
 * Incubates a prepaid tower body from the same materials as
 * {@link TowerAnchorBlockEntity#payBodyCosts}. Target type follows the linked Ψ-anchor.
 */
public final class PhiIncubatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_COUNT = 3;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX = 1;
    public static final int DATA_READY = 2;
    public static final int DATA_TARGET = 3;
    public static final int DATA_POWER = 4;
    public static final int DATA_LINKED = 5;
    public static final int DATA_COUNT = 6;

    public static final int COOK_ENHANCED = 600;
    public static final int COOK_COMBAT = 1200;
    public static final int COOK_ARCANE = 1800;
    /** Nearby Ψ-anchor search when incubator is not Φ-glued to the facility. */
    public static final int LINK_RADIUS = 48;

    private static final int[] ALL_SLOTS = {0, 1, 2};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = COOK_ENHANCED;
    @Nullable
    private TowerBodyType readyBody;
    /** Cached for ContainerData sync (client never resolves glue/anchor itself). */
    private int syncedTargetOrdinal;
    private boolean syncedLinked;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_READY -> readyBody == null ? 0 : readyBody.ordinal() + 1;
                case DATA_TARGET -> syncedTargetOrdinal;
                case DATA_POWER ->
                        level != null && PhiPower.hasPower(level, worldPosition) ? 1 : 0;
                case DATA_LINKED -> syncedLinked ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_MAX -> maxProgress = value;
                case DATA_TARGET -> syncedTargetOrdinal = value;
                case DATA_LINKED -> syncedLinked = value != 0;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_INCUBATOR.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    @Nullable
    public TowerBodyType readyBody() {
        return readyBody;
    }

    public int progress() {
        return progress;
    }

    public boolean isReadyFor(TowerBodyType type) {
        return readyBody != null && readyBody == type && type != TowerBodyType.BASIC;
    }

    public boolean hasTargetMaterials() {
        TowerBodyType target = resolveTarget();
        return target != TowerBodyType.BASIC && hasMaterials(items, target);
    }

    /** Consume a ready incubated body of the given type. */
    public boolean consumeReady(TowerBodyType type) {
        if (!isReadyFor(type)) {
            return false;
        }
        readyBody = null;
        setChanged();
        syncLit(false);
        return true;
    }

    public TowerBodyType resolveTarget() {
        if (level instanceof ServerLevel server) {
            return findLinkedAnchor(server)
                    .map(TowerAnchorBlockEntity::bodyType)
                    .orElse(TowerBodyType.BASIC);
        }
        TowerBodyType[] values = TowerBodyType.values();
        int i = Math.max(0, Math.min(values.length - 1, syncedTargetOrdinal));
        return values[i];
    }

    public boolean isLinked() {
        if (level instanceof ServerLevel server) {
            return findLinkedAnchor(server).isPresent();
        }
        return syncedLinked;
    }

    private Optional<TowerAnchorBlockEntity> findLinkedAnchor(ServerLevel server) {
        return TowerFacility.findLinkedComputer(server, worldPosition, LINK_RADIUS);
    }

    /** Refresh cached target/link for menu sync. */
    public void refreshLinkCache() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Optional<TowerAnchorBlockEntity> anchor = findLinkedAnchor(server);
        syncedLinked = anchor.isPresent();
        syncedTargetOrdinal = anchor.map(a -> a.bodyType().ordinal()).orElse(0);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.phi_incubator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new PhiIncubatorMenu(id, inv, this, data);
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (readyBody != null) {
            return false;
        }
        return isBodyIngredient(stack.getItem());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return readyBody == null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiIncubatorBlockEntity be) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        // Keep menu DATA_TARGET / DATA_LINKED fresh even while idle or ready.
        if (level.getGameTime() % 10L == 0L || be.progress > 0) {
            be.refreshLinkCache();
        }

        if (be.readyBody != null) {
            be.progress = 0;
            be.syncLit(false);
            return;
        }

        TowerBodyType target = be.resolveTarget();
        if (target == TowerBodyType.BASIC || !hasMaterials(be.items, target)) {
            if (be.progress > 0) {
                be.progress = Math.max(0, be.progress - 2);
                be.setChanged();
            }
            be.syncLit(false);
            return;
        }

        if (!PhiPower.consumeTick(level, pos, 1)) {
            if (be.progress > 0) {
                be.progress = Math.max(0, be.progress - 1);
                be.setChanged();
            }
            be.syncLit(false);
            return;
        }

        be.maxProgress = cookTicks(target);
        be.progress++;
        be.syncLit(true);
        if (be.progress >= be.maxProgress) {
            if (consumeMaterials(be.items, target)) {
                be.readyBody = target;
                be.progress = 0;
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7f, 1.15f);
            } else {
                be.progress = 0;
            }
            be.syncLit(false);
        }
        be.setChanged();
    }

    private void syncLit(boolean lit) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(PhiIncubatorBlock.LIT) && state.getValue(PhiIncubatorBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(PhiIncubatorBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    public static int cookTicks(TowerBodyType type) {
        return switch (type) {
            case ENHANCED -> COOK_ENHANCED;
            case COMBAT -> COOK_COMBAT;
            case ARCANE -> COOK_ARCANE;
            default -> COOK_ENHANCED;
        };
    }

    public static boolean isBodyIngredient(Item item) {
        return item == ModItems.PHI_BONE_PASTE.get()
                || item == ModItems.PHI_STEEL_INGOT.get()
                || item == ModItems.PURE_ESSONITE.get()
                || item == ModItems.PHI_NECTAR.get();
    }

    public static boolean hasMaterials(NonNullList<ItemStack> items, TowerBodyType type) {
        return switch (type) {
            case BASIC -> true;
            case ENHANCED -> count(items, ModItems.PHI_BONE_PASTE.get()) >= 4;
            case COMBAT -> count(items, ModItems.PHI_STEEL_INGOT.get()) >= 2
                    && count(items, ModItems.PHI_BONE_PASTE.get()) >= 8;
            case ARCANE -> count(items, ModItems.PURE_ESSONITE.get()) >= 1
                    && count(items, ModItems.PHI_NECTAR.get()) >= 1;
        };
    }

    public static boolean consumeMaterials(NonNullList<ItemStack> items, TowerBodyType type) {
        if (!hasMaterials(items, type)) {
            return false;
        }
        return switch (type) {
            case BASIC -> true;
            case ENHANCED -> take(items, ModItems.PHI_BONE_PASTE.get(), 4);
            case COMBAT -> take(items, ModItems.PHI_STEEL_INGOT.get(), 2)
                    && take(items, ModItems.PHI_BONE_PASTE.get(), 8);
            case ARCANE -> take(items, ModItems.PURE_ESSONITE.get(), 1)
                    && take(items, ModItems.PHI_NECTAR.get(), 1);
        };
    }

    private static int count(NonNullList<ItemStack> items, Item item) {
        int n = 0;
        for (ItemStack stack : items) {
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    private static boolean take(NonNullList<ItemStack> items, Item item, int amount) {
        int left = amount;
        for (int i = 0; i < items.size() && left > 0; i++) {
            ItemStack stack = items.get(i);
            if (!stack.is(item)) {
                continue;
            }
            int remove = Math.min(left, stack.getCount());
            stack.shrink(remove);
            left -= remove;
            if (stack.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
            }
        }
        return left <= 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        if (readyBody != null) {
            tag.putString("ReadyBody", readyBody.getSerializedName());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        progress = tag.getInt("Progress");
        maxProgress = tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : COOK_ENHANCED;
        if (tag.contains("ReadyBody")) {
            TowerBodyType loaded = TowerBodyType.fromId(tag.getString("ReadyBody"));
            readyBody = loaded == TowerBodyType.BASIC ? null : loaded;
        } else {
            readyBody = null;
        }
    }
}
