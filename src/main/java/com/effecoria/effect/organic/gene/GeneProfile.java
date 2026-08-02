package com.effecoria.effect.organic.gene;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Persistent gene grafts on a living host (player or mob). */
public final class GeneProfile {
    public static final StreamCodec<RegistryFriendlyByteBuf, GeneProfile> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.modMask);
                ByteBufCodecs.FLOAT.encode(buf, data.regenAccruedFraction);
                ByteBufCodecs.VAR_LONG.encode(buf, data.appliedGameTime);
                ByteBufCodecs.FLOAT.encode(buf, data.psiBonusApplied);
                ByteBufCodecs.FLOAT.encode(buf, data.phiBonusApplied);
                ByteBufCodecs.VAR_INT.encode(buf, data.mutationCycles);
                ByteBufCodecs.BOOL.encode(buf, data.engineerId != null);
                if (data.engineerId != null) {
                    buf.writeUUID(data.engineerId);
                }
            },
            buf -> {
                GeneProfile data = createDefault();
                data.modMask = ByteBufCodecs.VAR_INT.decode(buf);
                data.regenAccruedFraction = ByteBufCodecs.FLOAT.decode(buf);
                data.appliedGameTime = ByteBufCodecs.VAR_LONG.decode(buf);
                data.psiBonusApplied = ByteBufCodecs.FLOAT.decode(buf);
                data.phiBonusApplied = ByteBufCodecs.FLOAT.decode(buf);
                data.mutationCycles = ByteBufCodecs.VAR_INT.decode(buf);
                if (ByteBufCodecs.BOOL.decode(buf)) {
                    data.engineerId = buf.readUUID();
                }
                return data;
            });

    private int modMask;
    /** Cumulative HP healed as fraction of max — spends hunger/slow every 0.25. */
    private float regenAccruedFraction;
    private long appliedGameTime;
    /** Reversible Φ-heart bonuses applied to PlayerPsiData. */
    private float psiBonusApplied;
    private float phiBonusApplied;
    /** Cell-immortality replication error counter. */
    private int mutationCycles;
    @Nullable
    private UUID engineerId;

    public static GeneProfile createDefault() {
        return new GeneProfile();
    }

    public int modMask() {
        return modMask;
    }

    public EnumSet<GeneMod> mods() {
        return GeneMod.fromMask(modMask);
    }

    public boolean has(GeneMod mod) {
        return (modMask & mod.mask()) != 0;
    }

    public boolean isEmpty() {
        return modMask == 0;
    }

    public float regenAccruedFraction() {
        return regenAccruedFraction;
    }

    public void addRegenAccrued(float fractionOfMax) {
        regenAccruedFraction = Math.max(0f, regenAccruedFraction + fractionOfMax);
    }

    public void consumeRegenThreshold() {
        regenAccruedFraction = Math.max(0f, regenAccruedFraction - 0.25f);
    }

    public long appliedGameTime() {
        return appliedGameTime;
    }

    public float psiBonusApplied() {
        return psiBonusApplied;
    }

    public float phiBonusApplied() {
        return phiBonusApplied;
    }

    public void setChannelBonuses(float psiBonus, float phiBonus) {
        this.psiBonusApplied = psiBonus;
        this.phiBonusApplied = phiBonus;
    }

    public int mutationCycles() {
        return mutationCycles;
    }

    public void addMutationCycle() {
        mutationCycles = Math.min(99, mutationCycles + 1);
    }

    @Nullable
    public UUID engineerId() {
        return engineerId;
    }

    public void setMods(Iterable<GeneMod> mods, @Nullable UUID engineer, long gameTime) {
        this.modMask = GeneMod.toMask(mods);
        this.engineerId = engineer;
        this.appliedGameTime = gameTime;
        this.regenAccruedFraction = 0f;
        // Keep mutationCycles across rewrites — DNA remembers strain.
    }

    public void clear() {
        modMask = 0;
        regenAccruedFraction = 0f;
        appliedGameTime = 0L;
        psiBonusApplied = 0f;
        phiBonusApplied = 0f;
        engineerId = null;
        // mutationCycles intentionally kept
    }

    public List<String> modIds() {
        List<String> ids = new ArrayList<>();
        for (GeneMod mod : mods()) {
            ids.add(mod.id());
        }
        return ids;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        modMask = tag.contains("modMask") ? tag.getInt("modMask") : 0;
        if (modMask == 0 && tag.contains("mods", Tag.TAG_LIST)) {
            ListTag list = tag.getList("mods", Tag.TAG_STRING);
            EnumSet<GeneMod> set = EnumSet.noneOf(GeneMod.class);
            for (Tag entry : list) {
                GeneMod.byId(entry.getAsString()).ifPresent(set::add);
            }
            modMask = GeneMod.toMask(set);
        }
        regenAccruedFraction = tag.contains("regenAccrued") ? tag.getFloat("regenAccrued") : 0f;
        appliedGameTime = tag.contains("appliedAt") ? tag.getLong("appliedAt") : 0L;
        psiBonusApplied = tag.contains("psiBonus") ? tag.getFloat("psiBonus") : 0f;
        phiBonusApplied = tag.contains("phiBonus") ? tag.getFloat("phiBonus") : 0f;
        mutationCycles = tag.contains("mutationCycles") ? tag.getInt("mutationCycles") : 0;
        engineerId = tag.hasUUID("engineer") ? tag.getUUID("engineer") : null;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("modMask", modMask);
        ListTag list = new ListTag();
        for (String id : modIds()) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("mods", list);
        tag.putFloat("regenAccrued", regenAccruedFraction);
        tag.putLong("appliedAt", appliedGameTime);
        tag.putFloat("psiBonus", psiBonusApplied);
        tag.putFloat("phiBonus", phiBonusApplied);
        tag.putInt("mutationCycles", mutationCycles);
        if (engineerId != null) {
            tag.putUUID("engineer", engineerId);
        }
        return tag;
    }
}
