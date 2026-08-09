package com.effecoria.block;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PsiHelper;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Carnivorous Φ-vine — snags living things with a Ψ aura. */
public final class PhiSnareVineBlock extends BushBlock {
    public static final MapCodec<PhiSnareVineBlock> CODEC = simpleCodec(PhiSnareVineBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public PhiSnareVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlocks.PHI_DIRT.get())
                || state.is(ModBlocks.PHI_GRASS.get())
                || state.is(ModBlocks.PHI_LOG.get())
                || state.is(ModBlocks.ANCIENT_ESSENCE_WOOD.get())
                || state.is(ModBlocks.GOLDEN_BARK.get())
                || state.is(ModBlocks.PHI_STONE.get());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (mayPlaceOn(belowState, level, below)) {
            return true;
        }
        // Hang from logs/leaves sideways.
        for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(dir));
            if (side.is(ModBlocks.PHI_LOG.get())
                    || side.is(ModBlocks.ANCIENT_ESSENCE_WOOD.get())
                    || side.is(ModBlocks.PHI_LEAVES.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living) || entity.tickCount % 8 != 0) {
            return;
        }
        boolean mage = entity instanceof Player player && PsiHelper.get(player).initiated();
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, mage ? 2 : 1, true, false));
        living.hurt(level.damageSources().magic(), mage ? 1.5f : 0.5f);
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 pull = center.subtract(living.position()).normalize().scale(0.08);
        living.setDeltaMovement(living.getDeltaMovement().add(pull.x, 0.02, pull.z));
        living.hasImpulse = true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0,
                    0.02,
                    0);
        }
    }
}
