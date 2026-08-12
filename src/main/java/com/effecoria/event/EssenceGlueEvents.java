package com.effecoria.event;

import com.effecoria.core.glue.EssenceGlueService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = com.effecoria.EffecoriaMod.MOD_ID)
public final class EssenceGlueEvents {
    private EssenceGlueEvents() {}

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        EssenceGlueService.onBlockRemoved(server, event.getPos());
    }
}
