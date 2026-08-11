package com.effecoria.client.sound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Starts/stops looping reactor hums on the client. */
@OnlyIn(Dist.CLIENT)
public final class ReactorHumClient {
    private static final Map<BlockPos, ReactorHumSoundInstance> ACTIVE = new HashMap<>();

    private ReactorHumClient() {}

    public static void ensureSpark(BlockPos pos) {
        ensure(pos, ReactorHumSoundInstance.Kind.SPARK);
    }

    public static void ensureHeart(BlockPos pos) {
        ensure(pos, ReactorHumSoundInstance.Kind.HEART);
    }

    public static void ensureForge(BlockPos pos) {
        ensure(pos, ReactorHumSoundInstance.Kind.FORGE);
    }

    private static void ensure(BlockPos pos, ReactorHumSoundInstance.Kind kind) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) {
            return;
        }
        BlockPos key = pos.immutable();
        ReactorHumSoundInstance existing = ACTIVE.get(key);
        if (existing != null && !existing.isStopped() && existing.kind() == kind) {
            return;
        }
        if (existing != null) {
            existing.requestStop();
            ACTIVE.remove(key);
        }
        ReactorHumSoundInstance next = new ReactorHumSoundInstance(key, kind);
        ACTIVE.put(key, next);
        mc.getSoundManager().play(next);
    }

    static void onStopped(BlockPos pos) {
        ACTIVE.remove(pos);
    }

    /** Drop stale entries if the sound manager already discarded them. */
    public static void purgeStopped() {
        Iterator<Map.Entry<BlockPos, ReactorHumSoundInstance>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, ReactorHumSoundInstance> e = it.next();
            if (e.getValue().isStopped()) {
                it.remove();
            }
        }
    }
}
