package com.effecoria.effect.corruption;

import com.google.gson.JsonObject;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One corruption curse package applied to a living entity. */
public final class CorruptionCurse {
    public enum CureTier {
        COMMON,
        RARE;

        public static CureTier fromString(String raw) {
            if (raw != null && raw.equalsIgnoreCase("rare")) {
                return RARE;
            }
            return COMMON;
        }

        public String id() {
            return this == RARE ? "rare" : "common";
        }
    }

    public record EffectSpec(Holder<MobEffect> effect, int amplifier, int durationTicks, boolean permanent) {
        public MobEffectInstance toInstance() {
            int ticks = permanent ? MobEffectInstance.INFINITE_DURATION : Math.max(1, durationTicks);
            return new MobEffectInstance(effect, ticks, Math.max(0, amplifier), false, true, true);
        }
    }

    private final String id;
    private final UUID casterId;
    private final CureTier cureTier;
    private final int contagionChunks;
    private final float softDotPerSecond;
    private final List<EffectSpec> effects;

    public CorruptionCurse(
            String id,
            UUID casterId,
            CureTier cureTier,
            int contagionChunks,
            float softDotPerSecond,
            List<EffectSpec> effects) {
        this.id = id;
        this.casterId = casterId;
        this.cureTier = cureTier;
        this.contagionChunks = Math.max(0, contagionChunks);
        this.softDotPerSecond = Math.max(0f, softDotPerSecond);
        this.effects = List.copyOf(effects);
    }

    public String id() {
        return id;
    }

    public UUID casterId() {
        return casterId;
    }

    public CureTier cureTier() {
        return cureTier;
    }

    public int contagionChunks() {
        return contagionChunks;
    }

    public float softDotPerSecond() {
        return softDotPerSecond;
    }

    public List<EffectSpec> effects() {
        return effects;
    }

    public double contagionRangeBlocks() {
        return contagionChunks * 16.0;
    }

    public static Builder builder(String id, UUID casterId) {
        return new Builder(id, casterId);
    }

    public static final class Builder {
        private final String id;
        private final UUID casterId;
        private CureTier cureTier = CureTier.COMMON;
        private int contagionChunks;
        private float softDotPerSecond;
        private final List<EffectSpec> effects = new ArrayList<>();

        private Builder(String id, UUID casterId) {
            this.id = id;
            this.casterId = casterId;
        }

        public Builder cureTier(CureTier tier) {
            this.cureTier = tier;
            return this;
        }

        public Builder contagionChunks(int chunks) {
            this.contagionChunks = chunks;
            return this;
        }

        public Builder softDotPerSecond(float dps) {
            this.softDotPerSecond = dps;
            return this;
        }

        public Builder effect(Holder<MobEffect> effect, int amplifier, int durationTicks, boolean permanent) {
            effects.add(new EffectSpec(effect, amplifier, durationTicks, permanent));
            return this;
        }

        public Builder fromParams(JsonObject params) {
            if (params.has("cure_tier")) {
                cureTier = CureTier.fromString(params.get("cure_tier").getAsString());
            }
            if (params.has("contagion_chunks")) {
                contagionChunks = params.get("contagion_chunks").getAsInt();
            }
            if (params.has("soft_dot_per_second")) {
                softDotPerSecond = params.get("soft_dot_per_second").getAsFloat();
            }
            return this;
        }

        public CorruptionCurse build() {
            return new CorruptionCurse(id, casterId, cureTier, contagionChunks, softDotPerSecond, effects);
        }
    }
}
