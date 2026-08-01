package com.effecoria.event;

import java.util.List;
import java.util.Map;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.core.seal.SealInstance;
import com.effecoria.core.seal.SealProgramRuntime;
import com.effecoria.core.seal.SealService;
import com.effecoria.core.seal.SealTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
                            spawnGlowParticles(level, pos);
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
        BlockPos standingOn = BlockPos.containing(living.getX(), living.getY() - 0.05, living.getZ());
        LevelChunk chunk = level.getChunkAt(standingOn);
        SealService.purgeExpired(level, chunk, gameTime);

        for (SealInstance seal : SealService.getAll(level, standingOn)) {
            if (seal.isExpired(gameTime)) {
                continue;
            }
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
            } else if (seal.typeId().equals(SealTypes.DAMAGE_TRAP)) {
                living.hurt(level.damageSources().magic(), SealService.trapDamage(seal));
            } else if (seal.typeId().equals(SealTypes.SNARE)) {
                int amp = seal.params() != null && seal.params().contains("slow_amplifier")
                        ? seal.params().getInt("slow_amplifier")
                        : 3;
                applySlowTo(living, seal, amp);
            } else if (seal.typeId().equals(SealTypes.REPULSE)) {
                float force = seal.params() != null && seal.params().contains("repulse_force")
                        ? seal.params().getFloat("repulse_force")
                        : 0.85f;
                force *= Math.max(0.5f, seal.strength() / 40f);
                living.setDeltaMovement(living.getDeltaMovement().add(0, force, 0));
                living.hurtMarked = true;
            }
        }
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
