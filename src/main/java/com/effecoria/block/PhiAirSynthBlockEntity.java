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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public final class PhiAirSynthBlockEntity extends BlockEntity {
    public PhiAirSynthBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PHI_AIR_SYNTH.get(), pos, state); }
    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiAirSynthBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 40 != 0
                || TowerFacility.findComputer(server, pos).filter(a -> a.consecrated() && a.bound()).isEmpty()
                || !PhiPower.consumeTick(level, pos, 2)) return;
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(6))) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 100, 0, false, false));
            player.getFoodData().eat(0, 0.1f);
        }
    }
}
