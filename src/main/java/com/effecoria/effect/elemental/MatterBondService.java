package com.effecoria.effect.elemental;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Environmental matter casting MVP — bond to water/ice, channel a defensive sheet, throw a form.
 * Instinct for Elemental school; see docs/MAGIC_PLAN.md.
 */
public final class MatterBondService {
    public enum Kind {
        WATER,
        ICE
    }

    public record Bond(BlockPos source, Kind kind, float strength) {}

    private static final Map<UUID, Bond> BONDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CHANNELING = new ConcurrentHashMap<>();
    private static final Map<UUID, List<BlockPos>> ACTIVE_WALLS = new ConcurrentHashMap<>();

    private MatterBondService() {}

    public static boolean canUse(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated() || data.school() != MagicSchool.ELEMENTAL) {
            return false;
        }
        return data.breathingMastery() >= BalanceConfig.MATTER_CAST_MIN_MASTERY.get().floatValue();
    }

    public static Bond getBond(UUID id) {
        return BONDS.get(id);
    }

    public static void clear(UUID id) {
        BONDS.remove(id);
        CHANNELING.remove(id);
        List<BlockPos> wall = ACTIVE_WALLS.remove(id);
        // walls expire via ElementalBlockService timers
        if (wall != null) {
            wall.clear();
        }
    }

    public static void tryLink(ServerPlayer player) {
        if (!canUse(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_need_elemental"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockHitResult hit = rayMatter(player, BalanceConfig.MATTER_BOND_RANGE.get());
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_no_source"), true);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Kind kind = classify(level, pos);
        if (kind == null) {
            // try fluid inside / adjacent
            BlockPos fluidPos = pos.relative(hit.getDirection());
            kind = classify(level, fluidPos);
            if (kind != null) {
                pos = fluidPos;
            }
        }
        if (kind == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_no_source"), true);
            return;
        }
        float strength = measureStrength(level, pos, kind);
        Bond bond = new Bond(pos.immutable(), kind, strength);
        BONDS.put(player.getUUID(), bond);
        syncBond(player, bond);
        spawnBondFx(level, pos, kind);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_bonded", kind.name().toLowerCase()), true);
    }

    public static void setChanneling(ServerPlayer player, boolean active) {
        if (!canUse(player)) {
            return;
        }
        if (!active) {
            CHANNELING.remove(player.getUUID());
            return;
        }
        Bond bond = BONDS.get(player.getUUID());
        if (bond == null || !bondValid(player, bond)) {
            CHANNELING.remove(player.getUUID());
            clearBondOnly(player);
            return;
        }
        CHANNELING.put(player.getUUID(), true);
    }

    public static void tryThrow(ServerPlayer player) {
        if (!canUse(player)) {
            return;
        }
        Bond bond = BONDS.get(player.getUUID());
        if (bond == null || !bondValid(player, bond)) {
            clearBondOnly(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_no_bond"), true);
            return;
        }
        float cost = BalanceConfig.MATTER_THROW_PSI.get().floatValue();
        PlayerPsiData data = PsiHelper.get(player);
        if (data.currentPsi() < cost) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_no_psi"), true);
            return;
        }
        data.setCurrentPsi(data.currentPsi() - cost);
        player.syncData(ModAttachments.PSI.get());

        ServerLevel level = player.serverLevel();
        Snowball shard = new Snowball(level, player);
        shard.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        Vec3 look = player.getLookAngle();
        shard.shoot(look.x, look.y, look.z, 1.35f, 1.5f);
        shard.getPersistentData().putBoolean(ElementalTags.PROJECTILE, true);
        if (bond.kind() == Kind.ICE) {
            shard.getPersistentData().putString(ElementalTags.KIND, ElementalTags.KIND_MATTER_ICE);
            shard.getPersistentData().putFloat(ElementalTags.POWER, 4f + bond.strength() * 0.5f);
        } else {
            shard.getPersistentData().putString(ElementalTags.KIND, ElementalTags.KIND_MATTER_WATER);
            shard.getPersistentData().putFloat(ElementalTags.POWER, 2.5f + bond.strength() * 0.35f);
        }
        level.addFreshEntity(shard);

        float next = Math.max(0f, bond.strength() - BalanceConfig.MATTER_THROW_SOURCE_DRAIN.get().floatValue());
        if (next <= 0.05f) {
            clearBondOnly(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.effecoria.matter_source_spent"), true);
        } else {
            Bond updated = new Bond(bond.source(), bond.kind(), next);
            BONDS.put(player.getUUID(), updated);
            syncBond(player, updated);
            drainSourceBlock(level, bond);
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        Bond bond = BONDS.get(player.getUUID());
        if (bond != null && !bondValid(player, bond)) {
            clearBondOnly(player);
            return;
        }
        if (!Boolean.TRUE.equals(CHANNELING.get(player.getUUID())) || bond == null) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(player);
        float drain = BalanceConfig.MATTER_CHANNEL_PSI_PER_TICK.get().floatValue();
        if (data.currentPsi() < drain) {
            CHANNELING.remove(player.getUUID());
            return;
        }
        data.setCurrentPsi(data.currentPsi() - drain);
        if (player.tickCount % 10 == 0) {
            player.syncData(ModAttachments.PSI.get());
            raiseWall(player, bond);
            float next = Math.max(0f, bond.strength() - 0.04f);
            if (next <= 0.05f) {
                clearBondOnly(player);
            } else {
                Bond updated = new Bond(bond.source(), bond.kind(), next);
                BONDS.put(player.getUUID(), updated);
            }
        }
    }

    private static void raiseWall(ServerPlayer player, Bond bond) {
        ServerLevel level = player.serverLevel();
        Direction facing = player.getDirection();
        Direction left = facing.getCounterClockWise();
        BlockPos feet = player.blockPosition().relative(facing, 2);
        BlockState sheet = bond.kind() == Kind.ICE ? Blocks.ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
        if (bond.kind() == Kind.WATER) {
            sheet = Blocks.PACKED_ICE.defaultBlockState(); // solid water-sheet stand-in
        }
        List<BlockPos> placed = new ArrayList<>();
        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h <= 2; h++) {
                BlockPos pos = feet.relative(left, w).above(h);
                if (ElementalBlockService.placeTemporary(level, pos, sheet, 40)) {
                    placed.add(pos.immutable());
                    level.sendParticles(
                            bond.kind() == Kind.ICE ? ModParticleTypes.ICE_CRYSTAL.get() : ModParticleTypes.WATER_SPLASH.get(),
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            2,
                            0.1,
                            0.1,
                            0.1,
                            0.01);
                }
            }
        }
        ACTIVE_WALLS.put(player.getUUID(), placed);
    }

    private static void drainSourceBlock(ServerLevel level, Bond bond) {
        BlockState state = level.getBlockState(bond.source());
        FluidState fluid = level.getFluidState(bond.source());
        if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
            level.setBlock(bond.source(), Blocks.AIR.defaultBlockState(), 3);
        } else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE)) {
            if (level.random.nextFloat() < 0.35f) {
                level.setBlock(bond.source(), Blocks.WATER.defaultBlockState(), 3);
            }
        } else if (state.is(BlockTags.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            level.destroyBlock(bond.source(), false);
        }
    }

    private static boolean bondValid(ServerPlayer player, Bond bond) {
        double range = BalanceConfig.MATTER_BOND_RANGE.get();
        if (player.distanceToSqr(Vec3.atCenterOf(bond.source())) > range * range) {
            return false;
        }
        return classify(player.serverLevel(), bond.source()) == bond.kind();
    }

    private static void clearBondOnly(ServerPlayer player) {
        BONDS.remove(player.getUUID());
        CHANNELING.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new ModNetworking.MatterBondSyncPayload(false, 0, 0, 0, "none", 0f));
    }

    private static void syncBond(ServerPlayer player, Bond bond) {
        PacketDistributor.sendToPlayer(
                player,
                new ModNetworking.MatterBondSyncPayload(
                        true,
                        bond.source().getX(),
                        bond.source().getY(),
                        bond.source().getZ(),
                        bond.kind().name().toLowerCase(),
                        bond.strength()));
    }

    private static void spawnBondFx(ServerLevel level, BlockPos pos, Kind kind) {
        level.sendParticles(
                kind == Kind.ICE ? ModParticleTypes.ICE_CRYSTAL.get() : ModParticleTypes.WATER_WAVE.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                12,
                0.35,
                0.35,
                0.35,
                0.02);
    }

    private static BlockHitResult rayMatter(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
    }

    private static Kind classify(ServerLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (fluid.is(FluidTags.WATER)) {
            return Kind.WATER;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE)
                || state.is(BlockTags.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)) {
            return Kind.ICE;
        }
        return null;
    }

    private static float measureStrength(ServerLevel level, BlockPos origin, Kind kind) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (classify(level, cursor) == kind) {
                        count++;
                    }
                }
            }
        }
        return Math.min(8f, 1f + count * 0.25f);
    }

    public static void tickLevel(ServerLevel level) {
        // no global cleanup needed beyond player ticks
    }

    /** Apply wet / brittle effects for matter projectiles. */
    public static void applyMatterHit(LivingHit hit) {
        hit.target().hurt(hit.source(), hit.damage());
        if (hit.ice()) {
            hit.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            ElementalEffects.spawnIceParticles(hit.level(), hit.target().position().add(0, 1, 0));
        } else {
            hit.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            hit.level().sendParticles(
                    ModParticleTypes.WATER_SPLASH.get(),
                    hit.target().getX(),
                    hit.target().getY() + 1,
                    hit.target().getZ(),
                    8,
                    0.2,
                    0.2,
                    0.2,
                    0.02);
        }
    }

    public record LivingHit(
            ServerLevel level,
            net.minecraft.world.entity.LivingEntity target,
            net.minecraft.world.damagesource.DamageSource source,
            float damage,
            boolean ice) {}
}
