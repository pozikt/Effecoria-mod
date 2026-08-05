package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Φ-saturated soil or stone — passively glows and slowly converts adjacent vanilla earth/stone.
 */
public class PhiFieldBlock extends Block {
  private static final int SPREAD_INTERVAL = 9;

  public PhiFieldBlock(Properties properties) {
    super(properties.randomTicks());
  }

  @Override
  public boolean isRandomlyTicking(BlockState state) {
    return true;
  }

  @Override
  public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (random.nextInt(SPREAD_INTERVAL) != 0) {
      return;
    }
    BlockPos target = pos.relative(
        switch (random.nextInt(6)) {
          case 0 -> net.minecraft.core.Direction.UP;
          case 1 -> net.minecraft.core.Direction.DOWN;
          case 2 -> net.minecraft.core.Direction.NORTH;
          case 3 -> net.minecraft.core.Direction.SOUTH;
          case 4 -> net.minecraft.core.Direction.EAST;
          default -> net.minecraft.core.Direction.WEST;
        });
    BlockState neighbor = level.getBlockState(target);
    BlockState converted = PhiSpreadLogic.convert(neighbor);
    if (converted != null) {
      level.setBlockAndUpdate(target, converted);
    }
    if (random.nextInt(3) == 0) {
      spawnGlow(level, pos, random);
    }
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    if (random.nextInt(24) == 0) {
      double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
      double y = pos.getY() + 0.55 + random.nextDouble() * 0.35;
      double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
      level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0.0, 0.012, 0.0);
    }
  }

  private static void spawnGlow(ServerLevel level, BlockPos pos, RandomSource random) {
    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
    double y = pos.getY() + 0.5 + random.nextDouble() * 0.4;
    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
    level.sendParticles(
        ModParticleTypes.PHI_SPARK.get(),
        x,
        y,
        z,
        2,
        0.08,
        0.06,
        0.08,
        0.01);
    if (random.nextInt(4) == 0) {
      level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.05, 0.04, 0.002);
    }
  }
}
