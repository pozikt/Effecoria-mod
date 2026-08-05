package com.effecoria.block;

import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Glowing Φ turf — loot table drops Φ-earth. */
public class PhiGrassBlock extends PhiFieldBlock {
  public PhiGrassBlock(Properties properties) {
    super(properties);
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    super.animateTick(state, level, pos, random);
    if (random.nextInt(16) == 0) {
      double x = pos.getX() + random.nextDouble();
      double y = pos.getY() + 1.02;
      double z = pos.getZ() + random.nextDouble();
      level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0.0, 0.02, 0.0);
    }
  }
}
