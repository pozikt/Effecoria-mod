package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
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

/** Era I clay crucible — slow impure ore→shard conversion when heated. */
public final class ClayCrucibleBlock extends BaseEntityBlock {
    public static final MapCodec<ClayCrucibleBlock> CODEC = simpleCodec(ClayCrucibleBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);

    public ClayCrucibleBlock(Properties properties) {
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
        return new ClayCrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.CLAY_CRUCIBLE.get(), ClayCrucibleBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof ClayCrucibleBlockEntity crucible)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.isEmpty()) {
            ItemStack taken = crucible.takeOutput();
            if (taken.isEmpty()) {
                taken = crucible.takeInput();
            }
            if (!taken.isEmpty()) {
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (ClayCrucibleBlockEntity.canAccept(stack) && crucible.insertInput(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.5f, 1.1f);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ClayCrucibleBlockEntity crucible) {
            net.minecraft.world.Containers.dropContents(level, pos, crucible);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static final class ClayCrucibleBlockEntity extends BlockEntity implements WorldlyContainer {
        public static final int SLOT_IN = 0;
        public static final int SLOT_OUT = 1;
        public static final int COOK_TIME = 200;
        private static final int[] IN_SLOTS = {SLOT_IN};
        private static final int[] OUT_SLOTS = {SLOT_OUT};

        private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
        private int progress;

        public ClayCrucibleBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CLAY_CRUCIBLE.get(), pos, state);
        }

        public static boolean canAccept(ItemStack stack) {
            return stack.is(ModBlocks.ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.GRANITE_ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.ANDESITE_ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.DIORITE_ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.TUFF_ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.BASALT_ESSENITE_ORE.get().asItem())
                    || stack.is(ModBlocks.ESSONITE_CRYSTAL.get().asItem());
        }

        public boolean insertInput(ItemStack stack) {
            if (!canAccept(stack) || !items.get(SLOT_IN).isEmpty()) {
                return false;
            }
            items.set(SLOT_IN, stack.copyWithCount(1));
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

        public static void serverTick(Level level, BlockPos pos, BlockState state, ClayCrucibleBlockEntity be) {
            HeatLevel heat = PhiHeat.getNeighborHeat(level, pos);
            if (!heat.isPresent() || be.items.get(SLOT_IN).isEmpty() || !be.items.get(SLOT_OUT).isEmpty()) {
                if (be.progress > 0) {
                    be.progress = Math.max(0, be.progress - 2);
                    be.setChanged();
                }
                return;
            }
            if (!(level instanceof ServerLevel server) || !PhiHeat.consumeNeighborHeat(server, pos)) {
                return;
            }
            int need = heat.ordinal() <= HeatLevel.LOW.ordinal() ? COOK_TIME : COOK_TIME / 2;
            be.progress++;
            if (be.progress >= need) {
                be.progress = 0;
                be.items.set(SLOT_IN, ItemStack.EMPTY);
                if (level.random.nextFloat() < 0.55f) {
                    be.items.set(SLOT_OUT, new ItemStack(ModItems.ESSONITE_SHARD.get()));
                } else {
                    be.items.set(SLOT_OUT, new ItemStack(Items.COBBLESTONE));
                }
                level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.35f, 1.3f);
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
            return index == SLOT_IN && canAccept(stack) && items.get(SLOT_IN).isEmpty();
        }

        @Override
        public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
            return index == SLOT_OUT;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            tag.putInt("Progress", progress);
            ContainerHelper.saveAllItems(tag, items, provider);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            progress = tag.getInt("Progress");
            ContainerHelper.loadAllItems(tag, items, provider);
        }
    }
}
