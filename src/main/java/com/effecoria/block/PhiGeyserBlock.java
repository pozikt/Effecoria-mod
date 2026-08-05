package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Planetary Φ-crack — dormant glow, cyclic plasma eruptions, dust harvest.
 */
public final class PhiGeyserBlock extends BaseEntityBlock {
    public static final MapCodec<PhiGeyserBlock> CODEC = simpleCodec(PhiGeyserBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PhiGeyserPhase> PHASE =
            EnumProperty.create("phase", PhiGeyserPhase.class);

    private static final VoxelShape SHAPE_NS = Block.box(5.0, 0.0, 1.0, 11.0, 4.0, 15.0);
    private static final VoxelShape SHAPE_EW = Block.box(1.0, 0.0, 5.0, 15.0, 4.0, 11.0);

    public PhiGeyserBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PHASE, PhiGeyserPhase.DORMANT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PHASE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiGeyserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PHI_GEYSER.get(), PhiGeyserBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(ModItems.RESONANCE_FOCUS.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PhiGeyserBlockEntity geyser)) {
            return ItemInteractionResult.FAIL;
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            return ItemInteractionResult.FAIL;
        }
        if (geyser.tryForceErupt((ServerLevel) level, state)) {
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.1f, 0.55f);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.FAIL;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PhiGeyserBlockEntity geyser)) {
            return;
        }
        geyser.onTouchCrack(player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()
                && entity instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof PhiGeyserBlockEntity geyser) {
            if (player.tickCount % 20 == 0) {
                geyser.onTouchCrack(player);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        PhiGeyserPhase phase = state.getValue(PHASE);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.35;
        double z = pos.getZ() + 0.5;

        if (phase == PhiGeyserPhase.DORMANT || phase == PhiGeyserPhase.COOLDOWN) {
            if (random.nextFloat() < 0.35f) {
                level.addParticle(ModParticleTypes.PHI_SPARK.get(), x, y, z, 0, 0.02, 0);
            }
            return;
        }
        if (phase == PhiGeyserPhase.PRECURSOR) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(
                        ModParticleTypes.ELEMENTAL_PLASMA.get(),
                        x + (random.nextDouble() - 0.5) * 0.4,
                        y,
                        z + (random.nextDouble() - 0.5) * 0.4,
                        0,
                        0.08,
                        0);
            }
            return;
        }
        // ERUPTING — tall Cherenkov column + gold veins + crown sparks
        int height = 18 + random.nextInt(40);
        for (int i = 0; i < 14; i++) {
            double h = random.nextDouble() * height;
            double spin = h * 0.35;
            double ox = Math.cos(spin) * (0.15 + h * 0.01);
            double oz = Math.sin(spin) * (0.15 + h * 0.01);
            level.addParticle(
                    ModParticleTypes.ELEMENTAL_PLASMA.get(), x + ox, y + h * 0.55, z + oz, 0, 0.35, 0);
            if (random.nextBoolean()) {
                level.addParticle(
                        ModParticleTypes.PHI_SPARK.get(),
                        x - ox * 0.6,
                        y + h * 0.55,
                        z - oz * 0.6,
                        (random.nextDouble() - 0.5) * 0.05,
                        0.1,
                        (random.nextDouble() - 0.5) * 0.05);
            }
        }
        for (int i = 0; i < 6; i++) {
            double crownY = y + height * 0.55;
            level.addParticle(
                    ParticleTypes.END_ROD,
                    x + (random.nextDouble() - 0.5) * 2.2,
                    crownY,
                    z + (random.nextDouble() - 0.5) * 2.2,
                    (random.nextDouble() - 0.5) * 0.08,
                    0.02,
                    (random.nextDouble() - 0.5) * 0.08);
        }
        if (random.nextFloat() < 0.4f) {
            level.addParticle(
                    ModParticleTypes.PHI_FLAME.get(),
                    x + (random.nextDouble() - 0.5),
                    y + 1.0 + random.nextDouble() * 4,
                    z + (random.nextDouble() - 0.5),
                    (random.nextDouble() - 0.5) * 0.05,
                    -0.02,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }
}
