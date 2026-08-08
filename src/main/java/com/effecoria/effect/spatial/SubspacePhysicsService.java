package com.effecoria.effect.spatial;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.EffecoriaMod;
import com.effecoria.armor.EssoniteArmorService;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModFluids;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.world.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Hyperspace survival physics: weightlessness, instant water → Φ-water, no liquid spread,
 * and unprotected-flesh essentialization.
 *
 * <p>Lore spheres / floating blood droplets are intentionally not simulated — fluids simply
 * refuse to flow and water Φ-activates immediately.
 */
public final class SubspacePhysicsService {
    private static final ResourceLocation ZERO_G_ID = EffecoriaMod.id("subspace_zero_g");
    private static final int FLUID_SCRUB_RADIUS = 8;
    private static final int FLUID_SCRUB_HEIGHT = 6;

    private static final Map<UUID, Integer> EXPOSURE_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PRESSURE_WARNED = new ConcurrentHashMap<>();

    private SubspacePhysicsService() {}

    public static void tickPlayer(ServerPlayer player) {
        if (!ModDimensions.isSubspace(player.level())) {
            clearPlayer(player);
            return;
        }

        applyZeroG(player);
        player.fallDistance = 0f;

        if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
            scrubFluidsNear(level, player.blockPosition());
        }

        tickHazard(player);
    }

    /** Keep dropped items / loose entities floating in the Φ-void. */
    public static void tickEntity(Entity entity) {
        if (entity.level().isClientSide() || !ModDimensions.isSubspace(entity.level())) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            if (!item.isNoGravity()) {
                item.setNoGravity(true);
            }
            item.setDeltaMovement(item.getDeltaMovement().scale(0.98));
            item.hurtMarked = true;
            return;
        }
        if (entity instanceof LivingEntity living && !(living instanceof Player)) {
            applyZeroG(living);
            living.fallDistance = 0f;
        }
    }

    public static void onJoinLevel(Entity entity) {
        if (entity.level().isClientSide() || !ModDimensions.isSubspace(entity.level())) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            item.setNoGravity(true);
        } else if (entity instanceof LivingEntity living) {
            applyZeroG(living);
        }
    }

    public static void clearPlayer(Player player) {
        clearZeroG(player);
        UUID id = player.getUUID();
        EXPOSURE_TICKS.remove(id);
        WARNED.remove(id);
        PRESSURE_WARNED.remove(id);
    }

    public static boolean isProtected(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.getAbilities().instabuild || player.isSpectator()) {
                return true;
            }
            if (EssoniteArmorService.hasAny(player)) {
                return true;
            }
        }
        return SpatialAugments.hasCocoon(entity, entity.level().getGameTime());
    }

    /**
     * Convert vanilla water to source Φ-water; reject any non-source fluid placement so liquids
     * never puddle or cascade in hyperspace.
     */
    public static BlockState constrainFluidPlacement(BlockState proposed) {
        FluidState fluid = proposed.getFluidState();
        if (fluid.isEmpty()) {
            return proposed;
        }
        // Flowing cells first — never promote them to source Φ-water (that caused flood/scrub flicker).
        if (!fluid.isSource()) {
            return Blocks.AIR.defaultBlockState();
        }
        if (isVanillaWater(fluid)) {
            return ModBlocks.PHI_WATER.get().defaultBlockState();
        }
        if (isPhiWater(fluid) && proposed.getBlock() instanceof LiquidBlock) {
            return ModBlocks.PHI_WATER.get().defaultBlockState();
        }
        return proposed;
    }

    public static boolean isVanillaWater(FluidState fluid) {
        return fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER);
    }

    public static boolean isVanillaWater(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(Blocks.ICE)
                || state.is(Blocks.FROSTED_ICE)
                || isVanillaWater(state.getFluidState());
    }

    private static boolean isPhiWater(FluidState fluid) {
        Fluid source = ModFluids.PHI_WATER.get();
        Fluid flowing = ModFluids.PHI_WATER_FLOWING.get();
        return fluid.is(source) || fluid.is(flowing);
    }

    public static void scrubFluidsNear(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -FLUID_SCRUB_HEIGHT; dy <= FLUID_SCRUB_HEIGHT; dy++) {
            for (int dx = -FLUID_SCRUB_RADIUS; dx <= FLUID_SCRUB_RADIUS; dx++) {
                for (int dz = -FLUID_SCRUB_RADIUS; dz <= FLUID_SCRUB_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    scrubCell(level, cursor);
                }
            }
        }
    }

    private static void scrubCell(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty() && !state.is(Blocks.ICE) && !state.is(Blocks.FROSTED_ICE)) {
            return;
        }

        // Flowing fragments evaporate — never promote them to Φ-water sources.
        if (!fluid.isEmpty() && !fluid.isSource()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }

        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) || (isVanillaWater(fluid) && fluid.isSource())) {
            level.setBlock(pos, ModBlocks.PHI_WATER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            SubspaceEssentializationService.watch(level, pos, level.getGameTime());
        }
    }

    private static void applyZeroG(LivingEntity living) {
        AttributeInstance gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (gravity.getModifier(ZERO_G_ID) == null) {
            gravity.addTransientModifier(
                    new AttributeModifier(ZERO_G_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void clearZeroG(LivingEntity living) {
        AttributeInstance gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity != null) {
            gravity.removeModifier(ZERO_G_ID);
        }
    }

    private static void tickHazard(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }

        UUID id = player.getUUID();
        if (isProtected(player)) {
            EXPOSURE_TICKS.remove(id);
            WARNED.remove(id);
            tickProtectedPressure(player);
            return;
        }

        int ticks = EXPOSURE_TICKS.getOrDefault(id, 0) + 20;
        EXPOSURE_TICKS.put(id, ticks);

        if (WARNED.putIfAbsent(id, Boolean.TRUE) == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.subspace.unprotected"), true);
        }

        float damage = BalanceConfig.SUBSPACE_EXPOSURE_DAMAGE.get().floatValue();
        int lethalAfter = BalanceConfig.SUBSPACE_PETRIFY_TICKS.get();
        if (ticks >= lethalAfter) {
            damage *= 3f;
            if (ticks == lethalAfter || ticks % 100 == 0) {
                player.displayClientMessage(Component.translatable("message.effecoria.subspace.petrifying"), true);
            }
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false, true));
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 3, false, false, true));
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.WITHER, 40, 1, false, false, true));
        } else if (ticks >= lethalAfter / 2) {
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
        }

        if (damage > 0f) {
            player.hurt(player.damageSources().magic(), damage);
        }

        var data = PsiHelper.get(player);
        if (data.initiated()) {
            ExhaustionService.addExhaustion(data, BalanceConfig.SUBSPACE_EXPOSURE_EXHAUSTION.get().floatValue());
            PsiHelper.set(player, data);
        }
    }

    private static void tickProtectedPressure(ServerPlayer player) {
        // Soft Orkanum pressure through cocoon / Φ-armor — reminder that hyperspace is hostile.
        if (player.tickCount % 600 != 0) {
            return;
        }
        var data = PsiHelper.get(player);
        if (data.initiated()) {
            ExhaustionService.addExhaustion(data, BalanceConfig.SUBSPACE_PRESSURE_EXHAUSTION.get().floatValue());
            PsiHelper.set(player, data);
        }
        if (PRESSURE_WARNED.putIfAbsent(player.getUUID(), Boolean.TRUE) == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.subspace.pressure"), true);
        }
    }
}
