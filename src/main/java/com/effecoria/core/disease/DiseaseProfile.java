package com.effecoria.core.disease;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;

/** Persistent Φ-disease state on a player (survives death unless clear_on_death). */
public final class DiseaseProfile {
    public static final StreamCodec<RegistryFriendlyByteBuf, DiseaseProfile> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.BOOL.encode(buf, data.clearOnDeath);
                ByteBufCodecs.BOOL.encode(buf, data.orkanumnScar);
                ByteBufCodecs.BOOL.encode(buf, data.crystalFeverImmunity);
                ByteBufCodecs.VAR_INT.encode(buf, data.diseases.size());
                for (Map.Entry<PhiDisease, DiseaseInstance> entry : data.diseases.entrySet()) {
                    ByteBufCodecs.VAR_INT.encode(buf, entry.getKey().ordinal());
                    DiseaseInstance.STREAM_CODEC.encode(buf, entry.getValue());
                }
            },
            buf -> {
                DiseaseProfile data = createDefault();
                data.clearOnDeath = ByteBufCodecs.BOOL.decode(buf);
                data.orkanumnScar = ByteBufCodecs.BOOL.decode(buf);
                data.crystalFeverImmunity = ByteBufCodecs.BOOL.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                PhiDisease[] values = PhiDisease.values();
                for (int i = 0; i < count; i++) {
                    int ordinal = ByteBufCodecs.VAR_INT.decode(buf);
                    DiseaseInstance inst = DiseaseInstance.STREAM_CODEC.decode(buf);
                    if (ordinal >= 0 && ordinal < values.length) {
                        data.diseases.put(values[ordinal], inst);
                    }
                }
                return data;
            });

    private final EnumMap<PhiDisease, DiseaseInstance> diseases = new EnumMap<>(PhiDisease.class);
    private boolean clearOnDeath;
    private boolean orkanumnScar;
    private boolean crystalFeverImmunity;
    /** Exposure counters for slow acquisition (server-only; not synced). */
    private int lowPhiExposure;
    private int highRadExposure;
    private int dustExposure;

    public static DiseaseProfile createDefault() {
        return new DiseaseProfile();
    }

    public Map<PhiDisease, DiseaseInstance> diseases() {
        return diseases;
    }

    public boolean has(PhiDisease disease) {
        return diseases.containsKey(disease);
    }

    @Nullable
    public DiseaseInstance get(PhiDisease disease) {
        return diseases.get(disease);
    }

    public boolean isEmpty() {
        return diseases.isEmpty() && !orkanumnScar;
    }

    public boolean clearOnDeath() {
        return clearOnDeath;
    }

    public void setClearOnDeath(boolean clearOnDeath) {
        this.clearOnDeath = clearOnDeath;
    }

    public boolean orkanumnScar() {
        return orkanumnScar;
    }

    public void setOrkanumnScar(boolean orkanumnScar) {
        this.orkanumnScar = orkanumnScar;
    }

    public boolean crystalFeverImmunity() {
        return crystalFeverImmunity;
    }

    public void setCrystalFeverImmunity(boolean crystalFeverImmunity) {
        this.crystalFeverImmunity = crystalFeverImmunity;
    }

    public int lowPhiExposure() {
        return lowPhiExposure;
    }

    public void setLowPhiExposure(int lowPhiExposure) {
        this.lowPhiExposure = Math.max(0, lowPhiExposure);
    }

    public int highRadExposure() {
        return highRadExposure;
    }

    public void setHighRadExposure(int highRadExposure) {
        this.highRadExposure = Math.max(0, highRadExposure);
    }

    public int dustExposure() {
        return dustExposure;
    }

    public void setDustExposure(int dustExposure) {
        this.dustExposure = Math.max(0, dustExposure);
    }

    public void put(PhiDisease disease, DiseaseInstance instance) {
        diseases.put(disease, instance);
    }

    public void remove(PhiDisease disease) {
        diseases.remove(disease);
    }

    public void clearAll() {
        diseases.clear();
        orkanumnScar = false;
        lowPhiExposure = 0;
        highRadExposure = 0;
        dustExposure = 0;
        // Keep crystalFeverImmunity — lore immunity after recovery.
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("clearOnDeath", clearOnDeath);
        tag.putBoolean("orkanumnScar", orkanumnScar);
        tag.putBoolean("crystalFeverImmunity", crystalFeverImmunity);
        tag.putInt("lowPhiExposure", lowPhiExposure);
        tag.putInt("highRadExposure", highRadExposure);
        tag.putInt("dustExposure", dustExposure);
        ListTag list = new ListTag();
        for (Map.Entry<PhiDisease, DiseaseInstance> entry : diseases.entrySet()) {
            CompoundTag entryTag = entry.getValue().save();
            entryTag.putString("id", entry.getKey().id());
            list.add(entryTag);
        }
        tag.put("diseases", list);
        return tag;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        clearOnDeath = tag.getBoolean("clearOnDeath");
        orkanumnScar = tag.getBoolean("orkanumnScar");
        crystalFeverImmunity = tag.getBoolean("crystalFeverImmunity");
        lowPhiExposure = Math.max(0, tag.getInt("lowPhiExposure"));
        highRadExposure = Math.max(0, tag.getInt("highRadExposure"));
        dustExposure = Math.max(0, tag.getInt("dustExposure"));
        diseases.clear();
        ListTag list = tag.getList("diseases", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            PhiDisease.byId(entryTag.getString("id")).ifPresent(disease -> {
                DiseaseInstance inst = DiseaseInstance.load(entryTag);
                inst.setStage(Math.min(inst.stage(), disease.maxStage()));
                diseases.put(disease, inst);
            });
        }
    }
}
