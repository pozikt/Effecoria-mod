package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.tower.FacilityNames;
import com.effecoria.core.tower.NamedFacilityDevice;
import com.effecoria.core.tower.PhiSonarService;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Cooldown + last Φ-sonar sweep (for console / cartography / goggles). */
public final class PhiSonarBlockEntity extends BlockEntity implements NamedFacilityDevice {
    public static final int SCAN_COOLDOWN_TICKS = 160; // 8s

    private int cooldownTicks;
    private long lastScanGameTime = -1L;
    private String facilityName = "";

    private int lastOriginX;
    private int lastOriginY;
    private int lastOriginZ;
    private int lastRadius;
    private int lastStep;
    private int lastWidth;
    private int lastModeId;
    @Nullable
    private byte[] lastHeights;
    @Nullable
    private byte[] lastTerrain;
    private List<PhiSonarService.Blip> lastBlips = List.of();

    public PhiSonarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_SONAR.get(), pos, state);
    }

    @Override
    public String facilityName() {
        return facilityName;
    }

    @Override
    public boolean setFacilityName(String name) {
        String next = FacilityNames.sanitize(name);
        if (next.equals(facilityName)) {
            return true;
        }
        facilityName = next;
        FacilityNames.markNamed(this);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiSonarBlockEntity be) {
        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
            if (be.cooldownTicks == 0) {
                be.setChanged();
            }
        }
    }

    public boolean ready() {
        return cooldownTicks <= 0;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public long lastScanGameTime() {
        return lastScanGameTime;
    }

    public boolean hasStoredScan() {
        return lastHeights != null
                && lastTerrain != null
                && lastWidth > 0
                && lastHeights.length == lastWidth * lastWidth
                && lastTerrain.length == lastWidth * lastWidth;
    }

    /** Marks a successful scan and starts the cooldown. */
    public void markScanned(long gameTime) {
        markScanned(gameTime, SCAN_COOLDOWN_TICKS);
    }

    /** Marks a successful scan with a mode-specific cooldown. */
    public void markScanned(long gameTime, int cooldown) {
        lastScanGameTime = gameTime;
        cooldownTicks = Math.max(1, cooldown);
        setChanged();
    }

    public void storeScan(PhiSonarService.ScanResult result) {
        lastOriginX = result.originX();
        lastOriginY = result.originY();
        lastOriginZ = result.originZ();
        lastRadius = result.radius();
        lastStep = result.step();
        lastWidth = result.width();
        lastModeId = result.modeId();
        lastHeights = result.heights().clone();
        lastTerrain = result.terrain().clone();
        lastBlips = List.copyOf(result.blips());
        setChanged();
    }

    @Nullable
    public ModNetworking.PhiSonarMapPayload toMapPayload() {
        if (!hasStoredScan()) {
            return null;
        }
        return new ModNetworking.PhiSonarMapPayload(
                lastOriginX,
                lastOriginY,
                lastOriginZ,
                lastRadius,
                lastStep,
                lastWidth,
                lastModeId,
                lastHeights.clone(),
                lastTerrain.clone(),
                lastBlips);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cooldownTicks);
        tag.putLong("LastScan", lastScanGameTime);
        if (hasStoredScan()) {
            tag.putInt("ScanOx", lastOriginX);
            tag.putInt("ScanOy", lastOriginY);
            tag.putInt("ScanOz", lastOriginZ);
            tag.putInt("ScanR", lastRadius);
            tag.putInt("ScanStep", lastStep);
            tag.putInt("ScanW", lastWidth);
            tag.putInt("ScanMode", lastModeId);
            tag.putByteArray("ScanH", lastHeights);
            tag.putByteArray("ScanT", lastTerrain);
            ListTag blipList = new ListTag();
            for (PhiSonarService.Blip blip : lastBlips) {
                CompoundTag b = new CompoundTag();
                b.putShort("X", blip.relX());
                b.putShort("Z", blip.relZ());
                b.putByte("K", blip.kind());
                blipList.add(b);
            }
            tag.put("ScanBlips", blipList);
        }
        FacilityNames.save(tag, facilityName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldownTicks = Math.max(0, tag.getInt("Cooldown"));
        lastScanGameTime = tag.contains("LastScan") ? tag.getLong("LastScan") : -1L;
        if (tag.contains("ScanH") && tag.contains("ScanT")) {
            lastOriginX = tag.getInt("ScanOx");
            lastOriginY = tag.getInt("ScanOy");
            lastOriginZ = tag.getInt("ScanOz");
            lastRadius = tag.getInt("ScanR");
            lastStep = tag.getInt("ScanStep");
            lastWidth = tag.getInt("ScanW");
            lastModeId = tag.getInt("ScanMode");
            lastHeights = tag.getByteArray("ScanH");
            lastTerrain = tag.getByteArray("ScanT");
            List<PhiSonarService.Blip> blips = new ArrayList<>();
            ListTag list = tag.getList("ScanBlips", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag b = list.getCompound(i);
                blips.add(new PhiSonarService.Blip(b.getShort("X"), b.getShort("Z"), b.getByte("K")));
            }
            lastBlips = List.copyOf(blips);
        } else {
            lastHeights = null;
            lastTerrain = null;
            lastBlips = List.of();
            lastWidth = 0;
        }
        facilityName = FacilityNames.load(tag);
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
