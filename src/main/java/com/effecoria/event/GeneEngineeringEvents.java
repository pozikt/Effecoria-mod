package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.organic.gene.GeneEngineeringService;
import com.effecoria.effect.organic.gene.GeneProfile;
import com.effecoria.core.psi.ModAttachments;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class GeneEngineeringEvents {
    private GeneEngineeringEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        GeneEngineeringService.tickLiving(living);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity host = event.getEntity();
        if (host.level().isClientSide()) {
            return;
        }
        var source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker) {
            GeneEngineeringService.onHostHurt(host, attacker);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            GeneProfile profile = player.getData(ModAttachments.GENE_PROFILE.get());
            if (!profile.isEmpty()) {
                GeneEngineeringService.reapplyOnLoad(player);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            GeneEngineeringService.reapplyOnLoad(player);
        }
    }
}
