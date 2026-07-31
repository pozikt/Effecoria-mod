package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Keep necro thralls obedient: no friendly fire, no chasing through walls, no loot farm. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class NecroAllyEvents {
    private NecroAllyEvents() {}

    @SubscribeEvent
    public static void onThrallDrops(LivingDropsEvent event) {
        if (NecroSummonService.isNecroThrall(event.getEntity())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onThrallXp(LivingExperienceDropEvent event) {
        if (NecroSummonService.isNecroThrall(event.getEntity())) {
            event.setDroppedExperience(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!mob.getPersistentData().hasUUID(NecroSummonService.OWNER_TAG)) {
            return;
        }
        LivingEntity next = event.getNewAboutToBeSetTarget();
        if (next == null) {
            return;
        }
        var ownerId = mob.getPersistentData().getUUID(NecroSummonService.OWNER_TAG);
        if (next.getUUID().equals(ownerId) || NecroSummonService.sameNecromancer(mob, next)) {
            event.setCanceled(true);
            return;
        }
        // Block autonomous aggro on cave mobs the owner cannot see.
        if (!(mob.level().getPlayerByUUID(ownerId) instanceof ServerPlayer owner)
                || !NecroSummonService.canEngage(owner, next)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker instanceof Mob mob
                && mob.getPersistentData().hasUUID(NecroSummonService.OWNER_TAG)
                && victim.getUUID().equals(mob.getPersistentData().getUUID(NecroSummonService.OWNER_TAG))) {
            event.setCanceled(true);
            mob.setTarget(null);
            return;
        }
        if (victim instanceof Mob thrall
                && thrall.getPersistentData().hasUUID(NecroSummonService.OWNER_TAG)
                && attacker.getUUID().equals(thrall.getPersistentData().getUUID(NecroSummonService.OWNER_TAG))) {
            thrall.setLastHurtByMob(null);
            thrall.setTarget(null);
        }
        if (NecroSummonService.sameNecromancer(attacker, victim)) {
            event.setCanceled(true);
            if (attacker instanceof Mob mob) {
                mob.setTarget(null);
            }
        }

        if (victim instanceof ServerPlayer owner && NecroSummonService.countOwned(owner) > 0) {
            NecroSummonService.syncCombatFocus(owner, attacker);
        }
        if (attacker instanceof ServerPlayer owner && NecroSummonService.countOwned(owner) > 0) {
            NecroSummonService.syncCombatFocus(owner, victim);
        }
    }
}
