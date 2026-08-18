package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.effect.organic.gene.GeneEngineeringService;
import com.effecoria.effect.organic.gene.GeneProfile;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
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
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        var child = event.getChild();
        if (child == null) {
            return;
        }
        GeneEngineeringService.inheritLockedDna(event.getParentA(), event.getParentB(), child);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        GeneEngineeringService.onHostHurt(victim, attacker);
        GeneEngineeringService.onHostAttack(attacker, victim, bonus -> event.setAmount(event.getAmount() + bonus));
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
