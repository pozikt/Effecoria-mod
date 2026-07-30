package com.effecoria.core.psi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.magic.MagicSchool;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public final class PlayerPsiData {
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPsiData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.FLOAT.encode(buf, data.currentPsi);
                ByteBufCodecs.FLOAT.encode(buf, data.maxPsi);
                ByteBufCodecs.FLOAT.encode(buf, data.soulStrength);
                ByteBufCodecs.FLOAT.encode(buf, data.biologyQ);
                ByteBufCodecs.STRING_UTF8.encode(buf, data.school.getSerializedName());
                ByteBufCodecs.FLOAT.encode(buf, data.frequencyHz);
                ByteBufCodecs.FLOAT.encode(buf, data.entropyB);
                ByteBufCodecs.BOOL.encode(buf, data.initiated);
                ByteBufCodecs.INT.encode(buf, data.selectedSpellIndex);
                ByteBufCodecs.VAR_LONG.encode(buf, data.phiSenseUntil);
                ByteBufCodecs.FLOAT.encode(buf, data.breathingMastery);
                ByteBufCodecs.FLOAT.encode(buf, data.trainingXp);
                ByteBufCodecs.INT.encode(buf, data.essence);
                ByteBufCodecs.FLOAT.encode(buf, data.phiMultiplier);
                ByteBufCodecs.FLOAT.encode(buf, data.exhaustion);
                ByteBufCodecs.INT.encode(buf, data.knownSpells.size());
                for (ResourceLocation spell : data.knownSpells) {
                    ResourceLocation.STREAM_CODEC.encode(buf, spell);
                }
                ByteBufCodecs.INT.encode(buf, data.spellCastCounts.size());
                for (Map.Entry<ResourceLocation, Integer> entry : data.spellCastCounts.entrySet()) {
                    ResourceLocation.STREAM_CODEC.encode(buf, entry.getKey());
                    ByteBufCodecs.VAR_INT.encode(buf, entry.getValue());
                }
                ByteBufCodecs.INT.encode(buf, data.spellLastCastAt.size());
                for (Map.Entry<ResourceLocation, Long> entry : data.spellLastCastAt.entrySet()) {
                    ResourceLocation.STREAM_CODEC.encode(buf, entry.getKey());
                    ByteBufCodecs.VAR_LONG.encode(buf, entry.getValue());
                }
            },
            buf -> {
                PlayerPsiData data = new PlayerPsiData();
                data.currentPsi = ByteBufCodecs.FLOAT.decode(buf);
                data.maxPsi = ByteBufCodecs.FLOAT.decode(buf);
                data.soulStrength = ByteBufCodecs.FLOAT.decode(buf);
                data.biologyQ = ByteBufCodecs.FLOAT.decode(buf);
                data.school = MagicSchool.fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buf));
                data.frequencyHz = ByteBufCodecs.FLOAT.decode(buf);
                data.entropyB = ByteBufCodecs.FLOAT.decode(buf);
                data.initiated = ByteBufCodecs.BOOL.decode(buf);
                data.selectedSpellIndex = ByteBufCodecs.INT.decode(buf);
                data.phiSenseUntil = ByteBufCodecs.VAR_LONG.decode(buf);
                data.breathingMastery = ByteBufCodecs.FLOAT.decode(buf);
                data.trainingXp = ByteBufCodecs.FLOAT.decode(buf);
                data.essence = ByteBufCodecs.INT.decode(buf);
                data.phiMultiplier = ByteBufCodecs.FLOAT.decode(buf);
                data.exhaustion = ByteBufCodecs.FLOAT.decode(buf);
                int spellCount = ByteBufCodecs.INT.decode(buf);
                data.knownSpells = new ArrayList<>(spellCount);
                for (int i = 0; i < spellCount; i++) {
                    data.knownSpells.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                int castCountEntries = ByteBufCodecs.INT.decode(buf);
                data.spellCastCounts = new HashMap<>(castCountEntries);
                for (int i = 0; i < castCountEntries; i++) {
                    ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                    data.spellCastCounts.put(id, ByteBufCodecs.VAR_INT.decode(buf));
                }
                int lastCastEntries = ByteBufCodecs.INT.decode(buf);
                data.spellLastCastAt = new HashMap<>(lastCastEntries);
                for (int i = 0; i < lastCastEntries; i++) {
                    ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                    data.spellLastCastAt.put(id, ByteBufCodecs.VAR_LONG.decode(buf));
                }
                return data;
            });

    private float currentPsi;
    private float maxPsi;
    private float soulStrength = 1f;
    private float biologyQ = 0.6f;
    private MagicSchool school = MagicSchool.NONE;
    private float frequencyHz;
    private float entropyB;
    private boolean initiated;
    private int selectedSpellIndex;
    private List<ResourceLocation> knownSpells = new ArrayList<>();
    private long phiSenseUntil;
    private float breathingMastery;
    private float trainingXp;
    private int essence;
    private float phiMultiplier = 1f;
    private float exhaustion;
    private Map<ResourceLocation, Integer> spellCastCounts = new HashMap<>();
    private Map<ResourceLocation, Long> spellLastCastAt = new HashMap<>();

    public static PlayerPsiData createDefault() {
        PlayerPsiData data = new PlayerPsiData();
        data.maxPsi = BalanceConfig.DEFAULT_MAX_PSI.get().floatValue();
        data.currentPsi = data.maxPsi * 0.5f;
        return data;
    }

    public float currentPsi() {
        return currentPsi;
    }

    public float maxPsi() {
        return maxPsi;
    }

    public float soulStrength() {
        return soulStrength;
    }

    public float biologyQ() {
        return biologyQ;
    }

    public MagicSchool school() {
        return school;
    }

    public float frequencyHz() {
        return frequencyHz;
    }

    public float entropyB() {
        return entropyB;
    }

    public boolean initiated() {
        return initiated;
    }

    public int selectedSpellIndex() {
        return selectedSpellIndex;
    }

    public List<ResourceLocation> knownSpells() {
        return knownSpells;
    }

    public long phiSenseUntil() {
        return phiSenseUntil;
    }

    public float breathingMastery() {
        return breathingMastery;
    }

    public float trainingXp() {
        return trainingXp;
    }

    public int essence() {
        return essence;
    }

    public float phiMultiplier() {
        return phiMultiplier;
    }

    public float exhaustion() {
        return exhaustion;
    }

    public void setExhaustion(float value) {
        this.exhaustion = Math.clamp(value, 0f, com.effecoria.core.progression.ExhaustionService.MAX);
    }

    public int spellCastCount(ResourceLocation spellId) {
        return spellCastCounts.getOrDefault(spellId, 0);
    }

    public long spellLastCastAt(ResourceLocation spellId) {
        return spellLastCastAt.getOrDefault(spellId, 0L);
    }

    public void recordSpellCast(ResourceLocation spellId, long gameTime) {
        spellCastCounts.merge(spellId, 1, Integer::sum);
        spellLastCastAt.put(spellId, gameTime);
    }

    public void addEssence(int amount) {
        this.essence = Math.max(0, this.essence + amount);
    }

    public void setEssence(int amount) {
        this.essence = Math.max(0, amount);
    }

    public void setPhiMultiplier(float value) {
        this.phiMultiplier = Math.max(0f, value);
    }

    public void setBiologyQ(float value) {
        this.biologyQ = Math.max(0f, value);
    }

    public float mastery() {
        return com.effecoria.core.formula.Mastery.factor(breathingMastery, essence);
    }

    /** Orkanum efficiency with breathing technique bonus. */
    public float effectiveBiologyQ() {
        float cap = BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue();
        float normalized = cap > 0f ? Math.min(1f, breathingMastery / cap) : 0f;
        float breathingMult = 1f + normalized * BalanceConfig.BREATHING_BIOLOGY_BONUS_MAX.get().floatValue();
        return biologyQ * breathingMult;
    }

    public boolean isPhiSenseActive(long gameTime) {
        return phiSenseUntil > gameTime;
    }

    public void setCurrentPsi(float value) {
        this.currentPsi = Math.clamp(value, 0f, maxPsi);
    }

    public void setEntropyB(float value) {
        this.entropyB = Math.max(0f, value);
    }

    public void setPhiSenseUntil(long gameTime) {
        this.phiSenseUntil = gameTime;
    }

    public void setSoulStrength(float value) {
        this.soulStrength = Math.max(0.1f, value);
    }

    public void setMaxPsi(float value) {
        this.maxPsi = Math.max(10f, value);
        this.currentPsi = Math.min(this.currentPsi, this.maxPsi);
    }

    public void setBreathingMastery(float value) {
        float cap = BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue();
        this.breathingMastery = Math.clamp(value, 0f, cap);
    }

    public void addTrainingXp(float amount) {
        this.trainingXp = Math.max(0f, this.trainingXp + amount);
    }

    public void setTrainingXp(float amount) {
        this.trainingXp = Math.max(0f, amount);
    }

    public void setSelectedSpellIndex(int index) {
        if (knownSpells.isEmpty()) {
            this.selectedSpellIndex = 0;
            return;
        }
        this.selectedSpellIndex = Math.floorMod(index, knownSpells.size());
    }

    public ResourceLocation selectedSpell() {
        if (knownSpells.isEmpty()) {
            return null;
        }
        return knownSpells.get(Math.floorMod(selectedSpellIndex, knownSpells.size()));
    }

    public void cycleSpell(int delta) {
        setSelectedSpellIndex(selectedSpellIndex + delta);
    }

    public void initiate(MagicSchool chosenSchool, List<ResourceLocation> spells) {
        applySchool(chosenSchool, spells, true);
    }

    /** Switch magic school — keeps Ψ pool and progression stats for testing. */
    public void reschool(MagicSchool chosenSchool, List<ResourceLocation> spells) {
        applySchool(chosenSchool, spells, false);
    }

    /** Adds newly introduced school spells without resetting progression. */
    public void mergeMissingSpells(List<ResourceLocation> spells) {
        for (ResourceLocation spell : spells) {
            if (!knownSpells.contains(spell)) {
                knownSpells.add(spell);
            }
        }
    }

    private void applySchool(MagicSchool chosenSchool, List<ResourceLocation> spells, boolean resetResources) {
        this.school = chosenSchool;
        this.frequencyHz = chosenSchool.nominalFrequencyHz();
        this.initiated = true;
        this.knownSpells = new ArrayList<>(spells);
        this.selectedSpellIndex = 0;
        this.entropyB = 0f;
        this.phiSenseUntil = 0L;
        this.spellCastCounts.clear();
        this.spellLastCastAt.clear();
        if (resetResources) {
            this.maxPsi = BalanceConfig.DEFAULT_MAX_PSI.get().floatValue();
            this.currentPsi = BalanceConfig.DEFAULT_STARTING_PSI.get().floatValue();
        } else {
            this.currentPsi = Math.min(this.currentPsi, this.maxPsi);
        }
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("currentPsi", currentPsi);
        tag.putFloat("maxPsi", maxPsi);
        tag.putFloat("soulStrength", soulStrength);
        tag.putFloat("biologyQ", biologyQ);
        tag.putString("school", school.getSerializedName());
        tag.putFloat("frequencyHz", frequencyHz);
        tag.putFloat("entropyB", entropyB);
        tag.putBoolean("initiated", initiated);
        tag.putInt("selectedSpellIndex", selectedSpellIndex);
        tag.putLong("phiSenseUntil", phiSenseUntil);
        tag.putFloat("breathingMastery", breathingMastery);
        tag.putFloat("trainingXp", trainingXp);
        tag.putInt("essence", essence);
        tag.putFloat("phiMultiplier", phiMultiplier);
        tag.putFloat("exhaustion", exhaustion);

        ListTag spellList = new ListTag();
        for (ResourceLocation spell : knownSpells) {
            spellList.add(net.minecraft.nbt.StringTag.valueOf(spell.toString()));
        }
        tag.put("knownSpells", spellList);

        CompoundTag castCounts = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : spellCastCounts.entrySet()) {
            castCounts.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("spellCastCounts", castCounts);

        CompoundTag lastCast = new CompoundTag();
        for (Map.Entry<ResourceLocation, Long> entry : spellLastCastAt.entrySet()) {
            lastCast.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("spellLastCastAt", lastCast);
        return tag;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        currentPsi = tag.getFloat("currentPsi");
        maxPsi = tag.getFloat("maxPsi");
        soulStrength = tag.getFloat("soulStrength");
        biologyQ = tag.getFloat("biologyQ");
        school = MagicSchool.fromSerializedName(tag.getString("school"));
        frequencyHz = tag.getFloat("frequencyHz");
        entropyB = tag.getFloat("entropyB");
        initiated = tag.getBoolean("initiated");
        selectedSpellIndex = tag.getInt("selectedSpellIndex");
        phiSenseUntil = tag.getLong("phiSenseUntil");
        if (tag.contains("breathingMastery")) {
            breathingMastery = tag.getFloat("breathingMastery");
        } else if (tag.contains("breathingTier")) {
            breathingMastery = tag.getInt("breathingTier") * 0.5f;
        } else {
            breathingMastery = 0f;
        }
        setBreathingMastery(breathingMastery);
        trainingXp = tag.contains("trainingXp") ? tag.getFloat("trainingXp") : 0f;
        essence = tag.contains("essence") ? tag.getInt("essence") : 0;
        phiMultiplier = tag.contains("phiMultiplier") ? tag.getFloat("phiMultiplier") : 1f;
        exhaustion = tag.contains("exhaustion") ? tag.getFloat("exhaustion") : 0f;

        knownSpells = new ArrayList<>();
        ListTag spellList = tag.getList("knownSpells", Tag.TAG_STRING);
        for (Tag entry : spellList) {
            ResourceLocation spellId = ResourceLocation.parse(entry.getAsString());
            if (spellId.getPath().equals("stone_shield")) {
                spellId = ResourceLocation.fromNamespaceAndPath("effecoria", "water_stream");
            }
            knownSpells.add(spellId);
        }

        spellCastCounts = new HashMap<>();
        spellLastCastAt = new HashMap<>();
        if (tag.contains("spellCastCounts", Tag.TAG_COMPOUND)) {
            CompoundTag castCounts = tag.getCompound("spellCastCounts");
            for (String key : castCounts.getAllKeys()) {
                spellCastCounts.put(ResourceLocation.parse(key), castCounts.getInt(key));
            }
        }
        if (tag.contains("spellLastCastAt", Tag.TAG_COMPOUND)) {
            CompoundTag lastCast = tag.getCompound("spellLastCastAt");
            for (String key : lastCast.getAllKeys()) {
                spellLastCastAt.put(ResourceLocation.parse(key), lastCast.getLong(key));
            }
        }

        // Old saves used school id "seals" for combat corruption spells.
        if (school == MagicSchool.SEALS && knownSpells.stream().anyMatch(id ->
                id.getPath().equals("corrupt_mark")
                        || id.getPath().equals("blight_pulse")
                        || (id.getPath().equals("binding_seal") && knownSpells.size() <= 3))) {
            school = MagicSchool.CORRUPTION;
            frequencyHz = school.nominalFrequencyHz();
        }
    }

    public PlayerPsiData copy() {
        PlayerPsiData copy = new PlayerPsiData();
        copy.currentPsi = currentPsi;
        copy.maxPsi = maxPsi;
        copy.soulStrength = soulStrength;
        copy.biologyQ = biologyQ;
        copy.school = school;
        copy.frequencyHz = frequencyHz;
        copy.entropyB = entropyB;
        copy.initiated = initiated;
        copy.selectedSpellIndex = selectedSpellIndex;
        copy.knownSpells = new ArrayList<>(knownSpells);
        copy.phiSenseUntil = phiSenseUntil;
        copy.breathingMastery = breathingMastery;
        copy.trainingXp = trainingXp;
        copy.essence = essence;
        copy.phiMultiplier = phiMultiplier;
        copy.exhaustion = exhaustion;
        copy.spellCastCounts = new HashMap<>(spellCastCounts);
        copy.spellLastCastAt = new HashMap<>(spellLastCastAt);
        return copy;
    }
}
