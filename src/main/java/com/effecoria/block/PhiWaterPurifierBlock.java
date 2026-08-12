package com.effecoria.block;
import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos; import net.minecraft.world.InteractionHand; import net.minecraft.world.InteractionResult; import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.BaseEntityBlock; import net.minecraft.world.level.block.RenderShape; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.entity.BlockEntityTicker; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.phys.BlockHitResult; import javax.annotation.Nullable;
public final class PhiWaterPurifierBlock extends BaseEntityBlock {
 public static final MapCodec<PhiWaterPurifierBlock> CODEC=simpleCodec(PhiWaterPurifierBlock::new); public PhiWaterPurifierBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;} @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PhiWaterPurifierBlockEntity(p,s);}
 @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,ModBlockEntities.PHI_WATER_PURIFIER.get(),PhiWaterPurifierBlockEntity::serverTick);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player pl,BlockHitResult h){return InteractionResult.PASS;}
 @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack st,BlockState s,Level l,BlockPos p,Player pl,InteractionHand hand,BlockHitResult h){if(!l.isClientSide()&&l.getBlockEntity(p)instanceof PhiWaterPurifierBlockEntity be&&be.insert(st))return net.minecraft.world.ItemInteractionResult.CONSUME;return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
}
