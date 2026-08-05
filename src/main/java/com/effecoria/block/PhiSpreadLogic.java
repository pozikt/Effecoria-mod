package com.effecoria.block;

import com.effecoria.content.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

/** Maps vanilla blocks to Φ-field replacements when adjacent to glowing Φ mass. */
public final class PhiSpreadLogic {
  private PhiSpreadLogic() {}

  public static BlockState convert(BlockState state) {
    if (state.is(ModBlocks.PHI_STONE.get())
            || state.is(ModBlocks.PHI_DIRT.get())
            || state.is(ModBlocks.PHI_GRASS.get())) {
      return null;
    }
    if (state.is(Blocks.GRASS_BLOCK)) {
      return ModBlocks.PHI_GRASS.get().defaultBlockState();
    }
    if (state.is(BlockTags.DIRT) || state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
      return ModBlocks.PHI_DIRT.get().defaultBlockState();
    }
    if (state.is(BlockTags.BASE_STONE_OVERWORLD)
            || state.is(Blocks.COBBLESTONE)
            || state.is(Blocks.MOSSY_COBBLESTONE)
            || state.is(Blocks.STONE)
            || state.is(Blocks.DEEPSLATE)
            || state.is(Blocks.TUFF)
            || state.is(Blocks.GRANITE)
            || state.is(Blocks.DIORITE)
            || state.is(Blocks.ANDESITE)
            || state.is(Blocks.BASALT)
            || state.is(Blocks.CALCITE)) {
      return ModBlocks.PHI_STONE.get().defaultBlockState();
    }
  return null;
  }
}
