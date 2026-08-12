package com.effecoria.block;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.tower.TowerFacility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class RegenChamberBlockEntity extends BlockEntity {
 public RegenChamberBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.REGEN_CHAMBER.get(),p,s);}
 public static void serverTick(Level l,BlockPos p,BlockState s,RegenChamberBlockEntity be){
  if(!(l instanceof ServerLevel server)||l.getGameTime()%20!=0||TowerFacility.findComputer(server,p).filter(a->a.consecrated()&&a.bound()).isEmpty()||!PhiPower.consumeTick(l,p,2))return;
  for(Player player:l.getEntitiesOfClass(Player.class,new AABB(p).inflate(2))) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,50,0,false,false));
 }
}
