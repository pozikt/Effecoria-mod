package com.effecoria.block;

import com.effecoria.content.ModItems;
import com.effecoria.core.tower.TowerFacility;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

public final class OmegaDamperBlock extends BaseEntityBlock {
    public static final MapCodec<OmegaDamperBlock> CODEC = simpleCodec(OmegaDamperBlock::new);
    public OmegaDamperBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new OmegaDamperBlockEntity(pos, state); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !stack.is(ModItems.PURIFIED_OBSIDIAN.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean cleared = TowerFacility.findComputer(server, pos).map(a -> a.clearOmega()).orElse(false);
        if (cleared && !player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable(cleared ? "message.effecoria.tower.omega_cleared" : "message.effecoria.tower.omega_empty"), true);
        return ItemInteractionResult.CONSUME;
    }
}
