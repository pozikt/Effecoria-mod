package com.effecoria.block;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Server-driven Φ-geyser cycle: dormant → precursor → eruption → cooldown.
 */
public final class PhiGeyserBlockEntity extends BlockEntity {
    private int phaseTicks;
    private int dormantTarget = 6000;
    private final Map<UUID, Integer> columnExposure = new HashMap<>();

    public PhiGeyserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_GEYSER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PhiGeyserBlockEntity geyser) {
        if (level.isClientSide()) {
            return;
        }
        geyser.serverTick((ServerLevel) level, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        PhiGeyserPhase phase = state.getValue(PhiGeyserBlock.PHASE);
        phaseTicks++;

        switch (phase) {
            case DORMANT -> {
                affectNear(level, pos, false);
                ambientFx(level, pos, 0.25f);
                if (phaseTicks >= dormantTarget) {
                    enter(level, pos, state, PhiGeyserPhase.PRECURSOR);
                }
            }
            case PRECURSOR -> {
                affectNear(level, pos, false);
                ambientFx(level, pos, 0.7f);
                if (phaseTicks % 20 == 0) {
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9f, 0.4f);
                }
                if (phaseTicks >= BalanceConfig.GEYSER_PRECURSOR_TICKS.get()) {
                    enter(level, pos, state, PhiGeyserPhase.ERUPTING);
                    level.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.4f, 0.55f);
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 0.7f);
                }
            }
            case ERUPTING -> {
                eruptFx(level, pos);
                affectNear(level, pos, true);
                affectColumn(level, pos);
                if (phaseTicks >= BalanceConfig.GEYSER_ERUPTION_TICKS.get()) {
                    settleAftermath(level, pos);
                    enter(level, pos, state, PhiGeyserPhase.COOLDOWN);
                }
            }
            case COOLDOWN -> {
                affectNear(level, pos, false);
                ambientFx(level, pos, 0.35f);
                if (phaseTicks >= BalanceConfig.GEYSER_COOLDOWN_TICKS.get()) {
                    enter(level, pos, state, PhiGeyserPhase.DORMANT);
                    rollNextDormant(level);
                }
            }
        }
    }

    public boolean tryForceErupt(ServerLevel level, BlockState state) {
        PhiGeyserPhase phase = state.getValue(PhiGeyserBlock.PHASE);
        if (phase == PhiGeyserPhase.ERUPTING || phase == PhiGeyserPhase.PRECURSOR) {
            return false;
        }
        enter(level, worldPosition, state, PhiGeyserPhase.PRECURSOR);
        return true;
    }

    public void onTouchCrack(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (data.initiated()) {
            ExhaustionService.addExhaustion(data, BalanceConfig.GEYSER_TOUCH_EXHAUSTION.get().floatValue());
            PsiHelper.set(player, data);
            player.hurt(player.damageSources().magic(), 2f);
        } else {
            player.hurt(player.damageSources().magic(), 4f);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
        }
    }

    private void enter(ServerLevel level, BlockPos pos, BlockState state, PhiGeyserPhase next) {
        phaseTicks = 0;
        columnExposure.clear();
        BlockState updated = state.setValue(PhiGeyserBlock.PHASE, next);
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        setChanged();
        level.sendBlockUpdated(pos, updated, updated, Block.UPDATE_CLIENTS);
    }

    private void rollNextDormant(ServerLevel level) {
        int min = BalanceConfig.GEYSER_DORMANT_MIN_TICKS.get();
        int max = Math.max(min + 1, BalanceConfig.GEYSER_DORMANT_MAX_TICKS.get());
        float moon = 1f + (Math.abs(level.getMoonPhase() - 4) / 4f) * 0.45f;
        dormantTarget = Mth.ceil((min + level.random.nextInt(max - min)) * moon);
    }

    private void affectNear(ServerLevel level, BlockPos pos, boolean erupting) {
        double radius = BalanceConfig.GEYSER_NEAR_RADIUS.get();
        AABB box = new AABB(pos).inflate(radius, radius * 0.6, radius);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            PlayerPsiData data = PsiHelper.get(player);
            if (data.initiated()) {
                float regenBoost = erupting
                        ? BalanceConfig.GEYSER_NEAR_REGEN_ERUPT.get().floatValue()
                        : BalanceConfig.GEYSER_NEAR_REGEN.get().floatValue();
                // Applied every tick while near — scale to ~per-second feel via /20 of a chunk of max.
                float gain = data.maxPsi() * (regenBoost - 1f) * 0.004f;
                data.setCurrentPsi(data.currentPsi() + gain);
                // Passive cell charge near geyser.
                ItemStack cell = ItemStack.EMPTY;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.is(ModItems.PHI_CELL.get())) {
                        cell = stack;
                        break;
                    }
                }
                if (cell.isEmpty() && player.getOffhandItem().is(ModItems.PHI_CELL.get())) {
                    cell = player.getOffhandItem();
                }
                if (!cell.isEmpty() && level.getGameTime() % 20 == 0) {
                    PhiHarnessItems.setCellCharge(cell, PhiHarnessItems.cellCharge(cell) + 0.02f);
                }
                PsiHelper.set(player, data);
            } else if (level.getGameTime() % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, false, false, true));
                player.hurt(player.damageSources().magic(), 1f);
            }
        }
        // Φ pollution: convert nearby dirt/stone while active
        if (level.getGameTime() % 30 == 0) {
            RandomSource random = level.random;
            BlockPos target = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            BlockState neighbor = level.getBlockState(target);
            BlockState converted = PhiSpreadLogic.convert(neighbor);
            if (converted != null && random.nextFloat() < (erupting ? 0.55f : 0.2f)) {
                level.setBlockAndUpdate(target, converted);
            }
        }
    }

    private void affectColumn(ServerLevel level, BlockPos pos) {
        double height = BalanceConfig.GEYSER_COLUMN_HEIGHT.get();
        AABB column = new AABB(
                pos.getX() + 0.15,
                pos.getY(),
                pos.getZ() + 0.15,
                pos.getX() + 0.85,
                pos.getY() + height,
                pos.getZ() + 0.85);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, column)) {
            PlayerPsiData data = PsiHelper.get(player);
            if (!data.initiated()) {
                player.hurt(player.damageSources().magic(), 40f);
                continue;
            }
            data.setCurrentPsi(data.maxPsi());
            int exposure = columnExposure.merge(player.getUUID(), 1, Integer::sum);
            if (exposure > BalanceConfig.GEYSER_SAFE_COLUMN_TICKS.get()) {
                ExhaustionService.addExhaustion(data, BalanceConfig.GEYSER_COLUMN_EXHAUSTION.get().floatValue());
                player.hurt(player.damageSources().magic(), 1.5f);
                BreathDebuffs.apply(
                        player, new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false, true));
            }
            PsiHelper.set(player, data);
            player.syncData(com.effecoria.core.psi.ModAttachments.PSI.get());
        }
        // Drop exposure for players who left the column
        if (phaseTicks % 20 == 0) {
            Iterator<Map.Entry<UUID, Integer>> it = columnExposure.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> e = it.next();
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
                if (p == null || !column.contains(p.getBoundingBox().getCenter())) {
                    it.remove();
                }
            }
        }
    }

    private void settleAftermath(ServerLevel level, BlockPos pos) {
        RandomSource random = level.random;
        int dust = BalanceConfig.GEYSER_DUST_MIN.get()
                + random.nextInt(Math.max(1, BalanceConfig.GEYSER_DUST_MAX.get() - BalanceConfig.GEYSER_DUST_MIN.get() + 1));
        for (int i = 0; i < dust; i++) {
            double ox = (random.nextDouble() - 0.5) * 2.5;
            double oz = (random.nextDouble() - 0.5) * 2.5;
            ItemEntity item = new ItemEntity(
                    level,
                    pos.getX() + 0.5 + ox,
                    pos.getY() + 0.6,
                    pos.getZ() + 0.5 + oz,
                    new ItemStack(ModItems.ESSENITE_DUST.get()));
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        // Essonite crust puddles
        int puddles = 1 + random.nextInt(3);
        for (int i = 0; i < puddles; i++) {
            BlockPos puddle = pos.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (puddle.equals(pos)) {
                continue;
            }
            BlockPos place = puddle;
            if (!level.getBlockState(place).canBeReplaced()) {
                place = puddle.above();
            }
            if (level.getBlockState(place).canBeReplaced()
                    && level.getBlockState(place.below()).isCollisionShapeFullBlock(level, place.below())) {
                level.setBlockAndUpdate(place, ModBlocks.ESSONITE_CRUST.get().defaultBlockState());
            }
        }
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.6f);
    }

    private void ambientFx(ServerLevel level, BlockPos pos, float intensity) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.4,
                pos.getZ() + 0.5,
                Math.max(1, (int) (3 * intensity)),
                0.15,
                0.1,
                0.15,
                0.01);
        if (level.random.nextFloat() < 0.15f * intensity) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.25f, 0.35f);
        }
    }

    private void eruptFx(ServerLevel level, BlockPos pos) {
        double height = BalanceConfig.GEYSER_COLUMN_HEIGHT.get();
        level.sendParticles(
                ModParticleTypes.ELEMENTAL_PLASMA.get(),
                pos.getX() + 0.5,
                pos.getY() + height * 0.35,
                pos.getZ() + 0.5,
                28,
                0.25,
                height * 0.35,
                0.25,
                0.02);
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                pos.getX() + 0.5,
                pos.getY() + height * 0.5,
                pos.getZ() + 0.5,
                18,
                0.4,
                height * 0.3,
                0.4,
                0.03);
        if (phaseTicks % 15 == 0) {
            level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.0f, 0.45f);
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.7f, 0.5f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("DormantTarget", dormantTarget);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phaseTicks = tag.getInt("PhaseTicks");
        dormantTarget = Math.max(200, tag.getInt("DormantTarget"));
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && dormantTarget <= 0) {
            rollNextDormant((ServerLevel) level);
        }
    }
}
