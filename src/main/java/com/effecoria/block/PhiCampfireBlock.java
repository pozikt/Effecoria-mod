package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeatSource;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/** Primitive Φ heat source — LOW heat for crucibles before the essence burner. */
public final class PhiCampfireBlock extends BaseEntityBlock {
    public static final MapCodec<PhiCampfireBlock> CODEC = simpleCodec(PhiCampfireBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
    private static final DustParticleOptions GLOW =
            new DustParticleOptions(new Vector3f(0.35f, 0.85f, 1.0f), 1.0f);

    public PhiCampfireBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiCampfireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_CAMPFIRE.get(), PhiCampfireBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof PhiCampfireBlockEntity campfire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModItems.ESSENITE_DUST.get()) || stack.is(ModItems.ESSONITE_SHARD.get())) {
            if (!level.isClientSide() && campfire.addFuel(stack.is(ModItems.ESSENITE_DUST.get()) ? 200 : 80)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
                player.displayClientMessage(Component.translatable("message.effecoria.phi_campfire_lit"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.45;
        double z = pos.getZ() + 0.5;
        level.addParticle(
                GLOW,
                x + (random.nextDouble() - 0.5) * 0.4,
                y,
                z + (random.nextDouble() - 0.5) * 0.4,
                0,
                0.01,
                0);
    }

    public static final class PhiCampfireBlockEntity extends BlockEntity implements PhiHeatSource {
        public static final int FUEL_MAX = 2400;
        private int fuelTicks;

        public PhiCampfireBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PHI_CAMPFIRE.get(), pos, state);
        }

        public boolean addFuel(int ticks) {
            fuelTicks = Math.min(FUEL_MAX, fuelTicks + ticks);
            setChanged();
            syncLit();
            return true;
        }

        private void syncLit() {
            if (level != null) {
                BlockState state = getBlockState();
                boolean lit = fuelTicks > 0;
                if (state.getValue(LIT) != lit) {
                    level.setBlock(worldPosition, state.setValue(LIT, lit), Block.UPDATE_ALL);
                }
            }
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, PhiCampfireBlockEntity be) {
            if (be.fuelTicks <= 0) {
                return;
            }
            be.fuelTicks--;
            if (be.fuelTicks % 40 == 0) {
                be.setChanged();
            }
            if (be.fuelTicks <= 0) {
                be.syncLit();
            }
        }

        @Override
        public HeatLevel heatLevel() {
            return fuelTicks > 0 ? HeatLevel.LOW : HeatLevel.NONE;
        }

        @Override
        public boolean consumeHeatTick() {
            if (fuelTicks <= 0) {
                return false;
            }
            fuelTicks = Math.max(0, fuelTicks - 2);
            setChanged();
            if (fuelTicks <= 0) {
                syncLit();
            }
            return true;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            tag.putInt("Fuel", fuelTicks);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            fuelTicks = tag.getInt("Fuel");
        }
    }
}
