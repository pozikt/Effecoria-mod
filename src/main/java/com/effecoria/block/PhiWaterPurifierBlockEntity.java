package com.effecoria.block;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.tower.TowerFacility;
import net.minecraft.core.BlockPos; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.entity.BlockEntity; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.item.ItemStack;
public final class PhiWaterPurifierBlockEntity extends BlockEntity {
 private ItemStack input=ItemStack.EMPTY,filter=ItemStack.EMPTY,output=ItemStack.EMPTY;
 public PhiWaterPurifierBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.PHI_WATER_PURIFIER.get(),p,s);}
 public boolean insert(ItemStack stack){if(stack.is(ModItems.PHI_WATER_BUCKET.get())&&input.isEmpty()){input=stack.split(1);setChanged();return true;}if((stack.is(ModItems.GOLD_FILTER.get())||stack.is(ModItems.LEAD_FILTER.get()))&&filter.isEmpty()){filter=stack.split(1);setChanged();return true;}return false;}
 public boolean extractTo(ItemStack held){if(output.isEmpty())return false; if(held.isEmpty()){held.setCount(0);return false;} return false;}
 public static void serverTick(Level l,BlockPos p,BlockState s,PhiWaterPurifierBlockEntity be){if(l.getGameTime()%40!=0||be.input.isEmpty()||be.filter.isEmpty()||!be.output.isEmpty()||!(l instanceof net.minecraft.server.level.ServerLevel server)||TowerFacility.findComputer(server,p).filter(a->a.consecrated()&&a.bound()).isEmpty()||!PhiPower.consumeTick(l,p,2))return;be.input.shrink(1);be.output=new ItemStack(ModItems.PURIFIED_PHI_WATER_BUCKET.get());be.filter.hurtAndBreak(1,server,(net.minecraft.world.entity.LivingEntity)null,item->{});be.setChanged();}
}
