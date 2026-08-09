package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PsiHelper;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rotten Moss — dull purple glow on Scar crust / bones. Standing on it seeps Ω (b) into initiated mages.
 */
public final class RottenMossBlock extends CarpetBlock {
    public static final MapCodec<RottenMossBlock> CODEC = simpleCodec(RottenMossBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public RottenMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CarpetBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), Direction.UP)
                || below.is(ModBlocks.ASH_SOIL.get())
                || below.is(ModBlocks.VOID_OBSIDIAN.get());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof Player player && player.tickCount % 40 == 0) {
            var data = PsiHelper.get(player);
            if (data.initiated()) {
                data.setEntropyB(data.entropyB() + 0.02f);
                PsiHelper.set(player, data);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(
                    ModParticleTypes.CORRUPTION_ENTROPY.get(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 0.15,
                    pos.getZ() + random.nextDouble(),
                    0,
                    0.02,
                    0);
        }
    }
}
