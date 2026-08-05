package com.effecoria.block;

import com.effecoria.content.ModBiomeTags;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Φ-saturated soil or stone — passively glows and slowly converts adjacent vanilla earth/stone.
 */
public class PhiFieldBlock extends Block {
    /** Outside plateau: slower infection. Inside biome: aggressive reclaim of vanilla leftovers. */
    private static final int SPREAD_INTERVAL = 9;
    private static final int SPREAD_INTERVAL_PLATEAU = 3;

    public PhiFieldBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean plateau = level.getBiome(pos).is(ModBiomeTags.ESSENCE_PLATEAU);
        int interval = plateau ? SPREAD_INTERVAL_PLATEAU : SPREAD_INTERVAL;
        if (random.nextInt(interval) != 0) {
            return;
        }
        int attempts = plateau ? 3 : 1;
        for (int i = 0; i < attempts; i++) {
            BlockPos target = pos.relative(net.minecraft.core.Direction.getRandom(random));
            BlockState neighbor = level.getBlockState(target);
            BlockState converted = PhiSpreadLogic.convert(neighbor);
            if (converted != null) {
                level.setBlockAndUpdate(target, converted);
            }
        }
        if (random.nextInt(3) == 0) {
            spawnGlow(level, pos, random);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(18) == 0) {
            double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
            double y = pos.getY() + 1.02;
            double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
            boolean gold = random.nextBoolean();
            if (gold) {
                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
            } else {
                level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0.0, 0.015, 0.0);
            }
        }
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level.isClientSide()) {
            return;
        }
        if (state.is(ModBlocks.PHI_STONE.get())) {
            // Glass shatter already from SoundType; add low Φ-hum + gold flash.
            level.playSound(
                    null,
                    pos,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    0.55f,
                    0.55f);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS,
                    0.25f,
                    0.45f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(
                        ParticleTypes.END_ROD,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        8,
                        0.25,
                        0.25,
                        0.25,
                        0.02);
                server.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        pos.getX() + 0.5,
                        pos.getY() + 0.55,
                        pos.getZ() + 0.5,
                        6,
                        0.2,
                        0.2,
                        0.2,
                        0.01);
            }
        }
    }

    private static void spawnGlow(ServerLevel level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 0.5 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), x, y, z, 2, 0.08, 0.06, 0.08, 0.01);
        if (random.nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.05, 0.04, 0.002);
        }
    }
}
