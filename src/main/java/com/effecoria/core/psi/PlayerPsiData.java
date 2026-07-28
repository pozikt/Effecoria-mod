package com.effecoria.core.psi;

import java.util.ArrayList;
import java.util.List;

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
                ByteBufCodecs.INT.encode(buf, data.breathingTier);
                ByteBufCodecs.FLOAT.encode(buf, data.trainingXp);
                ByteBufCodecs.INT.encode(buf, data.knownSpells.size());
                for (ResourceLocation spell : data.knownSpells) {
                    ResourceLocation.STREAM_CODEC.encode(buf, spell);
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
                data.breathingTier = ByteBufCodecs.INT.decode(buf);
                data.trainingXp = ByteBufCodecs.FLOAT.decode(buf);
                int spellCount = ByteBufCodecs.INT.decode(buf);
                data.knownSpells = new ArrayList<>(spellCount);
                for (int i = 0; i < spellCount; i++) {
                    data.knownSpells.add(ResourceLocation.STREAM_CODEC.decode(buf));
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
    private int breathingTier;
    private int calmBreathTicks;
    private float trainingXp;

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

    public int breathingTier() {
        return breathingTier;
    }

    public int calmBreathTicks() {
        return calmBreathTicks;
    }

    public float trainingXp() {
        return trainingXp;
    }

    /** Orkanum efficiency with breathing technique bonus. */
    public float effectiveBiologyQ() {
        float breathingMult = 1f + breathingTier * 0.15f;
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

    public void setBreathingTier(int tier) {
        this.breathingTier = Math.clamp(tier, 0, 2);
    }

    public void addCalmBreathTicks(int ticks) {
        this.calmBreathTicks += ticks;
    }

    public void resetCalmBreathTicks() {
        this.calmBreathTicks = 0;
    }

    public void addTrainingXp(float amount) {
        this.trainingXp = Math.max(0f, this.trainingXp + amount);
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

    private void applySchool(MagicSchool chosenSchool, List<ResourceLocation> spells, boolean resetResources) {
        this.school = chosenSchool;
        this.frequencyHz = chosenSchool.nominalFrequencyHz();
        this.initiated = true;
        this.knownSpells = new ArrayList<>(spells);
        this.selectedSpellIndex = 0;
        this.entropyB = 0f;
        this.phiSenseUntil = 0L;
        this.calmBreathTicks = 0;
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
        tag.putInt("breathingTier", breathingTier);
        tag.putFloat("trainingXp", trainingXp);

        ListTag spellList = new ListTag();
        for (ResourceLocation spell : knownSpells) {
            spellList.add(net.minecraft.nbt.StringTag.valueOf(spell.toString()));
        }
        tag.put("knownSpells", spellList);
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
        breathingTier = tag.contains("breathingTier") ? tag.getInt("breathingTier") : 0;
        trainingXp = tag.contains("trainingXp") ? tag.getFloat("trainingXp") : 0f;

        knownSpells = new ArrayList<>();
        ListTag spellList = tag.getList("knownSpells", Tag.TAG_STRING);
        for (Tag entry : spellList) {
            ResourceLocation spellId = ResourceLocation.parse(entry.getAsString());
            if (spellId.getPath().equals("stone_shield")) {
                spellId = ResourceLocation.fromNamespaceAndPath("effecoria", "water_stream");
            }
            knownSpells.add(spellId);
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
        copy.breathingTier = breathingTier;
        copy.calmBreathTicks = calmBreathTicks;
        copy.trainingXp = trainingXp;
        return copy;
    }
}
