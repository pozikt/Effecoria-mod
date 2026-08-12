package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.tower.TowerSoulbindService;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class FoundationAmuletBlock extends BaseEntityBlock {
    public static final MapCodec<FoundationAmuletBlock> CODEC = simpleCodec(FoundationAmuletBlock::new);
    public FoundationAmuletBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundationAmuletBlockEntity(pos, state);
    }
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (stack.is(ModItems.SOUL_SHARD.get()) && level instanceof ServerLevel server
                && player instanceof ServerPlayer sp
                && level.getBlockEntity(pos) instanceof FoundationAmuletBlockEntity amulet) {
            return TowerSoulbindService.bindAtAmulet(sp, server, amulet, stack)
                    ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof FoundationAmuletBlockEntity amulet
                && amulet.charged()) {
            TowerSoulbindService.onAmuletBroken(server, amulet);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
