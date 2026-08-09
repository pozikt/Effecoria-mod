package com.effecoria.core.progression;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Harpy flight: sprint wind-up → three jumps → vanilla elytra glide via {@code startFallFlying},
 * then space flaps for altitude (firework-style boost). Costs hunger.
 */
public final class HarpyFlightService {
    private static final Map<UUID, FlightState> STATE = new ConcurrentHashMap<>();

    private HarpyFlightService() {}

    private static final class FlightState {
        int jumpStreak;
        long lastJumpGameTime;
        boolean gliding;
        long lastFlapGameTime;
        boolean wasOnGround = true;
        boolean jumpHintShown;
    }

    private static FlightState state(ServerPlayer player) {
        return STATE.computeIfAbsent(player.getUUID(), id -> new FlightState());
    }

    public static void clear(ServerPlayer player) {
        FlightState s = STATE.remove(player.getUUID());
        if (s != null && s.gliding && player.isFallFlying()) {
            player.stopFallFlying();
        }
    }

    public static boolean isHarpy(ServerPlayer player) {
        return PsiHelper.get(player).race().orElse(null) == PlayerRace.HARPY;
    }

    public static void tick(ServerPlayer player) {
        if (!isHarpy(player)) {
            clear(player);
            return;
        }

        FlightState s = state(player);
        long now = player.level().getGameTime();

        // Detect grounded jumps without LivingJumpEvent (NeoForge version variance).
        boolean onGround = player.onGround();
        if (s.wasOnGround && !onGround && player.getDeltaMovement().y > 0.05 && !s.gliding) {
            onJump(player, s, now);
        }
        if (onGround && s.gliding) {
            stopGlide(player, s);
        }
        s.wasOnGround = onGround;

        if (!s.gliding) {
            // Expire wind-up streak if too slow / idle too long.
            int window = BalanceConfig.HARPY_JUMP_WINDOW_TICKS.get();
            if (s.jumpStreak > 0 && now - s.lastJumpGameTime > window) {
                s.jumpStreak = 0;
            }
            return;
        }

        if (player.isInWater() || player.isInLava() || player.isPassenger() || player.onClimbable()) {
            stopGlide(player, s);
            return;
        }

        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        float glideEx = BalanceConfig.HARPY_GLIDE_EXHAUSTION.get().floatValue();
        if (glideEx > 0f) {
            player.getFoodData().addExhaustion(glideEx);
        }

        // Starving mid-air: lose lift control but keep falling in glide until land.
        if (player.getFoodData().getFoodLevel() <= 0 && player.tickCount % 40 == 0) {
            player.displayClientMessage(Component.translatable("message.effecoria.harpy.starving"), true);
        }
    }

    private static void onJump(ServerPlayer player, FlightState s, long now) {
        double minSpeed = BalanceConfig.HARPY_MIN_SPEED.get();
        double horiz = player.getDeltaMovement().horizontalDistance();
        boolean sprinting = player.isSprinting();
        int window = BalanceConfig.HARPY_JUMP_WINDOW_TICKS.get();

        boolean windupOk = sprinting && horiz >= minSpeed;
        if (!windupOk || (s.jumpStreak > 0 && now - s.lastJumpGameTime > window)) {
            s.jumpStreak = windupOk ? 1 : 0;
            s.lastJumpGameTime = now;
            if (windupOk && !s.jumpHintShown) {
                player.displayClientMessage(Component.translatable("message.effecoria.harpy.windup"), true);
                s.jumpHintShown = true;
            }
            return;
        }

        s.jumpStreak++;
        s.lastJumpGameTime = now;

        if (s.jumpStreak == 1 || s.jumpStreak == 2) {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.harpy.jump_count", s.jumpStreak), true);
            return;
        }

        if (s.jumpStreak >= 3) {
            launch(player, s);
        }
    }

    private static void launch(ServerPlayer player, FlightState s) {
        s.jumpStreak = 0;
        s.gliding = true;
        s.jumpHintShown = false;

        Vec3 look = player.getLookAngle();
        double forward = BalanceConfig.HARPY_LAUNCH_FORWARD.get();
        double up = BalanceConfig.HARPY_LAUNCH_UP.get();
        double hx = look.x;
        double hz = look.z;
        double horizLen = Math.sqrt(hx * hx + hz * hz);
        if (horizLen < 1.0E-4) {
            float yaw = player.getYRot() * ((float) Math.PI / 180f);
            hx = -Mth.sin(yaw);
            hz = Mth.cos(yaw);
            horizLen = 1.0;
        }
        hx /= horizLen;
        hz /= horizLen;

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(
                motion.x * 0.2 + hx * forward,
                Math.max(motion.y, 0) * 0.2 + up,
                motion.z * 0.2 + hz * forward);
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.startFallFlying();

        float launchEx = BalanceConfig.HARPY_FLAP_EXHAUSTION.get().floatValue() * 0.5f;
        if (launchEx > 0f) {
            player.getFoodData().addExhaustion(launchEx);
        }
        player.displayClientMessage(Component.translatable("message.effecoria.harpy.launch"), true);
    }

    /** Space flap while gliding — firework-style boost with hunger cost. */
    public static boolean tryFlap(ServerPlayer player) {
        if (!isHarpy(player)) {
            return false;
        }
        FlightState s = state(player);
        if (!s.gliding && !player.isFallFlying()) {
            return false;
        }
        if (player.onGround() || player.isInWater()) {
            return false;
        }

        long now = player.level().getGameTime();
        int cooldown = BalanceConfig.HARPY_FLAP_COOLDOWN_TICKS.get();
        if (now - s.lastFlapGameTime < cooldown) {
            return false;
        }
        if (player.getFoodData().getFoodLevel() <= 0) {
            player.displayClientMessage(Component.translatable("message.effecoria.harpy.starving"), true);
            return false;
        }

        s.gliding = true;
        s.lastFlapGameTime = now;
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        // Climb flap: always gain altitude. Do NOT use look-Y firework boost —
        // elytra pitch is often downward and that made space feel like diving.
        float strength = BalanceConfig.HARPY_FLAP_STRENGTH.get().floatValue();
        applyClimbFlap(player, strength);

        float flapEx = BalanceConfig.HARPY_FLAP_EXHAUSTION.get().floatValue();
        if (flapEx > 0f) {
            player.getFoodData().addExhaustion(flapEx);
        }
        return true;
    }

    /** Horizontal look thrust + guaranteed upward impulse. */
    private static void applyClimbFlap(ServerPlayer player, float strength) {
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        double hx = look.x;
        double hz = look.z;
        double horiz = Math.sqrt(hx * hx + hz * hz);
        if (horiz < 1.0E-4) {
            float yaw = player.getYRot() * ((float) Math.PI / 180f);
            hx = -Mth.sin(yaw);
            hz = Mth.cos(yaw);
            horiz = 1.0;
        }
        hx /= horiz;
        hz /= horiz;

        double s = Math.max(0.6, strength);
        double forward = 0.45 * s;
        double climb = 0.48 + 0.22 * s;

        player.setDeltaMovement(
                motion.x * 0.55 + hx * forward,
                Math.max(0.12, motion.y * 0.25) + climb,
                motion.z * 0.55 + hz * forward);
        player.hurtMarked = true;
        player.hasImpulse = true;
    }

    private static void stopGlide(ServerPlayer player, FlightState s) {
        s.gliding = false;
        s.jumpStreak = 0;
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }

    public static void onFallLanding(ServerPlayer player, net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if (!isHarpy(player)) {
            return;
        }
        FlightState s = STATE.get(player.getUUID());
        if (s != null && s.gliding) {
            // Soft landing after glide — stack with race −50% fall damage.
            event.setDistance(event.getDistance() * 0.35f);
            stopGlide(player, s);
        }
    }
}
