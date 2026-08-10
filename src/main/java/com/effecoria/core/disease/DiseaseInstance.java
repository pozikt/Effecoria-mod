package com.effecoria.core.disease;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/** One active disease on a host. */
public final class DiseaseInstance {
    public static final StreamCodec<RegistryFriendlyByteBuf, DiseaseInstance> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.stage);
                ByteBufCodecs.VAR_INT.encode(buf, data.ticksActive);
                ByteBufCodecs.BOOL.encode(buf, data.echoHostile);
            },
            buf -> {
                DiseaseInstance data = new DiseaseInstance();
                data.stage = ByteBufCodecs.VAR_INT.decode(buf);
                data.ticksActive = ByteBufCodecs.VAR_INT.decode(buf);
                data.echoHostile = ByteBufCodecs.BOOL.decode(buf);
                return data;
            });

    private int stage = 1;
    private int ticksActive;
    private boolean echoHostile;

    public static DiseaseInstance of(int stage) {
        DiseaseInstance inst = new DiseaseInstance();
        inst.stage = Math.max(1, stage);
        return inst;
    }

    public int stage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = Math.max(1, stage);
    }

    public int ticksActive() {
        return ticksActive;
    }

    public void addTicks(int ticks) {
        this.ticksActive = Math.max(0, ticksActive + ticks);
    }

    public void setTicksActive(int ticks) {
        this.ticksActive = Math.max(0, ticks);
    }

    public boolean echoHostile() {
        return echoHostile;
    }

    public void setEchoHostile(boolean echoHostile) {
        this.echoHostile = echoHostile;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stage", stage);
        tag.putInt("ticks", ticksActive);
        tag.putBoolean("echoHostile", echoHostile);
        return tag;
    }

    public static DiseaseInstance load(CompoundTag tag) {
        DiseaseInstance inst = new DiseaseInstance();
        inst.stage = Mth.clamp(tag.getInt("stage"), 1, 8);
        inst.ticksActive = Math.max(0, tag.getInt("ticks"));
        inst.echoHostile = tag.getBoolean("echoHostile");
        return inst;
    }

    public DiseaseInstance copy() {
        DiseaseInstance copy = new DiseaseInstance();
        copy.stage = stage;
        copy.ticksActive = ticksActive;
        copy.echoHostile = echoHostile;
        return copy;
    }
}
