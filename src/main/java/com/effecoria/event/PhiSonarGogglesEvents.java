package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModItems;
import com.effecoria.core.tower.PhiSonarGogglesService;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

/** Syncs tower Φ-sonar feed when Φ-sonar goggles are equipped. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class PhiSonarGogglesEvents {
    private PhiSonarGogglesEvents() {}

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getTo().is(ModItems.PHI_SONAR_GOGGLES.get())) {
            return;
        }
        PhiSonarGogglesService.trySyncOnEquip(player);
    }
}
