package com.effecoria.effect.spatial;

import com.effecoria.network.ModNetworking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

/** Server-side Spatial VFX routing: singularity / cuts / ripples / warp field / waves. */
public final class SpatialVfx {
    public enum Bucket {
        SINGULARITY,
        CUT,
        RIPPLE,
        NONE
    }

    /** How a cut is shaped in the world (mirrored by client MODE_* ints). */
    public enum CutMode {
        LINE(0),
        AROUND(1);

        public final int id;

        CutMode(int id) {
            this.id = id;
        }
    }

    /** Only the mini-pulsar keeps a true singularity look. */
    private static final Set<String> SINGULARITY = Set.of("spatial_singularity");

    private static final Set<String> CUT = Set.of(
            "warp_bolt",
            "rift_slash",
            "void_lance",
            "rift_burst",
            "spatial_surge",
            "rift_yank");

    /** Blink / jump / exchange — space ripple, not a black hole. */
    private static final Set<String> RIPPLE = Set.of(
            "blink",
            "far_blink",
            "warp_exchange",
            "absolute_fold",
            "rift_excise",
            "spatial_ward");

    private static final Set<String> CUT_AROUND = Set.of("rift_slash", "rift_burst");

    private SpatialVfx() {}

    public static Bucket bucket(ResourceLocation spellId) {
        if (spellId == null) {
            return Bucket.NONE;
        }
        String path = spellId.getPath();
        if (SINGULARITY.contains(path)) {
            return Bucket.SINGULARITY;
        }
        if (CUT.contains(path)) {
            return Bucket.CUT;
        }
        if (RIPPLE.contains(path)) {
            return Bucket.RIPPLE;
        }
        return Bucket.NONE;
    }

    public static CutMode cutMode(ResourceLocation spellId) {
        if (spellId != null && CUT_AROUND.contains(spellId.getPath())) {
            return CutMode.AROUND;
        }
        return CutMode.LINE;
    }

    public static boolean shouldSingularity(ResourceLocation spellId) {
        return bucket(spellId) == Bucket.SINGULARITY;
    }

    public static boolean shouldCut(ResourceLocation spellId) {
        return bucket(spellId) == Bucket.CUT;
    }

    public static boolean shouldRipple(ResourceLocation spellId) {
        return bucket(spellId) == Bucket.RIPPLE;
    }

    public static void playSingularity(ServerPlayer caster, Vec3 focus, float power) {
        float intensity = Mth.clamp(power / 70f, 0.55f, 1.35f);
        int duration = 24 + Math.round(intensity * 22f);
        PacketDistributor.sendToPlayersNear(
                caster.serverLevel(),
                null,
                focus.x,
                focus.y,
                focus.z,
                48.0,
                new ModNetworking.SingularityFxPayload(focus.x, focus.y, focus.z, intensity, duration));
    }

    public static void playRipple(ServerPlayer caster, Vec3 focus, float power) {
        playRipple(caster.serverLevel(), focus, power);
    }

    public static void playRipple(ServerLevel level, Vec3 focus, float power) {
        float intensity = Mth.clamp(power / 70f, 0.45f, 1.25f);
        int duration = 18 + Math.round(intensity * 10f);
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                focus.x,
                focus.y,
                focus.z,
                48.0,
                new ModNetworking.SpatialRippleFxPayload(focus.x, focus.y, focus.z, intensity, duration));
    }

    /** Short lens bend flash at a projectile deflection / impact point. */
    public static void playLensBend(ServerLevel level, Vec3 focus) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                focus.x,
                focus.y,
                focus.z,
                40.0,
                new ModNetworking.SpatialRippleFxPayload(focus.x, focus.y, focus.z, 0.85f, 14));
    }

    public static void playWarpField(ServerLevel level, Vec3 center, float radius, float power, boolean refresh) {
        float intensity = Mth.clamp(power / 70f, 0.55f, 1.25f);
        int duration = refresh ? 45 : 90;
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                64.0,
                new ModNetworking.SpatialWarpFxPayload(
                        center.x, center.y, center.z, intensity, radius, duration, refresh));
    }

    public static void playGravityWave(ServerLevel level, Vec3 center, float radius, float power) {
        float intensity = Mth.clamp(power / 70f, 0.55f, 1.4f);
        int duration = 22 + Math.round(intensity * 8f);
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                56.0,
                new ModNetworking.SpatialWaveFxPayload(
                        center.x, center.y, center.z, intensity, radius, duration));
    }

    /** Pocket mouth — vertical seam + soft ripple (never a singularity). */
    public static void playPocketOpen(ServerPlayer caster, float power) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 mouth = eye.add(look.scale(1.15));
        Vec3 up = new Vec3(0, 0.85, 0);
        playCut(caster, mouth.subtract(up), mouth.add(up), Mth.clamp(power / 70f, 0.5f, 1.2f), 1, CutMode.LINE);
        playRipple(caster.serverLevel(), mouth, power * 0.9f);
    }

    public static void playCut(
            ServerPlayer caster, Vec3 from, Vec3 to, float intensity, int slashCount, CutMode mode) {
        ServerLevel level = caster.serverLevel();
        float clamped = Mth.clamp(intensity, 0.35f, 1.5f);
        int slashes = Mth.clamp(slashCount, 1, 6);
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                to.x,
                to.y,
                to.z,
                48.0,
                new ModNetworking.SpatialCutFxPayload(
                        from.x, from.y, from.z, to.x, to.y, to.z, clamped, slashes, mode.id));
    }

    public static void playLineFromCaster(ServerPlayer caster, Vec3 tip, float power, int slashCount) {
        Vec3 from = caster.getEyePosition().add(caster.getLookAngle().scale(0.35));
        float intensity = Mth.clamp(power / 70f, 0.45f, 1.35f);
        playCut(caster, from, tip, intensity, slashCount, CutMode.LINE);
    }

    public static void playAround(ServerPlayer caster, Vec3 center, float power, int slashCount) {
        float intensity = Mth.clamp(power / 70f, 0.5f, 1.4f);
        double radius = 1.2 + intensity * 0.8;
        playCut(caster, center.add(-radius, 0, 0), center, intensity, 2, CutMode.AROUND);
    }

    public static void playCutFromCaster(ServerPlayer caster, Vec3 to, float power, int slashCount) {
        playLineFromCaster(caster, to.add(0, 1.0, 0), power, slashCount);
    }
}
