package com.effecoria.block;

import com.effecoria.alchemy.menu.OmegaDamperMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.OmegaRodItem;
import com.effecoria.core.glue.EssenceGlueData;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrubs Ω from the facility Ψ-anchor and Forge cores into {@link OmegaRodItem} sockets.
 */
public final class OmegaDamperBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_COUNT = 3;
    /** Scrub pulse period (~1s). */
    public static final int SCRUB_INTERVAL = 20;
    /**
     * Ω-centis moved per pulse, split across absorbing rods.
     * With {@link OmegaRodItem#MAX_SATURATION}=2000 → ~8+ min continuous fill per rod
     * (was 40/s into 1000 → ~25s; too fast for a replaceable filter).
     */
    public static final int SCRUB_PER_TICK = 4;

    public static final int DATA_SCRUBBING = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_TOWER_OMEGA = 2;
    public static final int DATA_FORGE_OMEGA = 3;
    public static final int DATA_COUNT = 4;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NEED_RODS = 1;
    public static final int STATUS_SCRUBBING = 2;
    public static final int STATUS_SATURATED = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int status = STATUS_IDLE;
    private boolean scrubbing;
    private int syncedTowerOmega;
    private int syncedForgeOmega;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_SCRUBBING -> scrubbing ? 1 : 0;
                case DATA_STATUS -> status;
                case DATA_TOWER_OMEGA -> syncedTowerOmega;
                case DATA_FORGE_OMEGA -> syncedForgeOmega;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_SCRUBBING -> scrubbing = value != 0;
                case DATA_STATUS -> status = value;
                case DATA_TOWER_OMEGA -> syncedTowerOmega = value;
                case DATA_FORGE_OMEGA -> syncedForgeOmega = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public OmegaDamperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMEGA_DAMPER.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public int statusCode() {
        return status;
    }

    public boolean scrubbing() {
        return scrubbing;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OmegaDamperBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % SCRUB_INTERVAL != 0L) {
            return;
        }
        be.tickScrub(server, pos, state);
    }

    private void tickScrub(ServerLevel server, BlockPos pos, BlockState state) {
        List<ItemStack> rods = absorbingRods();
        boolean hasAnyRod = false;
        boolean allSaturated = true;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            if (OmegaRodItem.isOmegaRod(stack)) {
                hasAnyRod = true;
                if (!OmegaRodItem.isSaturated(stack)) {
                    allSaturated = false;
                }
            }
        }

        int available = availableOmega(server, pos);
        boolean didScrub = false;
        if (!rods.isEmpty() && available > 0) {
            int budget = Math.min(SCRUB_PER_TICK, available);
            int perRod = Math.max(1, budget / rods.size());
            int remaining = budget;
            for (ItemStack rod : rods) {
                if (remaining <= 0) {
                    break;
                }
                int want = Math.min(perRod, remaining);
                int taken = pullOmega(server, pos, want);
                if (taken <= 0) {
                    break;
                }
                int absorbed = OmegaRodItem.absorb(rod, taken);
                remaining -= absorbed;
                if (absorbed > 0) {
                    didScrub = true;
                }
            }
            if (didScrub) {
                setChanged();
            }
        }

        int nextStatus;
        if (!hasAnyRod) {
            nextStatus = STATUS_NEED_RODS;
        } else if (allSaturated) {
            nextStatus = STATUS_SATURATED;
        } else if (didScrub) {
            nextStatus = STATUS_SCRUBBING;
        } else {
            nextStatus = STATUS_IDLE;
        }
        scrubbing = didScrub;
        status = nextStatus;
        syncedTowerOmega = towerOmegaPercent(server, pos);
        syncedForgeOmega = forgeOmegaPercent(server, pos);
        boolean lit = didScrub;
        if (state.getValue(OmegaDamperBlock.LIT) != lit) {
            server.setBlock(pos, state.setValue(OmegaDamperBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private List<ItemStack> absorbingRods() {
        List<ItemStack> rods = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            if (OmegaRodItem.canAbsorb(stack)) {
                rods.add(stack);
            }
        }
        return rods;
    }

    private int towerOmegaPercent(ServerLevel server, BlockPos pos) {
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(server, pos).orElse(null);
        return computer == null ? 0 : computer.omegaPercent();
    }

    private int forgeOmegaPercent(ServerLevel server, BlockPos pos) {
        int max = 0;
        for (BlockPos member : EssenceGlueData.get(server).component(pos)) {
            if (server.getBlockEntity(member) instanceof ForgeReactorBlockEntity forge) {
                max = Math.max(max, forge.omegaPercent());
            }
        }
        return max;
    }

    private int availableOmega(ServerLevel server, BlockPos pos) {
        int total = 0;
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(server, pos).orElse(null);
        if (computer != null) {
            total += computer.omegaCentis();
        }
        for (BlockPos member : EssenceGlueData.get(server).component(pos)) {
            if (server.getBlockEntity(member) instanceof ForgeReactorBlockEntity forge) {
                total += forge.omegaCentis();
            }
        }
        return total;
    }

    /** Pull with anchor priority, then forge cores. Returns centis actually removed. */
    private int pullOmega(ServerLevel server, BlockPos pos, int want) {
        int left = want;
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(server, pos).orElse(null);
        if (computer != null && left > 0) {
            left -= computer.reduceOmega(left);
        }
        if (left > 0) {
            for (BlockPos member : EssenceGlueData.get(server).component(pos)) {
                if (left <= 0) {
                    break;
                }
                if (server.getBlockEntity(member) instanceof ForgeReactorBlockEntity forge) {
                    left -= forge.reduceOmega(left);
                }
            }
        }
        return want - left;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.omega_damper");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new OmegaDamperMenu(id, inv, this, data);
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
        return OmegaRodItem.isOmegaRod(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Status", status);
        tag.putBoolean("Scrubbing", scrubbing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, provider);
        status = tag.getInt("Status");
        scrubbing = tag.getBoolean("Scrubbing");
    }
}
