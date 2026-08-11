package com.effecoria.core.alchemy;

import com.effecoria.block.TurretMountBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

/** Aim math for rotating Φ-turrets (yaw/pitch in Minecraft entity degrees). */
public final class TurretAim {
    private TurretAim() {}

    public record Angles(float yawDeg, float pitchDeg) {}

    /** Pivot at the seam between mount and barrel cells. */
    public static Vec3 pivot(BlockPos mountPos, BlockState mountState) {
        Direction out = TurretAssembly.barrelDirection(mountState);
        return Vec3.atCenterOf(mountPos).add(out.getStepX() * 0.5, out.getStepY() * 0.5, out.getStepZ() * 0.5);
    }

    /** World-space aim direction from yaw/pitch (Minecraft entity convention). */
    public static Vec3 directionFromAngles(float yawDeg, float pitchDeg) {
        float yaw = yawDeg * Mth.DEG_TO_RAD;
        float pitch = pitchDeg * Mth.DEG_TO_RAD;
        float cosP = Mth.cos(pitch);
        return new Vec3(-Mth.sin(yaw) * cosP, -Mth.sin(pitch), Mth.cos(yaw) * cosP);
    }

    public static Angles anglesToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(-d.y, horiz) * Mth.RAD_TO_DEG);
        return new Angles(yaw, pitch);
    }

    /** Default rest pose: look along barrel outward axis. */
    public static Angles restPose(BlockState mountState) {
        Direction out = TurretAssembly.barrelDirection(mountState);
        return switch (out) {
            case UP -> new Angles(0f, -90f);
            case DOWN -> new Angles(0f, 90f);
            case SOUTH -> new Angles(0f, 0f);
            case WEST -> new Angles(90f, 0f);
            case NORTH -> new Angles(180f, 0f);
            case EAST -> new Angles(-90f, 0f);
        };
    }

    /** Clamp aim so the barrel cannot shoot into the mount / support. */
    public static Angles clamp(BlockState mountState, float yawDeg, float pitchDeg) {
        AttachFace face = mountState.getValue(TurretMountBlock.FACE);
        return switch (face) {
            case FLOOR -> new Angles(Mth.wrapDegrees(yawDeg), Mth.clamp(pitchDeg, -80f, 10f));
            case CEILING -> new Angles(Mth.wrapDegrees(yawDeg), Mth.clamp(pitchDeg, -10f, 80f));
            case WALL -> {
                Direction out = mountState.getValue(TurretMountBlock.FACING);
                float baseYaw = switch (out) {
                    case SOUTH -> 0f;
                    case WEST -> 90f;
                    case NORTH -> 180f;
                    case EAST -> -90f;
                    default -> 0f;
                };
                float rel = Mth.wrapDegrees(yawDeg - baseYaw);
                rel = Mth.clamp(rel, -75f, 75f);
                yield new Angles(Mth.wrapDegrees(baseYaw + rel), Mth.clamp(pitchDeg, -60f, 60f));
            }
        };
    }

    public static float approachAngle(float current, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxStep) {
            delta = maxStep;
        } else if (delta < -maxStep) {
            delta = -maxStep;
        }
        return current + delta;
    }
}
