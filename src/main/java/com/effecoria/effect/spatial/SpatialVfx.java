package com.effecoria.effect.spatial;

import com.effecoria.network.ModNetworking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

/** Server-side Spatial VFX routing: singularity wells vs dimensional cuts. */
public final class SpatialVfx {
    public enum Bucket {
        SINGULARITY,
        CUT,
        NONE
    }

    /** How a cut is shaped in the world (mirrored by client MODE_* ints). */
    public enum CutMode {
        /** Seam from A → B (caster→target, yank path, forward surge). */
        LINE(0),
        /** Radial Judgement cuts through a focus. */
        AROUND(1);

        public final int id;

        CutMode(int id) {
            this.id = id;
        }
    }

    private static final Set<String> SINGULARITY = Set.of(
            "gravity_snare",
            "gravity_well",
            "gravity_field",
            "spatial_singularity",
            "absolute_fold",
            "subspace_voyage",
            "rift_excise");

    private static final Set<String> CUT = Set.of(
            "warp_bolt",
            "rift_slash",
            "void_lance",
            "fold_repulse",
            "rift_burst",
            "spatial_surge",
            "rift_yank");

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

    /** Line cut from caster eyes to a world tip (everyone nearby sees the seam). */
    public static void playLineFromCaster(ServerPlayer caster, Vec3 tip, float power, int slashCount) {
        Vec3 from = caster.getEyePosition().add(caster.getLookAngle().scale(0.35));
        float intensity = Mth.clamp(power / 70f, 0.45f, 1.35f);
        playCut(caster, from, tip, intensity, slashCount, CutMode.LINE);
    }

    /** Radial cuts centered on a world focus. */
    public static void playAround(ServerPlayer caster, Vec3 center, float power, int slashCount) {
        float intensity = Mth.clamp(power / 70f, 0.5f, 1.4f);
        double radius = 1.2 + intensity * 0.8;
        playCut(caster, center.add(-radius, 0, 0), center, intensity, slashCount, CutMode.AROUND);
    }

    /** Legacy helper: line to feet+1. Prefer {@link #playLineFromCaster} / {@link #playAround}. */
    public static void playCutFromCaster(ServerPlayer caster, Vec3 to, float power, int slashCount) {
        playLineFromCaster(caster, to.add(0, 1.0, 0), power, slashCount);
    }
}
