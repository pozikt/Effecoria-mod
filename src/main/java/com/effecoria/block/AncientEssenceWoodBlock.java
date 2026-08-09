package com.effecoria.block;

import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Ancient canopy heartwood — ultra-dense Φ-conductive timber. */
public final class AncientEssenceWoodBlock extends RotatedPillarBlock {
    public AncientEssenceWoodBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0,
                    0.03,
                    0);
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
        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return;
        }
        if (player.getRandom().nextFloat() < 0.18f) {
            popResource(level, pos, new ItemStack(ModItems.ANCIENT_HEARTWOOD.get()));
        }
    }
}
