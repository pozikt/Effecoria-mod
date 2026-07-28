package com.effecoria.event;

import java.util.Map;
import java.util.Optional;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.core.seal.SealInstance;
import com.effecoria.core.seal.SealService;
import com.effecoria.core.seal.SealTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        Optional<SealInstance> sealOpt = SealService.get(event.getEntity().level(), pos);
        if (sealOpt.isEmpty()) {
            return;
        }
        SealInstance seal = sealOpt.get();
        if (!seal.typeId().equals(SealTypes.FORTIFY) || seal.isExpired(event.getEntity().level().getGameTime())) {
            return;
        }
        event.setNewSpeed(event.getNewSpeed() * SealService.fortifyBreakFactor(seal));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
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
        if (player.tickCount % 10 != 0) {
            return;
        }
        tickSealsNearPlayer(player);
    }

    private static void tickSealsNearPlayer(ServerPlayer player) {
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
                for (Map.Entry<BlockPos, SealInstance> entry : Map.copyOf(data.seals()).entrySet()) {
                    BlockPos pos = entry.getKey();
                    SealInstance seal = entry.getValue();
                    if (seal.isExpired(gameTime)) {
                        continue;
                    }
                    if (seal.typeId().equals(SealTypes.DAMAGE_TRAP)) {
                        applyTrap(level, pos, seal);
                    } else if (seal.typeId().equals(SealTypes.GLOW)) {
                        SealService.ensureGlowLight(level, chunk, pos, seal);
                        spawnGlowParticles(level, pos);
                    }
                }
            }
        }
    }

    private static void applyTrap(ServerLevel level, BlockPos pos, SealInstance seal) {
        float damage = SealService.trapDamage(seal);
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.05, 0.1, 0.05);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    pos.getX() + 0.5,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.5,
                    4,
                    0.15,
                    0.05,
                    0.15,
                    0.01);
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
