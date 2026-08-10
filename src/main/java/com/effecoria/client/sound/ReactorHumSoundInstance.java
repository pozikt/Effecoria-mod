package com.effecoria.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.effecoria.block.HeartReactorBlock;
import com.effecoria.block.SparkReactorBlock;
import com.effecoria.content.ModBlocks;

/** Looping reactor hum — Spark is a high whine, Heart a deep rumble. */
@OnlyIn(Dist.CLIENT)
public final class ReactorHumSoundInstance extends AbstractTickableSoundInstance {
    public enum Kind {
        SPARK(SoundEvents.BEACON_AMBIENT, 0.28f, 1.92f),
        HEART(SoundEvents.BEACON_AMBIENT, 0.62f, 0.36f);

        private final SoundEvent sound;
        private final float volume;
        private final float pitch;

        Kind(SoundEvent sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private final BlockPos pos;
    private final Kind kind;

    public ReactorHumSoundInstance(BlockPos pos, Kind kind) {
        super(kind.sound, SoundSource.BLOCKS, RandomSource.create());
        this.pos = pos.immutable();
        this.kind = kind;
        this.looping = true;
        this.delay = 0;
        this.volume = kind.volume;
        this.pitch = kind.pitch;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + (kind == Kind.HEART ? 0.5 : 0.5);
        this.z = pos.getZ() + 0.5;
    }

    public BlockPos pos() {
        return pos;
    }

    public Kind kind() {
        return kind;
    }

    public void requestStop() {
        stop();
    }

    @Override
    public void tick() {
        Level level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null || !isStillHumming(level)) {
            stop();
            ReactorHumClient.onStopped(pos);
        }
    }

    private boolean isStillHumming(Level level) {
        BlockState state = level.getBlockState(pos);
        return switch (kind) {
            case SPARK -> state.is(ModBlocks.SPARK_REACTOR.get()) && state.getValue(SparkReactorBlock.LIT);
            case HEART -> state.is(ModBlocks.HEART_REACTOR_CORE.get()) && state.getValue(HeartReactorBlock.LIT);
        };
    }
}
