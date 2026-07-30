package com.effecoria.client;

import java.util.Map;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.core.seal.SealInstance;
import com.effecoria.core.seal.SealTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-side seal visuals from synced chunk attachments. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSealEffects {
    private ClientSealEffects() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (minecraft.player.tickCount % 8 != 0) {
            return;
        }
        Level level = minecraft.level;
        BlockPos origin = minecraft.player.blockPosition();
        long gameTime = level.getGameTime();

        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                int chunkX = (origin.getX() >> 4) + cx;
                int chunkZ = (origin.getZ() >> 4) + cz;
                if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                ChunkSealData data = chunk.getData(ModAttachments.CHUNK_SEALS.get());
                if (data.isEmpty()) {
                    continue;
                }
                for (Map.Entry<BlockPos, SealInstance> entry : data.seals().entrySet()) {
                    SealInstance seal = entry.getValue();
                    if (seal.isExpired(gameTime)) {
                        continue;
                    }
                    BlockPos pos = entry.getKey();
                    if (pos.distSqr(origin) > 48 * 48) {
                        continue;
                    }
                    spawnClientParticles(level, pos, seal);
                }
            }
        }
    }

    private static void spawnClientParticles(Level level, BlockPos pos, SealInstance seal) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.5;
        if (seal.typeId().equals(SealTypes.GLOW)) {
            level.addParticle(ModParticleTypes.SEAL_GLYPH.get(), x, y, z, 0, 0.008, 0);
            level.addParticle(ModParticleTypes.SEAL_SPARK.get(), x + 0.2, y + 0.2, z, 0, 0.012, 0);
        } else if (seal.typeId().equals(SealTypes.DAMAGE_TRAP)
                || seal.typeId().equals(SealTypes.SNARE)
                || seal.typeId().equals(SealTypes.REPULSE)) {
            level.addParticle(ModParticleTypes.CORRUPTION_RUNE.get(), x, y + 0.5, z, 0, 0.015, 0);
            level.addParticle(ModParticleTypes.CORRUPTION_POISON.get(), x, y + 0.3, z, 0, -0.01, 0);
        } else if (seal.typeId().equals(SealTypes.FORTIFY)) {
            level.addParticle(ModParticleTypes.SEAL_GLYPH.get(), x, y, z, 0, 0.01, 0);
        }
    }
}
