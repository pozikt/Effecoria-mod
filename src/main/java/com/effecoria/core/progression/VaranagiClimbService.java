package com.effecoria.core.progression;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Varanagi wall/tree climbing: vine-like grab on solid faces and trunks, plus sprint
 * scramble dashes up the same surfaces.
 */
public final class VaranagiClimbService {
    private static final Map<UUID, ClimbState> STATE = new ConcurrentHashMap<>();

    private VaranagiClimbService() {}

    private static final class ClimbState {
        boolean climbing;
        long lastClimbGameTime;
        long lastDashGameTime;
        /** Soften fall after leaving a wall. */
        int fallGraceTicks;
        /** Client-synced jump key (LivingEntity.jumping is protected). */
        boolean jumpHeld;
    }

    private static ClimbState state(Player player) {
        return STATE.computeIfAbsent(player.getUUID(), id -> new ClimbState());
    }

    public static void clear(Player player) {
        STATE.remove(player.getUUID());
    }

    public static boolean isVaranagi(Player player) {
        return PsiHelper.get(player).race().orElse(null) == PlayerRace.VARANAGI;
    }

    public static boolean isClimbing(Player player) {
        ClimbState s = STATE.get(player.getUUID());
        return s != null && s.climbing;
    }

    public static void setJumpHeld(Player player, boolean held) {
        if (!isVaranagi(player)) {
            return;
        }
        state(player).jumpHeld = held;
    }

    /**
     * Shared tick for client prediction and server authority.
     * Vine-like climb when pressed into a wall/trunk; native ladders/vines stay vanilla.
     */
    public static void tick(Player player) {
        if (!isVaranagi(player) || player.isSpectator() || player.getAbilities().flying) {
            ClimbState s = STATE.get(player.getUUID());
            if (s != null) {
                s.climbing = false;
            }
            return;
        }
        if (player.isPassenger() || player.isInWater() || player.isInLava() || player.onGround()) {
            ClimbState s = state(player);
            if (player.onGround()) {
                s.climbing = false;
                s.fallGraceTicks = 0;
            }
            return;
        }

        ClimbState s = state(player);
        boolean natural = player.onClimbable();
        boolean againstWall = !natural && isAgainstClimbSurface(player);
        boolean climbing = natural || againstWall;

        if (!climbing) {
            if (s.climbing) {
                s.fallGraceTicks = BalanceConfig.VARANAGI_CLIMB_FALL_GRACE_TICKS.get();
            }
            s.climbing = false;
            if (s.fallGraceTicks > 0) {
                s.fallGraceTicks--;
                player.resetFallDistance();
            }
            return;
        }

        s.climbing = true;
        s.lastClimbGameTime = player.level().getGameTime();
        s.fallGraceTicks = 0;
        player.resetFallDistance();

        // Native vines/ladders already move the player — only fake wall/tree needs physics.
        // Sprint+jump is reserved for scramble dashes (see tryDash).
        if (againstWall) {
            applyVineLikeClimb(player);
        }
    }

    /** Sprint+jump scramble while on a climbable wall, tree, ladder, or vine. */
    public static boolean tryDash(ServerPlayer player) {
        if (!isVaranagi(player) || player.isSpectator() || player.getAbilities().flying) {
            return false;
        }
        if (player.onGround() || player.isInWater() || player.isPassenger()) {
            return false;
        }
        if (!player.onClimbable() && !isAgainstClimbSurface(player) && !isNearClimbSurface(player, 0.35)) {
            return false;
        }
        if (!player.isSprinting()) {
            return false;
        }

        ClimbState s = state(player);
        long now = player.level().getGameTime();
        int cooldown = BalanceConfig.VARANAGI_CLIMB_DASH_COOLDOWN_TICKS.get();
        if (now - s.lastDashGameTime < cooldown) {
            return false;
        }

        s.lastDashGameTime = now;
        s.climbing = true;
        applyDash(player);

        float exhaustion = BalanceConfig.VARANAGI_CLIMB_DASH_EXHAUSTION.get().floatValue();
        if (exhaustion > 0f) {
            player.getFoodData().addExhaustion(exhaustion);
        }
        player.resetFallDistance();
        return true;
    }

    public static void onFall(ServerPlayer player, net.neoforged.neoforge.event.entity.living.LivingFallEvent event) {
        if (!isVaranagi(player)) {
            return;
        }
        ClimbState s = STATE.get(player.getUUID());
        if (s == null) {
            return;
        }
        if (s.climbing || s.fallGraceTicks > 0 || player.level().getGameTime() - s.lastClimbGameTime < 8) {
            event.setDistance(0f);
            s.climbing = false;
            s.fallGraceTicks = 0;
        }
    }

    private static void applyVineLikeClimb(Player player) {
        double speed = BalanceConfig.VARANAGI_CLIMB_SPEED.get();
        double slide = BalanceConfig.VARANAGI_CLIMB_SLIDE.get();
        Vec3 motion = player.getDeltaMovement();
        // Don't fight a scramble dash mid-impulse.
        ClimbState s = state(player);
        if (player.level().getGameTime() - s.lastDashGameTime <= 3) {
            player.resetFallDistance();
            return;
        }

        double y;
        if (s.jumpHeld && !player.isSprinting()) {
            // Vine climb: hold jump (without sprint) to ascend.
            y = speed;
        } else if (player.isShiftKeyDown()) {
            y = -speed;
        } else {
            // Vine hang: slow slip down, cancel freefall.
            y = Math.max(motion.y, -slide);
            if (y > 0) {
                y *= 0.6;
            }
        }

        // Keep a little into-wall pressure so horizontalCollision stays true.
        double damp = 0.85;
        player.setDeltaMovement(motion.x * damp, y, motion.z * damp);
        player.hasImpulse = true;
        if (player instanceof ServerPlayer) {
            player.hurtMarked = true;
        }
    }

    private static void applyDash(ServerPlayer player) {
        double up = BalanceConfig.VARANAGI_CLIMB_DASH_UP.get();
        double along = BalanceConfig.VARANAGI_CLIMB_DASH_ALONG.get();
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

        player.setDeltaMovement(
                motion.x * 0.25 + hx * along,
                Math.max(motion.y, 0.0) * 0.2 + up,
                motion.z * 0.25 + hz * along);
        player.hurtMarked = true;
        player.hasImpulse = true;
    }

    /** Pressed into a climb surface (wall face or tree). */
    public static boolean isAgainstClimbSurface(Player player) {
        return player.horizontalCollision && isNearClimbSurface(player, 0.2);
    }

    public static boolean isNearClimbSurface(Player player, double pad) {
        BlockGetter level = player.level();
        AABB bb = player.getBoundingBox();
        double[] ys = {bb.minY + 0.05, bb.minY + player.getBbHeight() * 0.45, Math.max(bb.minY + 0.2, bb.maxY - 0.15)};
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        for (double y : ys) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                double x = dir.getStepX() != 0
                        ? (dir.getStepX() > 0 ? bb.maxX + pad : bb.minX - pad)
                        : (bb.minX + bb.maxX) * 0.5;
                double z = dir.getStepZ() != 0
                        ? (dir.getStepZ() > 0 ? bb.maxZ + pad : bb.minZ - pad)
                        : (bb.minZ + bb.maxZ) * 0.5;
                mut.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
                BlockState state = level.getBlockState(mut);
                if (isTreeBlock(state) || isWallFace(state, level, mut, dir.getOpposite())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTreeBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    /** Solid wall face the player can cling to (as if vines were on it). */
    private static boolean isWallFace(BlockState state, BlockGetter level, BlockPos pos, Direction faceTowardPlayer) {
        if (state.isAir() || isTreeBlock(state)) {
            return false;
        }
        // Skip blocks that are already true climbables — vanilla handles those.
        if (state.is(BlockTags.CLIMBABLE)) {
            return false;
        }
        return state.isFaceSturdy(level, pos, faceTowardPlayer);
    }
}
