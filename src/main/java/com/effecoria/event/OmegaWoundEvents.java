package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModMobEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/** Ω-wound from Rotfang bites cancels natural / potion healing. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class OmegaWoundEvents {
    private OmegaWoundEvents() {}

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModMobEffects.OMEGA_WOUND)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && com.effecoria.core.disease.DiseaseEffects.blocksHealing(player)) {
            event.setCanceled(true);
        }
    }
}
