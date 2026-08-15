package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.tower.PhoenixShedService;
import com.effecoria.core.tower.TowerBodyType;
import com.effecoria.core.tower.TowerDomeService;
import com.effecoria.core.tower.TowerStructureValidator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Ψ-computer heart of a Mage Tower — consecrate, soulbind, revive chamber, Φ-dome. */
public final class TowerAnchorBlockEntity extends BlockEntity {
    public static final int INV_SIZE = 9;
    public static final int OMEGA_MAX = 10000;

    private boolean consecrated;
    private double verticality = 1.0;
    private double phiScatter = 1.0;
    private String reactorClass = "none";
    private int gluedCells;
    private int presentBlocks;
    private double integrity;
    private AABB structureBounds = new AABB(0, 0, 0, 1, 1, 1);

    @Nullable
    private UUID ownerUuid;
    private boolean bound;
    private int omegaCentis;
    private int reviveCount;
    private TowerBodyType bodyType = TowerBodyType.BASIC;

    private boolean domeCombat;
    /** Last successful Φ drain for the dome (transient runtime). */
    private boolean domePowered;
    /** Avoid spamming clear packets while passive. */
    private boolean clientDomeSynced;

    /** Pre-Phoenix contactor CLOSED snapshot; cleared on restore. */
    @Nullable
    private ListTag phoenixContactorSnapshot;

    /** Lex Loci hardware Phoenix edict — shed non-life contactors on owner death. */
    private boolean phoenixEdictEnabled = true;

    /** Empty = built-in Phoenix word program. */
    private final List<String> lociTokens = new ArrayList<>();

    /** Soft re-shed interval used by {@link PhiWatchdogBlockEntity} (~2s). */
    public static final int PHOENIX_WATCHDOG_INTERVAL = 40;

    private final NonNullList<ItemStack> items = NonNullList.withSize(INV_SIZE, ItemStack.EMPTY);

    public TowerAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER_ANCHOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TowerAnchorBlockEntity be) {
        TowerDomeService.serverTick(level, pos, be);
    }

    public boolean consecrated() {
        return consecrated;
    }

    public boolean bound() {
        return bound;
    }

    @Nullable
    public UUID ownerUuid() {
        return ownerUuid;
    }

    public int omegaCentis() {
        return omegaCentis;
    }

    public int omegaPercent() {
        return Math.min(100, omegaCentis / 100);
    }

    public int reviveCount() {
        return reviveCount;
    }

    public TowerBodyType bodyType() {
        return bodyType;
    }

    public double phiScatter() {
        return phiScatter;
    }

    public double verticality() {
        return verticality;
    }

    public double integrity() {
        return integrity;
    }

    public int gluedCells() {
        return gluedCells;
    }

    public int presentBlocks() {
        return presentBlocks;
    }

    public String reactorClass() {
        return reactorClass;
    }

    public AABB structureBounds() {
        return structureBounds;
    }

    public boolean domeCombat() {
        return domeCombat;
    }

    public void setDomeCombat(boolean value) {
        if (domeCombat == value) {
            return;
        }
        domeCombat = value;
        setChanged();
        sync();
    }

    public boolean toggleDomeCombat() {
        setDomeCombat(!domeCombat);
        return domeCombat;
    }

    public boolean domePowered() {
        return domePowered;
    }

    public void setDomePowered(boolean value) {
        domePowered = value;
    }

    public boolean clientDomeSynced() {
        return clientDomeSynced;
    }

    public void setClientDomeSynced(boolean value) {
        clientDomeSynced = value;
    }

    public void cycleBodyType() {
        bodyType = bodyType.next();
        setChanged();
        sync();
    }

    public void setBodyType(TowerBodyType type) {
        bodyType = type == null ? TowerBodyType.BASIC : type;
        setChanged();
        sync();
    }

    public boolean applyConsecration(TowerStructureValidator.Result result) {
        if (!result.ok() || result.report() == null) {
            return false;
        }
        consecrated = true;
        verticality = result.verticality();
        phiScatter = result.phiScatter();
        reactorClass = result.reactorClass().name().toLowerCase();
        gluedCells = result.report().gluedCells();
        presentBlocks = result.report().presentBlocks();
        integrity = result.report().integrity();
        structureBounds = result.report().bounds();
        setChanged();
        sync();
        return true;
    }

    public void refreshIntegrity(ServerLevel level) {
        var report = com.effecoria.core.glue.EssenceGlueStructure.inspect(level, worldPosition);
        gluedCells = report.gluedCells();
        presentBlocks = report.presentBlocks();
        integrity = report.integrity();
        structureBounds = report.bounds();
        setChanged();
    }

    public void bindOwner(UUID uuid) {
        ownerUuid = uuid;
        bound = true;
        setChanged();
        sync();
    }

    public void unbind() {
        ownerUuid = null;
        bound = false;
        domeCombat = false;
        domePowered = false;
        phoenixContactorSnapshot = null;
        phoenixEdictEnabled = true;
        lociTokens.clear();
        setChanged();
        sync();
    }

    public List<String> lociTokens() {
        return Collections.unmodifiableList(lociTokens);
    }

    public void setLociTokens(List<String> tokens) {
        lociTokens.clear();
        if (tokens != null) {
            for (String token : tokens) {
                if (token != null && !token.isEmpty()) {
                    lociTokens.add(token);
                }
            }
        }
        setChanged();
        sync();
    }

    public boolean phoenixEdictEnabled() {
        return phoenixEdictEnabled;
    }

    public void setPhoenixEdictEnabled(boolean enabled) {
        if (phoenixEdictEnabled == enabled) {
            return;
        }
        phoenixEdictEnabled = enabled;
        if (!enabled && level instanceof ServerLevel server) {
            PhoenixShedService.clearSignalsIfDisarmed(server, worldPosition);
        }
        setChanged();
        sync();
    }

    public boolean togglePhoenixEdict() {
        setPhoenixEdictEnabled(!phoenixEdictEnabled);
        return phoenixEdictEnabled;
    }

    public boolean hasPhoenixSnapshot() {
        return phoenixContactorSnapshot != null && !phoenixContactorSnapshot.isEmpty();
    }

    public void storePhoenixSnapshot(ListTag snapshot) {
        phoenixContactorSnapshot = snapshot == null || snapshot.isEmpty() ? null : snapshot.copy();
        setChanged();
    }

    /** Returns and clears the stored snapshot (may be empty). */
    @Nullable
    public ListTag takePhoenixSnapshot() {
        ListTag snap = phoenixContactorSnapshot;
        phoenixContactorSnapshot = null;
        setChanged();
        return snap;
    }

    public boolean clearOmega() {
        if (omegaCentis <= 0) {
            return false;
        }
        omegaCentis = 0;
        setChanged();
        sync();
        return true;
    }

    public void addOmegaPercent(int percent) {
        if (percent <= 0) {
            return;
        }
        int scaled = (int) Math.round(percent * 100 * Math.max(0.35, phiScatter));
        omegaCentis = Math.min(OMEGA_MAX, omegaCentis + scaled);
        setChanged();
        sync();
    }

    public void onRevive() {
        reviveCount++;
        setChanged();
        sync();
    }

    public boolean tryInsert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int before = stack.getCount();
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
                setChanged();
                return true;
            }
            if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                stack.shrink(move);
                setChanged();
            }
        }
        return stack.getCount() < before;
    }

    /** Consume count of item from chamber; returns true if fully paid. */
    public boolean consume(Item item, int count) {
        int need = count;
        for (int i = 0; i < items.size() && need > 0; i++) {
            ItemStack slot = items.get(i);
            if (slot.is(item)) {
                int take = Math.min(need, slot.getCount());
                slot.shrink(take);
                need -= take;
                if (slot.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
            }
        }
        if (need == 0) {
            setChanged();
            return true;
        }
        return false;
    }

    public boolean has(Item item, int count) {
        int found = 0;
        for (ItemStack slot : items) {
            if (slot.is(item)) {
                found += slot.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean payBodyCosts(TowerBodyType type) {
        return switch (type) {
            case BASIC -> true;
            case ENHANCED -> has(ModItems.PHI_BONE_PASTE.get(), 4) && consume(ModItems.PHI_BONE_PASTE.get(), 4);
            case COMBAT -> has(ModItems.PHI_STEEL_INGOT.get(), 2)
                    && has(ModItems.PHI_BONE_PASTE.get(), 8)
                    && consume(ModItems.PHI_STEEL_INGOT.get(), 2)
                    && consume(ModItems.PHI_BONE_PASTE.get(), 8);
            case ARCANE -> has(ModItems.PURE_ESSONITE.get(), 1)
                    && has(ModItems.PHI_NECTAR.get(), 1)
                    && consume(ModItems.PURE_ESSONITE.get(), 1)
                    && consume(ModItems.PHI_NECTAR.get(), 1);
        };
    }

    public boolean consumeSoulShardForXp() {
        if (has(ModItems.SOUL_SHARD.get(), 1)) {
            return consume(ModItems.SOUL_SHARD.get(), 1);
        }
        return false;
    }

    public Component statusLine() {
        int pct = (int) Math.round(integrity * 100.0);
        return Component.translatable(
                "message.effecoria.tower.status",
                consecrated ? 1 : 0,
                gluedCells,
                pct,
                String.format("%.2f", verticality),
                String.format("%.2f", phiScatter),
                bodyType.getSerializedName(),
                omegaPercent(),
                reviveCount,
                bound ? 1 : 0,
                domePowered ? 1 : 0,
                domeCombat ? 1 : 0);
    }

    public BlockPos revivePos() {
        return worldPosition.above();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Consecrated", consecrated);
        tag.putDouble("Verticality", verticality);
        tag.putDouble("PhiScatter", phiScatter);
        tag.putString("ReactorClass", reactorClass);
        tag.putInt("GluedCells", gluedCells);
        tag.putInt("PresentBlocks", presentBlocks);
        tag.putDouble("Integrity", integrity);
        tag.putDouble("BoundMinX", structureBounds.minX);
        tag.putDouble("BoundMinY", structureBounds.minY);
        tag.putDouble("BoundMinZ", structureBounds.minZ);
        tag.putDouble("BoundMaxX", structureBounds.maxX);
        tag.putDouble("BoundMaxY", structureBounds.maxY);
        tag.putDouble("BoundMaxZ", structureBounds.maxZ);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putBoolean("Bound", bound);
        tag.putInt("Omega", omegaCentis);
        tag.putInt("ReviveCount", reviveCount);
        tag.putString("BodyType", bodyType.getSerializedName());
        tag.putBoolean("DomeCombat", domeCombat);
        tag.putBoolean("PhoenixEdict", phoenixEdictEnabled);
        if (!lociTokens.isEmpty()) {
            ListTag tokens = new ListTag();
            for (String token : lociTokens) {
                tokens.add(StringTag.valueOf(token));
            }
            tag.put("LociTokens", tokens);
        }
        if (phoenixContactorSnapshot != null && !phoenixContactorSnapshot.isEmpty()) {
            tag.put("PhoenixContactors", phoenixContactorSnapshot.copy());
        }
        ContainerHelper.saveAllItems(tag, items, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        consecrated = tag.getBoolean("Consecrated");
        verticality = tag.getDouble("Verticality");
        phiScatter = tag.contains("PhiScatter") ? tag.getDouble("PhiScatter") : 1.0;
        reactorClass = tag.contains("ReactorClass") ? tag.getString("ReactorClass") : "none";
        gluedCells = tag.getInt("GluedCells");
        presentBlocks = tag.getInt("PresentBlocks");
        integrity = tag.getDouble("Integrity");
        if (tag.contains("BoundMinX")) {
            structureBounds = new AABB(
                    tag.getDouble("BoundMinX"),
                    tag.getDouble("BoundMinY"),
                    tag.getDouble("BoundMinZ"),
                    tag.getDouble("BoundMaxX"),
                    tag.getDouble("BoundMaxY"),
                    tag.getDouble("BoundMaxZ"));
        }
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        bound = tag.getBoolean("Bound");
        omegaCentis = tag.getInt("Omega");
        reviveCount = tag.getInt("ReviveCount");
        bodyType = TowerBodyType.fromId(tag.getString("BodyType"));
        domeCombat = tag.getBoolean("DomeCombat");
        phoenixEdictEnabled = !tag.contains("PhoenixEdict") || tag.getBoolean("PhoenixEdict");
        lociTokens.clear();
        if (tag.contains("LociTokens", Tag.TAG_LIST)) {
            ListTag tokens = tag.getList("LociTokens", Tag.TAG_STRING);
            for (int i = 0; i < tokens.size(); i++) {
                String raw = tokens.getString(i);
                if (!raw.isEmpty()) {
                    lociTokens.add(raw);
                }
            }
        }
        if (tag.contains("PhoenixContactors", Tag.TAG_LIST)) {
            phoenixContactorSnapshot = tag.getList("PhoenixContactors", Tag.TAG_COMPOUND).copy();
        } else {
            phoenixContactorSnapshot = null;
        }
        ContainerHelper.loadAllItems(tag, items, provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
