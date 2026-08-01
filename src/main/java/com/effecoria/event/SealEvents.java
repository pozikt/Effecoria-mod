package com.effecoria.event;

import java.util.List;
import java.util.Map;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.core.seal.SealInstance;
import com.effecoria.core.seal.SealProgramEffects;
import com.effecoria.core.seal.SealProgramRuntime;
import com.effecoria.core.seal.SealService;
import com.effecoria.core.seal.SealTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
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
        if (player.tickCount % 5 != 0) {
            return;
        }
        tickSealsNearPlayer(player);
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

    private static void tickSealsNearPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        BlockPos origin = player.blockPosition();
        BlockPos standingOn = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());

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
                        if (seal.typeId().equals(SealTypes.PROGRAM)) {
                            tickProgram(level, chunk, pos, seal, gameTime, player, standingOn);
                        } else if (seal.typeId().equals(SealTypes.DAMAGE_TRAP)) {
                            SealProgramEffects.applyStandingHurt(level, pos, seal, SealService.trapDamage(seal));
                        } else if (seal.typeId().equals(SealTypes.SNARE)) {
                            int amp = seal.params() != null && seal.params().contains("slow_amplifier")
                                    ? seal.params().getInt("slow_amplifier")
                                    : 3;
                            SealProgramEffects.applyStandingSlow(level, pos, seal, amp);
                        } else if (seal.typeId().equals(SealTypes.REPULSE)) {
                            float force = seal.params() != null && seal.params().contains("repulse_force")
                                    ? seal.params().getFloat("repulse_force")
                                    : 0.85f;
                            force *= Math.max(0.5f, seal.strength() / 40f);
                            SealProgramEffects.applyStandingPush(level, pos, seal, force);
                        } else if (seal.typeId().equals(SealTypes.GLOW)) {
                            SealService.ensureGlowLight(level, chunk, pos, seal);
                            spawnGlowParticles(level, pos);
                        }
                    }
                }
            }
        }
    }

    private static void tickProgram(
            ServerLevel level,
            LevelChunk chunk,
            BlockPos pos,
            SealInstance seal,
            long gameTime,
            ServerPlayer player,
            BlockPos standingOn) {
        SealProgramRuntime.tickApproach(level, pos, seal, gameTime);

        if (standingOn.equals(pos)) {
            SealProgramRuntime.pulse(level, pos, seal, gameTime, SealProgramRuntime.SenseEvent.STEP, player);
        }

        int glow = SealProgramRuntime.effectiveGlow(seal, gameTime);
        if (glow > 0) {
            SealService.ensureGlowLight(level, chunk, pos, seal);
            spawnGlowParticles(level, pos);
        }

        float hurt = SealProgramRuntime.effectiveHurt(seal, gameTime);
        if (hurt > 0f) {
            SealProgramEffects.applyStandingHurt(level, pos, seal, hurt);
        }
        int slow = SealProgramRuntime.effectiveSlow(seal, gameTime);
        if (slow > 0) {
            SealProgramEffects.applyStandingSlow(level, pos, seal, slow);
        }
        float push = SealProgramRuntime.effectivePush(seal, gameTime);
        if (push > 0f) {
            SealProgramEffects.applyStandingPush(level, pos, seal, push);
        }
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
