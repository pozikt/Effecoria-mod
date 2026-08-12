package com.effecoria.block;
import com.effecoria.content.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.BaseEntityBlock; import net.minecraft.world.level.block.RenderShape; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.entity.BlockEntityTicker; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState; import javax.annotation.Nullable;
public final class RegenChamberBlock extends BaseEntityBlock {
 public static final MapCodec<RegenChamberBlock> CODEC=simpleCodec(RegenChamberBlock::new); public RegenChamberBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;} @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RegenChamberBlockEntity(p,s);}
 @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,ModBlockEntities.REGEN_CHAMBER.get(),RegenChamberBlockEntity::serverTick);}
}
