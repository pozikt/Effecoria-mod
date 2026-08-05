package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.entity.CrystalCrabEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class PlateauMobEvents {
    private PlateauMobEvents() {}

    @SubscribeEvent
    public static void onCrystalBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!CrystalCrabEntity.isProtectedCrystal(event.getState())) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        var crabs = level.getEntitiesOfClass(
                CrystalCrabEntity.class,
                player.getBoundingBox().inflate(12.0),
                crab -> crab.isAlive());
        for (CrystalCrabEntity crab : crabs) {
            crab.onCrystalDisturbed(player, event.getPos());
        }
    }
}
