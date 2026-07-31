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
                ByteBufCodecs.VAR_INT.encode(buf, data.breathTrainHits);
                ByteBufCodecs.VAR_INT.encode(buf, data.breathTrainSessionMisses);
                ByteBufCodecs.VAR_LONG.encode(buf, data.breathTrainFatigueUntilMs);
                ByteBufCodecs.BOOL.encode(buf, data.steamFlightActive);
                ByteBufCodecs.FLOAT.encode(buf, data.steamFlightDrainPerTick);
                ByteBufCodecs.VAR_INT.encode(buf, data.ionChargeTicksRemaining);
                ByteBufCodecs.FLOAT.encode(buf, data.ionChargeBonusDamage);
                ByteBufCodecs.VAR_LONG.encode(buf, data.lichAscensionUntil);
                ByteBufCodecs.FLOAT.encode(buf, data.phylacteryEfficiency);
                ByteBufCodecs.FLOAT.encode(buf, data.biologyQBeforeLich);
                ByteBufCodecs.BOOL.encode(buf, data.seenEntropyWarn);
                ByteBufCodecs.BOOL.encode(buf, data.seenEntropyTutorial);
                ByteBufCodecs.VAR_INT.encode(buf, data.castSuccessStreak);
                ByteBufCodecs.FLOAT.encode(buf, data.necroReservedPsi);
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
                data.breathTrainHits = ByteBufCodecs.VAR_INT.decode(buf);
                data.breathTrainSessionMisses = ByteBufCodecs.VAR_INT.decode(buf);
                data.breathTrainFatigueUntilMs = ByteBufCodecs.VAR_LONG.decode(buf);
                data.steamFlightActive = ByteBufCodecs.BOOL.decode(buf);
                data.steamFlightDrainPerTick = ByteBufCodecs.FLOAT.decode(buf);
                data.ionChargeTicksRemaining = ByteBufCodecs.VAR_INT.decode(buf);
                data.ionChargeBonusDamage = ByteBufCodecs.FLOAT.decode(buf);
                data.lichAscensionUntil = ByteBufCodecs.VAR_LONG.decode(buf);
                data.phylacteryEfficiency = ByteBufCodecs.FLOAT.decode(buf);
                data.biologyQBeforeLich = ByteBufCodecs.FLOAT.decode(buf);
                data.seenEntropyWarn = ByteBufCodecs.BOOL.decode(buf);
                data.seenEntropyTutorial = ByteBufCodecs.BOOL.decode(buf);
                data.castSuccessStreak = ByteBufCodecs.VAR_INT.decode(buf);
                data.necroReservedPsi = ByteBufCodecs.FLOAT.decode(buf);
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
    private int breathTrainHits;
    private int breathTrainSessionMisses;
    private long breathTrainFatigueUntilMs;
    private boolean steamFlightActive;
    private float steamFlightDrainPerTick;
    private int ionChargeTicksRemaining;
    private float ionChargeBonusDamage;
    private long lichAscensionUntil;
    private float phylacteryEfficiency;
    private float biologyQBeforeLich;
    private boolean seenEntropyWarn;
    private boolean seenEntropyTutorial;
    private int castSuccessStreak;
    /** Synced thrall Ψ reserve for client HUD (server recalculates each tick). */
    private float necroReservedPsi;
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

    public int breathTrainHits() {
        return breathTrainHits;
    }

    public int breathTrainSessionMisses() {
        return breathTrainSessionMisses;
    }

    public float breathTrainRegenBonus() {
        return breathTrainHits * BalanceConfig.BREATHING_TRAIN_REGEN_BONUS.get().floatValue();
    }

    public boolean isBreathTrainFatigued() {
        return System.currentTimeMillis() < breathTrainFatigueUntilMs;
    }

    public long breathTrainFatigueRemainingMs() {
        return Math.max(0L, breathTrainFatigueUntilMs - System.currentTimeMillis());
    }

    public boolean steamFlightActive() {
        return steamFlightActive;
    }

    public float steamFlightDrainPerTick() {
        return steamFlightDrainPerTick;
    }

    public void setSteamFlightActive(boolean active) {
        this.steamFlightActive = active;
    }

    public void setSteamFlightDrainPerTick(float drainPerTick) {
        this.steamFlightDrainPerTick = Math.max(0f, drainPerTick);
    }

    public int ionChargeTicksRemaining() {
        return ionChargeTicksRemaining;
    }

    public void activateIonCharge(int ticks, float bonusDamage) {
        this.ionChargeTicksRemaining = Math.max(this.ionChargeTicksRemaining, Math.max(0, ticks));
        this.ionChargeBonusDamage = Math.max(this.ionChargeBonusDamage, Math.max(0f, bonusDamage));
    }

    /** Consumes stored ion bonus for the next electric strike (e.g. plasma). */
    public float takeIonChargeBonus() {
        if (ionChargeTicksRemaining <= 0 || ionChargeBonusDamage <= 0f) {
            return 0f;
        }
        float bonus = ionChargeBonusDamage;
        ionChargeTicksRemaining = 0;
        ionChargeBonusDamage = 0f;
        return bonus;
    }

    public void tickIonCharge() {
        if (ionChargeTicksRemaining > 0) {
            ionChargeTicksRemaining--;
        }
    }

    public void clearIonCharge() {
        ionChargeTicksRemaining = 0;
        ionChargeBonusDamage = 0f;
    }

    /** Successful timing hit: permanent regen bonus + mastery. No fatigue. */
    public void recordSuccessfulBreathTrain() {
        breathTrainHits++;
        float masteryGain = BalanceConfig.BREATHING_TRAIN_MASTERY_GAIN.get().floatValue();
        if (masteryGain > 0f) {
            com.effecoria.core.progression.BreathingService.addMastery(this, masteryGain);
        }
    }

    /**
     * Failed timing click: shrinks the green zone (via miss count) and may apply fatigue.
     *
     * @return true if fatigue was applied this miss
     */
    public boolean recordBreathTrainMiss() {
        breathTrainSessionMisses++;
        int limit = Math.max(1, BalanceConfig.BREATHING_TRAIN_MISS_LIMIT.get());
        if (breathTrainSessionMisses < limit) {
            return false;
        }
        breathTrainSessionMisses = 0;
        long fatigueMs = BalanceConfig.BREATHING_TRAIN_FATIGUE_MS.get();
        breathTrainFatigueUntilMs = System.currentTimeMillis() + Math.max(0L, fatigueMs);
        return true;
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

    /** Orkanum efficiency with breathing technique bonus (scales past 100%). */
    public float effectiveBiologyQ() {
        float ratio = com.effecoria.core.progression.BreathingService.referenceRatio(breathingMastery);
        float breathingMult = 1f + ratio * BalanceConfig.BREATHING_BIOLOGY_BONUS_MAX.get().floatValue();
        return biologyQ * breathingMult;
    }

    public boolean isPhiSenseActive(long gameTime) {
        return phiSenseUntil > gameTime;
    }

    public boolean isLichAscensionActive(long gameTime) {
        return lichAscensionUntil > gameTime;
    }

    public float phylacteryEfficiency() {
        return phylacteryEfficiency;
    }

    /** Enter lich ascension: Q_biology → 0, Ψ regen uses phylactery efficiency until {@code until}. */
    public void beginLichAscension(long until, float storedBiologyQ, float phylEfficiency) {
        this.biologyQBeforeLich = Math.max(0.05f, storedBiologyQ);
        this.biologyQ = 0f;
        this.lichAscensionUntil = until;
        this.phylacteryEfficiency = Math.clamp(phylEfficiency, 0.2f, 1.5f);
    }

    public void boostPhylacteryEfficiency(long gameTime, float bonus) {
        if (!isLichAscensionActive(gameTime)) {
            return;
        }
        this.phylacteryEfficiency = Math.clamp(phylacteryEfficiency + bonus, 0.2f, 1.5f);
    }

    /** Restores biology when ascension ends; returns true if state just cleared. */
    public boolean tickLichAscension(long gameTime) {
        if (lichAscensionUntil <= 0) {
            return false;
        }
        if (gameTime < lichAscensionUntil) {
            return false;
        }
        if (biologyQ <= 0f && biologyQBeforeLich > 0f) {
            setBiologyQ(biologyQBeforeLich);
        }
        biologyQBeforeLich = 0f;
        lichAscensionUntil = 0L;
        phylacteryEfficiency = 0f;
        return true;
    }

    public void clearLichAscension() {
        if (biologyQ <= 0f && biologyQBeforeLich > 0f) {
            setBiologyQ(biologyQBeforeLich);
        }
        biologyQBeforeLich = 0f;
        lichAscensionUntil = 0L;
        phylacteryEfficiency = 0f;
    }

    public void setCurrentPsi(float value) {
        this.currentPsi = Math.clamp(value, 0f, maxPsi);
    }

    public void setEntropyB(float value) {
        this.entropyB = Math.max(0f, value);
    }

    public boolean seenEntropyWarn() {
        return seenEntropyWarn;
    }

    public void setSeenEntropyWarn(boolean value) {
        this.seenEntropyWarn = value;
    }

    public boolean seenEntropyTutorial() {
        return seenEntropyTutorial;
    }

    public void setSeenEntropyTutorial(boolean value) {
        this.seenEntropyTutorial = value;
    }

    public int castSuccessStreak() {
        return castSuccessStreak;
    }

    public void setCastSuccessStreak(int value) {
        this.castSuccessStreak = Math.max(0, value);
    }

    public float necroReservedPsi() {
        return necroReservedPsi;
    }

    public void setNecroReservedPsi(float value) {
        this.necroReservedPsi = Math.max(0f, value);
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
        float hard = BalanceConfig.BREATHING_HARD_CAP.get().floatValue();
        float v = Math.max(0f, value);
        if (hard > 0f) {
            v = Math.min(v, hard);
        }
        this.breathingMastery = v;
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

    public void unlockSpell(ResourceLocation spellId) {
        if (knownSpells.contains(spellId)) {
            return;
        }
        knownSpells.add(spellId);
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
        tag.putInt("breathTrainHits", breathTrainHits);
        tag.putInt("breathTrainSessionMisses", breathTrainSessionMisses);
        tag.putLong("breathTrainFatigueUntilMs", breathTrainFatigueUntilMs);
        tag.putBoolean("steamFlightActive", steamFlightActive);
        tag.putFloat("steamFlightDrainPerTick", steamFlightDrainPerTick);
        tag.putInt("ionChargeTicksRemaining", ionChargeTicksRemaining);
        tag.putFloat("ionChargeBonusDamage", ionChargeBonusDamage);
        tag.putLong("lichAscensionUntil", lichAscensionUntil);
        tag.putFloat("phylacteryEfficiency", phylacteryEfficiency);
        tag.putFloat("biologyQBeforeLich", biologyQBeforeLich);
        tag.putBoolean("seenEntropyWarn", seenEntropyWarn);
        tag.putBoolean("seenEntropyTutorial", seenEntropyTutorial);
        tag.putInt("castSuccessStreak", castSuccessStreak);

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
        breathTrainHits = tag.contains("breathTrainHits") ? tag.getInt("breathTrainHits") : 0;
        breathTrainSessionMisses = tag.contains("breathTrainSessionMisses") ? tag.getInt("breathTrainSessionMisses") : 0;
        breathTrainFatigueUntilMs = tag.contains("breathTrainFatigueUntilMs") ? tag.getLong("breathTrainFatigueUntilMs") : 0L;
        steamFlightActive = tag.contains("steamFlightActive") && tag.getBoolean("steamFlightActive");
        steamFlightDrainPerTick = tag.contains("steamFlightDrainPerTick") ? tag.getFloat("steamFlightDrainPerTick") : 0f;
        ionChargeTicksRemaining =
                tag.contains("ionChargeTicksRemaining") ? tag.getInt("ionChargeTicksRemaining") : 0;
        ionChargeBonusDamage = tag.contains("ionChargeBonusDamage") ? tag.getFloat("ionChargeBonusDamage") : 0f;
        lichAscensionUntil = tag.contains("lichAscensionUntil") ? tag.getLong("lichAscensionUntil") : 0L;
        phylacteryEfficiency = tag.contains("phylacteryEfficiency") ? tag.getFloat("phylacteryEfficiency") : 0f;
        biologyQBeforeLich = tag.contains("biologyQBeforeLich") ? tag.getFloat("biologyQBeforeLich") : 0f;
        seenEntropyWarn = tag.contains("seenEntropyWarn") && tag.getBoolean("seenEntropyWarn");
        seenEntropyTutorial = tag.contains("seenEntropyTutorial") && tag.getBoolean("seenEntropyTutorial");
        castSuccessStreak = tag.contains("castSuccessStreak") ? tag.getInt("castSuccessStreak") : 0;

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
        copy.breathTrainHits = breathTrainHits;
        copy.breathTrainSessionMisses = breathTrainSessionMisses;
        copy.breathTrainFatigueUntilMs = breathTrainFatigueUntilMs;
        copy.steamFlightActive = steamFlightActive;
        copy.steamFlightDrainPerTick = steamFlightDrainPerTick;
        copy.ionChargeTicksRemaining = ionChargeTicksRemaining;
        copy.ionChargeBonusDamage = ionChargeBonusDamage;
        copy.lichAscensionUntil = lichAscensionUntil;
        copy.phylacteryEfficiency = phylacteryEfficiency;
        copy.biologyQBeforeLich = biologyQBeforeLich;
        copy.seenEntropyWarn = seenEntropyWarn;
        copy.seenEntropyTutorial = seenEntropyTutorial;
        copy.castSuccessStreak = castSuccessStreak;
        copy.necroReservedPsi = necroReservedPsi;
        copy.spellCastCounts = new HashMap<>(spellCastCounts);
        copy.spellLastCastAt = new HashMap<>(spellLastCastAt);
        return copy;
    }
}
