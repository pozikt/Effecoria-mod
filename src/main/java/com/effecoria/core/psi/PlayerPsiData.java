package com.effecoria.core.psi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.magic.MagicSchool;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
                ByteBufCodecs.BOOL.encode(buf, data.schoolChoiceDeferred);
                ByteBufCodecs.STRING_UTF8.encode(buf, data.raceId);
                ByteBufCodecs.FLOAT.encode(buf, data.raceMaxPsiBonus);
                ByteBufCodecs.INT.encode(buf, data.selectedSpellIndex);
                ByteBufCodecs.VAR_LONG.encode(buf, data.phiSenseUntil);
                ByteBufCodecs.FLOAT.encode(buf, data.breathingMastery);
                ByteBufCodecs.FLOAT.encode(buf, data.trainingXp);
                ByteBufCodecs.INT.encode(buf, data.essence);
                ByteBufCodecs.FLOAT.encode(buf, data.phiMultiplier);
                ByteBufCodecs.FLOAT.encode(buf, data.exhaustion);
                ByteBufCodecs.VAR_INT.encode(buf, data.breathTrainHits);
                ByteBufCodecs.VAR_INT.encode(buf, data.breathTrainSessionMisses);
                ByteBufCodecs.VAR_INT.encode(buf, data.breathTrainSessionClicks);
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
                ByteBufCodecs.VAR_INT.encode(buf, data.primerTipsMask);
                ByteBufCodecs.VAR_INT.encode(buf, data.primerSeenMask);
                ByteBufCodecs.VAR_INT.encode(buf, data.castSuccessStreak);
                ByteBufCodecs.FLOAT.encode(buf, data.necroReservedPsi);
                ByteBufCodecs.VAR_LONG.encode(buf, data.overcastUntil);
                ByteBufCodecs.FLOAT.encode(buf, data.overcastSeverity);
                ByteBufCodecs.INT.encode(buf, data.knownSpells.size());
                for (ResourceLocation spell : data.knownSpells) {
                    ResourceLocation.STREAM_CODEC.encode(buf, spell);
                }
                ByteBufCodecs.INT.encode(buf, data.knownSealWords.size());
                for (ResourceLocation word : data.knownSealWords) {
                    ResourceLocation.STREAM_CODEC.encode(buf, word);
                }
                ByteBufCodecs.INT.encode(buf, data.knownItemSeals.size());
                for (ResourceLocation seal : data.knownItemSeals) {
                    ResourceLocation.STREAM_CODEC.encode(buf, seal);
                }
                ByteBufCodecs.INT.encode(buf, data.savedSealExpressions.size());
                for (List<ResourceLocation> expression : data.savedSealExpressions) {
                    ByteBufCodecs.INT.encode(buf, expression.size());
                    for (ResourceLocation token : expression) {
                        ResourceLocation.STREAM_CODEC.encode(buf, token);
                    }
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
                data.schoolChoiceDeferred = ByteBufCodecs.BOOL.decode(buf);
                data.raceId = ByteBufCodecs.STRING_UTF8.decode(buf);
                data.raceMaxPsiBonus = ByteBufCodecs.FLOAT.decode(buf);
                data.selectedSpellIndex = ByteBufCodecs.INT.decode(buf);
                data.phiSenseUntil = ByteBufCodecs.VAR_LONG.decode(buf);
                data.breathingMastery = ByteBufCodecs.FLOAT.decode(buf);
                data.trainingXp = ByteBufCodecs.FLOAT.decode(buf);
                data.essence = ByteBufCodecs.INT.decode(buf);
                data.phiMultiplier = ByteBufCodecs.FLOAT.decode(buf);
                data.exhaustion = ByteBufCodecs.FLOAT.decode(buf);
                data.breathTrainHits = ByteBufCodecs.VAR_INT.decode(buf);
                data.breathTrainSessionMisses = ByteBufCodecs.VAR_INT.decode(buf);
                data.breathTrainSessionClicks = ByteBufCodecs.VAR_INT.decode(buf);
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
                data.primerTipsMask = ByteBufCodecs.VAR_INT.decode(buf);
                data.primerSeenMask = ByteBufCodecs.VAR_INT.decode(buf);
                data.castSuccessStreak = ByteBufCodecs.VAR_INT.decode(buf);
                data.necroReservedPsi = ByteBufCodecs.FLOAT.decode(buf);
                data.overcastUntil = ByteBufCodecs.VAR_LONG.decode(buf);
                data.overcastSeverity = ByteBufCodecs.FLOAT.decode(buf);
                int spellCount = ByteBufCodecs.INT.decode(buf);
                data.knownSpells = new ArrayList<>(spellCount);
                for (int i = 0; i < spellCount; i++) {
                    data.knownSpells.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                int wordCount = ByteBufCodecs.INT.decode(buf);
                data.knownSealWords = new ArrayList<>(wordCount);
                for (int i = 0; i < wordCount; i++) {
                    data.knownSealWords.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                int itemSealCount = ByteBufCodecs.INT.decode(buf);
                data.knownItemSeals = new ArrayList<>(itemSealCount);
                for (int i = 0; i < itemSealCount; i++) {
                    data.knownItemSeals.add(ResourceLocation.STREAM_CODEC.decode(buf));
                }
                int expressionCount = ByteBufCodecs.INT.decode(buf);
                data.savedSealExpressions = new ArrayList<>(expressionCount);
                for (int i = 0; i < expressionCount; i++) {
                    int tokenCount = ByteBufCodecs.INT.decode(buf);
                    List<ResourceLocation> expression = new ArrayList<>(tokenCount);
                    for (int j = 0; j < tokenCount; j++) {
                        expression.add(ResourceLocation.STREAM_CODEC.decode(buf));
                    }
                    data.savedSealExpressions.add(expression);
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
    /** Player closed first-join school select without initiating; may choose later via Resonance Focus. */
    private boolean schoolChoiceDeferred;
    /** Serialized {@link com.effecoria.core.progression.PlayerRace} id; empty = unset. */
    private String raceId = "";
    /** Max-Ψ bonus already applied from race (Lonver); cleared on rerace. */
    private float raceMaxPsiBonus;
    private int selectedSpellIndex;
    private List<ResourceLocation> knownSpells = new ArrayList<>();
    private List<ResourceLocation> knownSealWords = new ArrayList<>();
    private List<ResourceLocation> knownItemSeals = new ArrayList<>();
    private List<List<ResourceLocation>> savedSealExpressions = new ArrayList<>();
    private long phiSenseUntil;
    private float breathingMastery;
    private float trainingXp;
    private int essence;
    private float phiMultiplier = 1f;
    private float exhaustion;
    private int breathTrainHits;
    private int breathTrainSessionMisses;
    /** Total timing clicks this drill round (synced — closing UI does not reset). */
    private int breathTrainSessionClicks;
    private long breathTrainFatigueUntilMs;
    /** Server-only anti-spam clock for breathing-train hits (not persisted / not synced). */
    private transient long lastBreathTrainHitMs;
    private boolean steamFlightActive;
    private float steamFlightDrainPerTick;
    private int ionChargeTicksRemaining;
    private float ionChargeBonusDamage;
    private long lichAscensionUntil;
    private float phylacteryEfficiency;
    private float biologyQBeforeLich;
    private boolean seenEntropyWarn;
    private boolean seenEntropyTutorial;
    /** Bitmask of first-hour tip IDs already shown ({@link com.effecoria.core.progression.FirstHourTips}). */
    private int primerTipsMask;
    /** Bitmask of Magic Primer chapters the player has opened ({@link com.effecoria.core.progression.PrimerChapters}). */
    private int primerSeenMask;
    private int castSuccessStreak;
    /** Synced thrall/servant Ψ reserve for client HUD (server recalculates each tick). */
    private float necroReservedPsi;
    /** Server-only thrall UUID ledger (not synced via STREAM_CODEC). */
    private List<UUID> necroThrallIds = new ArrayList<>();
    /** Server-only mental servant UUID ledger (not synced via STREAM_CODEC). */
    private List<UUID> mentalServantIds = new ArrayList<>();
    /** Server-only Ψ reserve per mental servant UUID (source of truth for HUD sync). */
    private Map<UUID, Float> mentalServantReserves = new HashMap<>();
    /** Server-only Φ-construct UUID ledger (MVP: one). */
    private List<UUID> constructIds = new ArrayList<>();
    private long overcastUntil;
    private float overcastSeverity;
    private Map<ResourceLocation, Integer> spellCastCounts = new HashMap<>();
    private Map<ResourceLocation, Long> spellLastCastAt = new HashMap<>();

    /** Soulbound Mage Tower (server-persisted via NBT / copyOnDeath). */
    private boolean towerBound;
    private String towerDimId = "";
    private int towerX;
    private int towerY;
    private int towerZ;
    private boolean pendingTowerRevive;
    private String preferredBodyType = "basic";
    private int savedTowerXpTotal;
    private int savedTowerXpLevel;
    private float savedTowerXpProgress;

    /** Server-only last position for movement-based training XP. */
    private transient double trainingSampleX = Double.NaN;
    private transient double trainingSampleZ = Double.NaN;

    public static PlayerPsiData createDefault() {
        PlayerPsiData data = new PlayerPsiData();
        data.maxPsi = BalanceConfig.DEFAULT_MAX_PSI.get().floatValue();
        data.currentPsi = data.maxPsi * 0.5f;
        data.biologyQ = com.effecoria.core.progression.BiologyService.defaultBaseline();
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

    public boolean schoolChoiceDeferred() {
        return schoolChoiceDeferred;
    }

    public void setSchoolChoiceDeferred(boolean deferred) {
        this.schoolChoiceDeferred = deferred;
    }

    public String raceId() {
        return raceId == null ? "" : raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId == null ? "" : raceId;
    }

    public java.util.Optional<com.effecoria.core.progression.PlayerRace> race() {
        return com.effecoria.core.progression.PlayerRace.byId(raceId());
    }

    public float raceMaxPsiBonus() {
        return raceMaxPsiBonus;
    }

    public void setRaceMaxPsiBonus(float raceMaxPsiBonus) {
        this.raceMaxPsiBonus = Math.max(0f, raceMaxPsiBonus);
    }

    public int selectedSpellIndex() {
        return selectedSpellIndex;
    }

    public List<ResourceLocation> knownSpells() {
        return knownSpells;
    }

    public List<ResourceLocation> knownSealWords() {
        return knownSealWords;
    }

    public List<ResourceLocation> knownItemSeals() {
        return knownItemSeals;
    }

    public boolean knowsItemSeal(ResourceLocation id) {
        return knownItemSeals.contains(id);
    }

    public void unlockItemSeal(ResourceLocation id) {
        if (!knownItemSeals.contains(id)) {
            knownItemSeals.add(id);
        }
    }

    public void setKnownItemSeals(List<ResourceLocation> seals) {
        this.knownItemSeals = new ArrayList<>(seals);
    }

    public List<List<ResourceLocation>> savedSealExpressions() {
        return savedSealExpressions;
    }

    public List<ResourceLocation> savedSealExpression(int slot) {
        if (slot < 0 || slot >= savedSealExpressions.size()) {
            return List.of();
        }
        return savedSealExpressions.get(slot);
    }

    public void saveSealExpression(int slot, List<ResourceLocation> tokens) {
        if (slot < 0) {
            return;
        }
        while (savedSealExpressions.size() <= slot) {
            savedSealExpressions.add(new ArrayList<>());
        }
        savedSealExpressions.set(slot, new ArrayList<>(tokens));
    }

    public boolean knowsSealWord(ResourceLocation id) {
        return knownSealWords.contains(id);
    }

    public void unlockSealWord(ResourceLocation id) {
        if (!knownSealWords.contains(id)) {
            knownSealWords.add(id);
        }
    }

    public void setKnownSealWords(List<ResourceLocation> words) {
        this.knownSealWords = new ArrayList<>(words);
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

    public int breathTrainSessionClicks() {
        return breathTrainSessionClicks;
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

    /** @return true if enough real time has passed since the last accepted hit. */
    public boolean tryAcceptBreathTrainHit(long minIntervalMs) {
        long now = System.currentTimeMillis();
        if (now - lastBreathTrainHitMs < Math.max(0L, minIntervalMs)) {
            return false;
        }
        lastBreathTrainHitMs = now;
        return true;
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
        breathTrainSessionClicks++;
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
        breathTrainSessionClicks++;
        breathTrainSessionMisses++;
        int limit = Math.max(1, BalanceConfig.BREATHING_TRAIN_MISS_LIMIT.get());
        if (breathTrainSessionMisses < limit) {
            return false;
        }
        breathTrainSessionMisses = 0;
        breathTrainSessionClicks = 0;
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

    public int primerTipsMask() {
        return primerTipsMask;
    }

    public void setPrimerTipsMask(int value) {
        this.primerTipsMask = value;
    }

    public int primerSeenMask() {
        return primerSeenMask;
    }

    public void setPrimerSeenMask(int value) {
        this.primerSeenMask = value;
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

    /** Server-only thrall ledger; not included in STREAM_CODEC. */
    public List<UUID> necroThrallIds() {
        return necroThrallIds;
    }

    public void trackThrall(UUID thrallId) {
        if (thrallId == null) {
            return;
        }
        if (!necroThrallIds.contains(thrallId)) {
            necroThrallIds.add(thrallId);
        }
    }

    public void untrackThrall(UUID thrallId) {
        if (thrallId == null) {
            return;
        }
        necroThrallIds.remove(thrallId);
    }

    /** Server-only mental servant ledger; not included in STREAM_CODEC. */
    public List<UUID> mentalServantIds() {
        return mentalServantIds;
    }

    public void trackMentalServant(UUID servantId) {
        trackMentalServant(servantId, 1f);
    }

    public void trackMentalServant(UUID servantId, float reserveCost) {
        if (servantId == null) {
            return;
        }
        if (!mentalServantIds.contains(servantId)) {
            mentalServantIds.add(servantId);
        }
        mentalServantReserves.put(servantId, Math.max(1f, reserveCost));
    }

    public void untrackMentalServant(UUID servantId) {
        if (servantId == null) {
            return;
        }
        mentalServantIds.remove(servantId);
        mentalServantReserves.remove(servantId);
    }

    public List<UUID> constructIds() {
        return constructIds;
    }

    public void trackConstruct(UUID constructId) {
        if (constructId == null) {
            return;
        }
        if (!constructIds.contains(constructId)) {
            constructIds.add(constructId);
        }
    }

    public void untrackConstruct(UUID constructId) {
        if (constructId == null) {
            return;
        }
        constructIds.remove(constructId);
    }

    /** Sum of Ψ reserved by mental servants (server ledger; independent of entity lookup). */
    public float mentalReservedPsi() {
        float total = 0f;
        for (float value : mentalServantReserves.values()) {
            total += Math.max(0f, value);
        }
        return total;
    }

    public boolean hasOvercastTrauma(long gameTime) {
        return overcastSeverity > 0f && gameTime < overcastUntil;
    }

    public float overcastSeverity() {
        return overcastSeverity;
    }

    public long overcastUntil() {
        return overcastUntil;
    }

    public void clearOvercastTrauma() {
        overcastUntil = 0L;
        overcastSeverity = 0f;
    }

    public void applyOvercastTrauma(long gameTime, float severity, int durationTicks) {
        float s = Math.clamp(severity, 0f, 1f);
        this.overcastSeverity = Math.max(this.overcastSeverity, s);
        long until = gameTime + Math.max(1, durationTicks);
        this.overcastUntil = Math.max(this.overcastUntil, until);
    }

    /** Ψ regen crush while overcast trauma is active. */
    public float overcastRegenMultiplier(long gameTime) {
        if (!hasOvercastTrauma(gameTime)) {
            return 1f;
        }
        float min = BalanceConfig.OVERCAST_REGEN_MIN.get().floatValue();
        float max = BalanceConfig.OVERCAST_REGEN_MAX.get().floatValue();
        return max + (min - max) * overcastSeverity;
    }

    /** Effective breathing collapse while overcast trauma is active. */
    public float overcastBreathFactor(long gameTime) {
        if (!hasOvercastTrauma(gameTime)) {
            return 1f;
        }
        float min = BalanceConfig.OVERCAST_BREATH_MIN.get().floatValue();
        float max = BalanceConfig.OVERCAST_BREATH_MAX.get().floatValue();
        return max + (min - max) * overcastSeverity;
    }

    public void setPhiSenseUntil(long gameTime) {
        this.phiSenseUntil = gameTime;
    }

    public void setSoulStrength(float value) {
        this.soulStrength = Math.max(0.1f, value);
    }

    public boolean towerBound() {
        return towerBound;
    }

    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> towerDim() {
        if (towerDimId == null || towerDimId.isEmpty()) {
            return null;
        }
        return net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(towerDimId));
    }

    public net.minecraft.core.BlockPos towerPos() {
        if (!towerBound) {
            return null;
        }
        return new net.minecraft.core.BlockPos(towerX, towerY, towerZ);
    }

    public boolean pendingTowerRevive() {
        return pendingTowerRevive;
    }

    public com.effecoria.core.tower.TowerBodyType preferredBodyType() {
        return com.effecoria.core.tower.TowerBodyType.fromId(preferredBodyType);
    }

    public void setPreferredBodyType(com.effecoria.core.tower.TowerBodyType type) {
        this.preferredBodyType = type == null ? "basic" : type.getSerializedName();
    }

    public int savedTowerXpTotal() {
        return savedTowerXpTotal;
    }

    public int savedTowerXpLevel() {
        return savedTowerXpLevel;
    }

    public float savedTowerXpProgress() {
        return savedTowerXpProgress;
    }

    public void bindTower(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
            net.minecraft.core.BlockPos pos,
            com.effecoria.core.tower.TowerBodyType body) {
        this.towerBound = true;
        this.towerDimId = dim.location().toString();
        this.towerX = pos.getX();
        this.towerY = pos.getY();
        this.towerZ = pos.getZ();
        setPreferredBodyType(body);
    }

    public void clearTowerBind() {
        this.towerBound = false;
        this.towerDimId = "";
        this.towerX = 0;
        this.towerY = 0;
        this.towerZ = 0;
        this.pendingTowerRevive = false;
        this.savedTowerXpTotal = 0;
        this.savedTowerXpLevel = 0;
        this.savedTowerXpProgress = 0f;
    }

    public void prepareTowerRevive(int xpTotal, int xpLevel, float xpProgress) {
        this.pendingTowerRevive = true;
        this.savedTowerXpTotal = Math.max(0, xpTotal);
        this.savedTowerXpLevel = Math.max(0, xpLevel);
        this.savedTowerXpProgress = Math.max(0f, xpProgress);
    }

    public void clearPendingTowerRevive() {
        this.pendingTowerRevive = false;
        this.savedTowerXpTotal = 0;
        this.savedTowerXpLevel = 0;
        this.savedTowerXpProgress = 0f;
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

    /** Horizontal blocks moved since the last progression sample; ignores teleports. */
    public float consumeMovementSample(net.minecraft.server.level.ServerPlayer player) {
        double x = player.getX();
        double z = player.getZ();
        if (Double.isNaN(trainingSampleX)) {
            trainingSampleX = x;
            trainingSampleZ = z;
            return 0f;
        }
        double dx = x - trainingSampleX;
        double dz = z - trainingSampleZ;
        trainingSampleX = x;
        trainingSampleZ = z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 32.0) {
            return 0f;
        }
        return (float) dist;
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
        this.schoolChoiceDeferred = false;
        this.knownSpells = new ArrayList<>(spells);
        this.selectedSpellIndex = 0;
        this.entropyB = 0f;
        this.phiSenseUntil = 0L;
        if (resetResources && biologyQ <= 0.001f) {
            biologyQ = com.effecoria.core.progression.BiologyService.defaultBaseline();
        }
        this.spellCastCounts.clear();
        this.spellLastCastAt.clear();
        if (chosenSchool == MagicSchool.SEALS) {
            this.knownSealWords = new ArrayList<>(com.effecoria.core.seal.SealProgramService.starterWordIds());
            this.knownSpells = new ArrayList<>();
            this.knownItemSeals = new ArrayList<>(com.effecoria.core.artifact.ItemSealCatalog.starterIds());
        } else if (resetResources) {
            this.knownSealWords = new ArrayList<>();
            this.knownItemSeals = new ArrayList<>();
        }
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
        tag.putBoolean("schoolChoiceDeferred", schoolChoiceDeferred);
        tag.putString("raceId", raceId == null ? "" : raceId);
        tag.putFloat("raceMaxPsiBonus", raceMaxPsiBonus);
        tag.putInt("selectedSpellIndex", selectedSpellIndex);
        tag.putLong("phiSenseUntil", phiSenseUntil);
        tag.putFloat("breathingMastery", breathingMastery);
        tag.putFloat("trainingXp", trainingXp);
        tag.putInt("essence", essence);
        tag.putFloat("phiMultiplier", phiMultiplier);
        tag.putFloat("exhaustion", exhaustion);
        tag.putInt("breathTrainHits", breathTrainHits);
        tag.putInt("breathTrainSessionMisses", breathTrainSessionMisses);
        tag.putInt("breathTrainSessionClicks", breathTrainSessionClicks);
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
        tag.putInt("primerTipsMask", primerTipsMask);
        tag.putInt("primerSeenMask", primerSeenMask);
        tag.putInt("castSuccessStreak", castSuccessStreak);
        tag.putFloat("necroReservedPsi", necroReservedPsi);
        tag.putLong("overcastUntil", overcastUntil);
        tag.putFloat("overcastSeverity", overcastSeverity);
        tag.putBoolean("towerBound", towerBound);
        tag.putString("towerDimId", towerDimId == null ? "" : towerDimId);
        tag.putInt("towerX", towerX);
        tag.putInt("towerY", towerY);
        tag.putInt("towerZ", towerZ);
        tag.putBoolean("pendingTowerRevive", pendingTowerRevive);
        tag.putString("preferredBodyType", preferredBodyType == null ? "basic" : preferredBodyType);
        tag.putInt("savedTowerXpTotal", savedTowerXpTotal);
        tag.putInt("savedTowerXpLevel", savedTowerXpLevel);
        tag.putFloat("savedTowerXpProgress", savedTowerXpProgress);

        ListTag thrallList = new ListTag();
        for (UUID thrallId : necroThrallIds) {
            thrallList.add(StringTag.valueOf(thrallId.toString()));
        }
        tag.put("necroThrallIds", thrallList);

        ListTag servantList = new ListTag();
        for (UUID servantId : mentalServantIds) {
            servantList.add(StringTag.valueOf(servantId.toString()));
        }
        tag.put("mentalServantIds", servantList);

        CompoundTag servantReserves = new CompoundTag();
        for (Map.Entry<UUID, Float> entry : mentalServantReserves.entrySet()) {
            servantReserves.putFloat(entry.getKey().toString(), entry.getValue());
        }
        tag.put("mentalServantReserves", servantReserves);

        ListTag constructList = new ListTag();
        for (UUID constructId : constructIds) {
            constructList.add(StringTag.valueOf(constructId.toString()));
        }
        tag.put("constructIds", constructList);

        ListTag spellList = new ListTag();
        for (ResourceLocation spell : knownSpells) {
            spellList.add(net.minecraft.nbt.StringTag.valueOf(spell.toString()));
        }
        tag.put("knownSpells", spellList);

        ListTag wordList = new ListTag();
        for (ResourceLocation word : knownSealWords) {
            wordList.add(net.minecraft.nbt.StringTag.valueOf(word.toString()));
        }
        tag.put("knownSealWords", wordList);
        ListTag itemSealList = new ListTag();
        for (ResourceLocation seal : knownItemSeals) {
            itemSealList.add(net.minecraft.nbt.StringTag.valueOf(seal.toString()));
        }
        tag.put("knownItemSeals", itemSealList);
        ListTag expressionsList = new ListTag();
        for (List<ResourceLocation> expression : savedSealExpressions) {
            ListTag expressionTag = new ListTag();
            for (ResourceLocation token : expression) {
                expressionTag.add(StringTag.valueOf(token.toString()));
            }
            expressionsList.add(expressionTag);
        }
        tag.put("savedSealExpressions", expressionsList);

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
        schoolChoiceDeferred = tag.contains("schoolChoiceDeferred") && tag.getBoolean("schoolChoiceDeferred");
        raceId = tag.contains("raceId") ? tag.getString("raceId") : "";
        raceMaxPsiBonus = tag.contains("raceMaxPsiBonus") ? tag.getFloat("raceMaxPsiBonus") : 0f;
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
        breathTrainSessionClicks = tag.contains("breathTrainSessionClicks") ? tag.getInt("breathTrainSessionClicks") : 0;
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
        primerTipsMask = tag.contains("primerTipsMask") ? tag.getInt("primerTipsMask") : 0;
        primerSeenMask = tag.contains("primerSeenMask") ? tag.getInt("primerSeenMask") : 0;
        castSuccessStreak = tag.contains("castSuccessStreak") ? tag.getInt("castSuccessStreak") : 0;
        overcastUntil = tag.contains("overcastUntil") ? tag.getLong("overcastUntil") : 0L;
        overcastSeverity = tag.contains("overcastSeverity") ? tag.getFloat("overcastSeverity") : 0f;
        towerBound = tag.contains("towerBound") && tag.getBoolean("towerBound");
        towerDimId = tag.contains("towerDimId") ? tag.getString("towerDimId") : "";
        towerX = tag.contains("towerX") ? tag.getInt("towerX") : 0;
        towerY = tag.contains("towerY") ? tag.getInt("towerY") : 0;
        towerZ = tag.contains("towerZ") ? tag.getInt("towerZ") : 0;
        pendingTowerRevive = tag.contains("pendingTowerRevive") && tag.getBoolean("pendingTowerRevive");
        preferredBodyType = tag.contains("preferredBodyType") ? tag.getString("preferredBodyType") : "basic";
        savedTowerXpTotal = tag.contains("savedTowerXpTotal") ? tag.getInt("savedTowerXpTotal") : 0;
        savedTowerXpLevel = tag.contains("savedTowerXpLevel") ? tag.getInt("savedTowerXpLevel") : 0;
        savedTowerXpProgress = tag.contains("savedTowerXpProgress") ? tag.getFloat("savedTowerXpProgress") : 0f;
        necroReservedPsi = tag.contains("necroReservedPsi") ? tag.getFloat("necroReservedPsi") : 0f;

        necroThrallIds = new ArrayList<>();
        if (tag.contains("necroThrallIds", Tag.TAG_LIST)) {
            ListTag thrallList = tag.getList("necroThrallIds", Tag.TAG_STRING);
            for (Tag entry : thrallList) {
                try {
                    necroThrallIds.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException ignored) {
                    // Skip corrupt entries from older/broken saves.
                }
            }
        }
        mentalServantIds = new ArrayList<>();
        if (tag.contains("mentalServantIds", Tag.TAG_LIST)) {
            ListTag servantList = tag.getList("mentalServantIds", Tag.TAG_STRING);
            for (Tag entry : servantList) {
                try {
                    mentalServantIds.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException ignored) {
                    // Skip corrupt entries from older/broken saves.
                }
            }
        }
        mentalServantReserves = new HashMap<>();
        if (tag.contains("mentalServantReserves", Tag.TAG_COMPOUND)) {
            CompoundTag reserves = tag.getCompound("mentalServantReserves");
            for (String key : reserves.getAllKeys()) {
                try {
                    mentalServantReserves.put(UUID.fromString(key), Math.max(1f, reserves.getFloat(key)));
                } catch (IllegalArgumentException ignored) {
                    // Skip corrupt keys.
                }
            }
        }
        // Backfill reserve map for older saves that only had UUID lists.
        for (UUID id : mentalServantIds) {
            mentalServantReserves.putIfAbsent(id, 1f);
        }

        constructIds = new ArrayList<>();
        if (tag.contains("constructIds", Tag.TAG_LIST)) {
            ListTag constructList = tag.getList("constructIds", Tag.TAG_STRING);
            for (Tag entry : constructList) {
                try {
                    constructIds.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException ignored) {
                    // Skip corrupt entries.
                }
            }
        }

        knownSpells = new ArrayList<>();
        ListTag spellList = tag.getList("knownSpells", Tag.TAG_STRING);
        for (Tag entry : spellList) {
            ResourceLocation spellId = ResourceLocation.parse(entry.getAsString());
            if (spellId.getPath().equals("stone_shield")) {
                spellId = ResourceLocation.fromNamespaceAndPath("effecoria", "water_stream");
            }
            knownSpells.add(spellId);
        }

        knownSealWords = new ArrayList<>();
        if (tag.contains("knownSealWords", Tag.TAG_LIST)) {
            ListTag wordList = tag.getList("knownSealWords", Tag.TAG_STRING);
            for (Tag entry : wordList) {
                knownSealWords.add(ResourceLocation.parse(entry.getAsString()));
            }
        }
        knownItemSeals = new ArrayList<>();
        if (tag.contains("knownItemSeals", Tag.TAG_LIST)) {
            ListTag itemSealList = tag.getList("knownItemSeals", Tag.TAG_STRING);
            for (Tag entry : itemSealList) {
                knownItemSeals.add(ResourceLocation.parse(entry.getAsString()));
            }
        }
        savedSealExpressions = new ArrayList<>();
        if (tag.contains("savedSealExpressions", Tag.TAG_LIST)) {
            ListTag expressionsList = tag.getList("savedSealExpressions", Tag.TAG_LIST);
            for (Tag expressionEntry : expressionsList) {
                if (!(expressionEntry instanceof ListTag expressionTag)) {
                    continue;
                }
                List<ResourceLocation> expression = new ArrayList<>();
                for (Tag tokenEntry : expressionTag) {
                    if (tokenEntry.getId() == Tag.TAG_STRING) {
                        expression.add(ResourceLocation.parse(tokenEntry.getAsString()));
                    }
                }
                savedSealExpressions.add(expression);
            }
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

        // Migrate legacy seal spell casters onto the word lexicon.
        if (school == MagicSchool.SEALS) {
            knownSpells.removeIf(id -> id.getNamespace().equals("effecoria") && (
                    id.getPath().endsWith("_seal")
                            || id.getPath().endsWith("_glyph")
                            || id.getPath().equals("snare_matrix")
                            || id.getPath().equals("shock_trap")
                            || id.getPath().equals("anchor_fortify")
                            || id.getPath().equals("permanent_glow")
                            || id.getPath().equals("omega_ward")
                            || id.getPath().equals("beacon_seal")));
            if (knownSealWords.isEmpty()) {
                knownSealWords = new ArrayList<>(com.effecoria.core.seal.SealProgramService.starterWordIds());
            }
            if (knownItemSeals.isEmpty()) {
                knownItemSeals = new ArrayList<>(com.effecoria.core.artifact.ItemSealCatalog.starterIds());
            } else {
                for (ResourceLocation starter : com.effecoria.core.artifact.ItemSealCatalog.starterIds()) {
                    if (!knownItemSeals.contains(starter)) {
                        knownItemSeals.add(starter);
                    }
                }
            }
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
        copy.schoolChoiceDeferred = schoolChoiceDeferred;
        copy.raceId = raceId;
        copy.raceMaxPsiBonus = raceMaxPsiBonus;
        copy.selectedSpellIndex = selectedSpellIndex;
        copy.knownSpells = new ArrayList<>(knownSpells);
        copy.knownSealWords = new ArrayList<>(knownSealWords);
        copy.knownItemSeals = new ArrayList<>(knownItemSeals);
        copy.savedSealExpressions = new ArrayList<>();
        for (List<ResourceLocation> expression : savedSealExpressions) {
            copy.savedSealExpressions.add(new ArrayList<>(expression));
        }
        copy.phiSenseUntil = phiSenseUntil;
        copy.breathingMastery = breathingMastery;
        copy.trainingXp = trainingXp;
        copy.essence = essence;
        copy.phiMultiplier = phiMultiplier;
        copy.exhaustion = exhaustion;
        copy.breathTrainHits = breathTrainHits;
        copy.breathTrainSessionMisses = breathTrainSessionMisses;
        copy.breathTrainSessionClicks = breathTrainSessionClicks;
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
        copy.primerTipsMask = primerTipsMask;
        copy.primerSeenMask = primerSeenMask;
        copy.castSuccessStreak = castSuccessStreak;
        copy.necroReservedPsi = necroReservedPsi;
        copy.necroThrallIds = new ArrayList<>(necroThrallIds);
        copy.mentalServantIds = new ArrayList<>(mentalServantIds);
        copy.mentalServantReserves = new HashMap<>(mentalServantReserves);
        copy.constructIds = new ArrayList<>(constructIds);
        copy.overcastUntil = overcastUntil;
        copy.overcastSeverity = overcastSeverity;
        copy.spellCastCounts = new HashMap<>(spellCastCounts);
        copy.spellLastCastAt = new HashMap<>(spellLastCastAt);
        copy.towerBound = towerBound;
        copy.towerDimId = towerDimId;
        copy.towerX = towerX;
        copy.towerY = towerY;
        copy.towerZ = towerZ;
        copy.pendingTowerRevive = pendingTowerRevive;
        copy.preferredBodyType = preferredBodyType;
        copy.savedTowerXpTotal = savedTowerXpTotal;
        copy.savedTowerXpLevel = savedTowerXpLevel;
        copy.savedTowerXpProgress = savedTowerXpProgress;
        return copy;
    }
}
