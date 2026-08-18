package com.effecoria.event;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.core.seal.SealInstance;
import com.effecoria.core.seal.SealProgramEffects;
import com.effecoria.core.seal.SealProgramRuntime;
import com.effecoria.core.seal.SealService;
import com.effecoria.core.seal.SealTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class SealEvents {
    private SealEvents() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getPosition().isEmpty()) {
            return;
        }
        BlockPos pos = event.getPosition().get();
        long gameTime = event.getEntity().level().getGameTime();
        for (SealInstance seal : SealService.getAll(event.getEntity().level(), pos)) {
            if (seal.isExpired(gameTime)) {
                continue;
            }
            boolean fortify = seal.typeId().equals(SealTypes.FORTIFY)
                    || (SealProgramRuntime.isProgram(seal)
                            && SealProgramRuntime.effectiveHardness(seal, gameTime) > 0f);
            if (!fortify) {
                continue;
            }
            event.setNewSpeed(event.getNewSpeed() * SealService.fortifyBreakFactor(seal, gameTime));
            return;
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        pulseAt(level, event.getPos(), SealProgramRuntime.SenseEvent.HIT, event.getEntity());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player
                && player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty()
                && com.effecoria.core.seal.SealProgramService.clearComponent(player, event.getPos())) {
            event.setCanceled(true);
            return;
        }
        long gameTime = level.getGameTime();
        for (SealInstance seal : SealService.getAll(level, event.getPos())) {
            if (seal.isExpired(gameTime) || !SealProgramRuntime.isProgram(seal)) {
                continue;
            }
            if (SealProgramRuntime.effectiveClausura(seal, gameTime)
                    || SealProgramRuntime.effectiveServare(seal, gameTime)) {
                event.setCanceled(true);
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.effecoria.seal.locked"),
                            true);
                }
                break;
            }
        }
        pulseAt(level, event.getPos(), SealProgramRuntime.SenseEvent.USE, event.getEntity());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        pulseAt(serverLevel, event.getPos(), SealProgramRuntime.SenseEvent.BREAK, event.getPlayer());
        SealService.remove(serverLevel, event.getPos());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        // Glow lights + expiry purge still keyed to nearby players (cheap chunk scan).
        purgeAndMaintainGlowNear(player);
    }

    private static final String PD_SEAL_X = "effecoria_seal_sx";
    private static final String PD_SEAL_Y = "effecoria_seal_sy";
    private static final String PD_SEAL_Z = "effecoria_seal_sz";
    private static final String PD_SEAL_SET = "effecoria_seal_set";

    /** Seals keep working under any living entity — even if the caster died / left. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if (!(living.level() instanceof ServerLevel level)) {
            return;
        }
        if (living.tickCount % 5 != 0) {
            return;
        }
        SealProgramEffects.tickAbnegatio(living, level.getGameTime());
        tickSealsUnder(level, living);
    }

    private static void pulseAt(
            ServerLevel level, BlockPos pos, SealProgramRuntime.SenseEvent senseEvent, LivingEntity subject) {
        long gameTime = level.getGameTime();
        for (SealInstance seal : SealService.getAll(level, pos)) {
            if (seal.isExpired(gameTime) || !SealProgramRuntime.isProgram(seal)) {
                continue;
            }
            SealProgramRuntime.pulse(level, pos, seal, gameTime, senseEvent, subject);
            SealService.markDirty(level, pos);
        }
    }

    private static void purgeAndMaintainGlowNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        BlockPos origin = player.blockPosition();

        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                int chunkX = (origin.getX() >> 4) + cx;
                int chunkZ = (origin.getZ() >> 4) + cz;
                if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                SealService.purgeExpired(level, chunk, gameTime);
                ChunkSealData data = SealService.getChunkData(chunk);
                if (data.isEmpty()) {
                    continue;
                }
                for (Map.Entry<BlockPos, List<SealInstance>> entry : Map.copyOf(data.seals()).entrySet()) {
                    BlockPos pos = entry.getKey();
                    for (SealInstance seal : entry.getValue()) {
                        if (seal.isExpired(gameTime)) {
                            continue;
                        }
                        boolean glow = seal.typeId().equals(SealTypes.GLOW)
                                || (SealProgramRuntime.isProgram(seal)
                                        && SealProgramRuntime.effectiveGlow(seal, gameTime) > 0);
                        if (glow) {
                            SealService.ensureGlowLight(level, chunk, pos, seal);
                            if (!SealProgramRuntime.effectiveUmbra(seal, gameTime)) {
                                spawnGlowParticles(level, pos);
                            }
                        }
                        if (SealProgramRuntime.isProgram(seal)) {
                            SealProgramRuntime.tickApproach(level, pos, seal, gameTime);
                        }
                    }
                }
            }
        }
    }

    private static void tickSealsUnder(ServerLevel level, LivingEntity living) {
        long gameTime = level.getGameTime();
        BlockPos standingOn = resolveStandingSealPos(level, living);
        releaseLeftSupport(level, living, standingOn);

        LevelChunk chunk = level.getChunkAt(standingOn);
        SealService.purgeExpired(level, chunk, gameTime);

        List<SealInstance> layers = SealService.getAll(level, standingOn);
        if (layers.isEmpty()) {
            return;
        }

        boolean dirty = false;
        // Exactly one offensive combat layer (repulse > snare > trap > program).
        Optional<SealInstance> offensive = SealService.getChunkData(chunk).findOffensive(standingOn);
        if (offensive.isPresent()) {
            SealInstance seal = offensive.get();
            if (!seal.isExpired(gameTime)) {
                dirty |= applyOffensiveStanding(level, standingOn, living, seal, gameTime);
            }
        }

        // Fortify/glow are passive; programs may still need STEP even if somehow not selected —
        // findOffensive already prefers program only when no trap/snare/repulse.
        if (dirty) {
            SealService.markDirty(level, standingOn);
        }
    }

    private static boolean applyOffensiveStanding(
            ServerLevel level, BlockPos standingOn, LivingEntity living, SealInstance seal, long gameTime) {
        if (seal.typeId().equals(SealTypes.PROGRAM)) {
            SealProgramRuntime.pulse(
                    level, standingOn, seal, gameTime, SealProgramRuntime.SenseEvent.STEP, living);
            float hurt = SealProgramRuntime.effectiveHurt(seal, gameTime);
            if (hurt > 0f) {
                living.hurt(level.damageSources().magic(), hurt);
            }
            int slow = SealProgramRuntime.effectiveSlow(seal, gameTime);
            if (slow > 0) {
                applySlowTo(living, seal, slow);
            }
            float push = SealProgramRuntime.effectivePush(seal, gameTime);
            if (push > 0f) {
                living.setDeltaMovement(living.getDeltaMovement().add(0, push, 0));
                living.hurtMarked = true;
            }
            float calor = SealProgramRuntime.effectiveCalor(seal, gameTime);
            if (calor > 0f) {
                living.igniteForSeconds(Math.max(1, Math.round(calor / 2f)));
            }
            if (SealProgramRuntime.effectiveServare(seal, gameTime)) {
                applySlowTo(living, seal, 4);
            }
            if (SealProgramRuntime.effectiveAbnegatio(seal, gameTime)) {
                living.getPersistentData().putLong("effecoria_abnegatio_until", gameTime + 30);
                living.noPhysics = true;
            }
            return true;
        }
        if (seal.typeId().equals(SealTypes.DAMAGE_TRAP)) {
            living.hurt(level.damageSources().magic(), SealService.trapDamage(seal));
            return false;
        }
        if (seal.typeId().equals(SealTypes.SNARE)) {
            int amp = seal.params() != null && seal.params().contains("slow_amplifier")
                    ? seal.params().getInt("slow_amplifier")
                    : 3;
            applySlowTo(living, seal, amp);
            return false;
        }
        if (seal.typeId().equals(SealTypes.REPULSE)) {
            float force = seal.params() != null && seal.params().contains("repulse_force")
                    ? seal.params().getFloat("repulse_force")
                    : 0.85f;
            force *= Math.max(0.5f, seal.strength() / 40f);
            living.setDeltaMovement(living.getDeltaMovement().add(0, force, 0));
            living.hurtMarked = true;
            return false;
        }
        return false;
    }

    /**
     * Block that should host standing seals: prefer {@link LivingEntity#getOnPos()}, then
     * feet-1 fallbacks, picking the candidate that actually has a seal when ambiguous.
     */
    private static BlockPos resolveStandingSealPos(ServerLevel level, LivingEntity living) {
        BlockPos onPos = living.getOnPos();
        BlockPos legacy = BlockPos.containing(living.getX(), living.getY() - 0.05, living.getZ());
        BlockPos belowFeet = living.blockPosition().below();
        if (!SealService.getAll(level, onPos).isEmpty()) {
            return onPos;
        }
        if (!legacy.equals(onPos) && !SealService.getAll(level, legacy).isEmpty()) {
            return legacy;
        }
        if (!belowFeet.equals(onPos)
                && !belowFeet.equals(legacy)
                && !SealService.getAll(level, belowFeet).isEmpty()) {
            return belowFeet;
        }
        return onPos;
    }

    /** When an entity leaves a sealed block, reset program sense latches so re-entry fires again. */
    private static void releaseLeftSupport(ServerLevel level, LivingEntity living, BlockPos current) {
        var pd = living.getPersistentData();
        if (!pd.getBoolean(PD_SEAL_SET)) {
            writeSupport(pd, current);
            return;
        }
        BlockPos prev = new BlockPos(pd.getInt(PD_SEAL_X), pd.getInt(PD_SEAL_Y), pd.getInt(PD_SEAL_Z));
        if (prev.equals(current)) {
            return;
        }
        for (SealInstance seal : SealService.getAll(level, prev)) {
            if (SealProgramRuntime.isProgram(seal)) {
                SealProgramRuntime.clearSenseFlags(seal);
                SealService.markDirty(level, prev);
            }
        }
        writeSupport(pd, current);
    }

    private static void writeSupport(CompoundTag pd, BlockPos pos) {
        pd.putBoolean(PD_SEAL_SET, true);
        pd.putInt(PD_SEAL_X, pos.getX());
        pd.putInt(PD_SEAL_Y, pos.getY());
        pd.putInt(PD_SEAL_Z, pos.getZ());
    }

    private static void applySlowTo(LivingEntity living, SealInstance seal, int amp) {
        if (!(living.level() instanceof ServerLevel level)) {
            return;
        }
        BreathDebuffs.apply(
                level,
                seal.casterId(),
                living,
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, Math.max(0, amp - 1)));
    }

    private static void spawnGlowParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 0.6,
                pos.getZ() + 0.5,
                2,
                0.25,
                0.25,
                0.25,
                0.0);
    }
}
