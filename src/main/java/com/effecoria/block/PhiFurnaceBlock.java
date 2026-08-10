package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.HeatLevel;
import com.effecoria.core.alchemy.PhiHeat;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Era II Φ-furnace — consumes neighbor Φ-heat to refine shards and bake Φ-glass.
 */
public final class PhiFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<PhiFurnaceBlock> CODEC = simpleCodec(PhiFurnaceBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public PhiFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new PhiFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_FURNACE.get(), PhiFurnaceBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof PhiFurnaceBlockEntity furnace)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.isEmpty()) {
            ItemStack taken = furnace.takeOutput();
            if (taken.isEmpty()) {
                taken = furnace.takeInput();
            }
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (PhiFurnaceBlockEntity.recipeFor(stack) != null && furnace.insertInput(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.5f, 1.2f);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PhiFurnaceBlockEntity furnace) {
            net.minecraft.world.Containers.dropContents(level, pos, furnace);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static final class PhiFurnaceBlockEntity extends BlockEntity implements WorldlyContainer {
        public static final int SLOT_IN = 0;
        public static final int SLOT_OUT = 1;
        private static final int[] IN_SLOTS = {SLOT_IN};
        private static final int[] OUT_SLOTS = {SLOT_OUT};

        private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
        private int progress;
        private int cookGoal = 160;

        public PhiFurnaceBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PHI_FURNACE.get(), pos, state);
        }

        @Nullable
        public static ItemStack recipeFor(ItemStack input) {
            if (input.is(ModItems.ESSONITE_SHARD.get())) {
                return new ItemStack(ModItems.PURE_ESSONITE.get());
            }
            if (input.is(ModItems.ESSENITE_DUST.get())) {
                // Bake dust+ implicit sand — dust alone → 1 phi glass shard equivalent as flask precursor: glass
                return new ItemStack(ModItems.PHI_GLASS.get());
            }
            if (input.is(Items.SAND) || input.is(Items.RED_SAND)) {
                return new ItemStack(ModItems.PHI_GLASS.get());
            }
            return null;
        }

        public boolean insertInput(ItemStack stack) {
            if (recipeFor(stack) == null || !items.get(SLOT_IN).isEmpty()) {
                return false;
            }
            items.set(SLOT_IN, stack.copyWithCount(1));
            progress = 0;
            setChanged();
            return true;
        }

        public ItemStack takeInput() {
            ItemStack stack = items.get(SLOT_IN);
            items.set(SLOT_IN, ItemStack.EMPTY);
            progress = 0;
            setChanged();
            return stack;
        }

        public ItemStack takeOutput() {
            ItemStack stack = items.get(SLOT_OUT);
            items.set(SLOT_OUT, ItemStack.EMPTY);
            setChanged();
            return stack;
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, PhiFurnaceBlockEntity be) {
            HeatLevel heat = PhiHeat.getNeighborHeat(level, pos);
            ItemStack in = be.items.get(SLOT_IN);
            ItemStack result = recipeFor(in);
            if (!heat.isPresent() || in.isEmpty() || result == null || !be.items.get(SLOT_OUT).isEmpty()) {
                if (be.progress > 0) {
                    be.progress = Math.max(0, be.progress - 2);
                    be.setChanged();
                }
                return;
            }
            if (!(level instanceof ServerLevel server) || !PhiHeat.consumeNeighborHeat(server, pos)) {
                return;
            }
            be.cookGoal = switch (heat) {
                case HIGH -> 80;
                case MEDIUM -> 120;
                default -> 200;
            };
            be.progress++;
            if (be.progress >= be.cookGoal) {
                be.progress = 0;
                be.items.set(SLOT_IN, ItemStack.EMPTY);
                be.items.set(SLOT_OUT, result);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 1.6f);
                be.setChanged();
            } else if (be.progress % 20 == 0) {
                be.setChanged();
            }
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return items.get(0).isEmpty() && items.get(1).isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack result = ContainerHelper.removeItem(items, slot, amount);
            if (!result.isEmpty()) {
                setChanged();
            }
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            items.clear();
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.DOWN ? OUT_SLOTS : IN_SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
            return index == SLOT_IN && recipeFor(stack) != null && items.get(SLOT_IN).isEmpty();
        }

        @Override
        public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
            return index == SLOT_OUT;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            tag.putInt("Progress", progress);
            tag.putInt("CookGoal", cookGoal);
            ContainerHelper.saveAllItems(tag, items, provider);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            progress = tag.getInt("Progress");
            cookGoal = tag.contains("CookGoal") ? tag.getInt("CookGoal") : 160;
            ContainerHelper.loadAllItems(tag, items, provider);
        }
    }
}
