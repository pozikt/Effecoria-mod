package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Keep necro thralls from attacking their necromancer or sibling thralls. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class NecroAllyEvents {
    private NecroAllyEvents() {}

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
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        // Thrall must not hurt owner.
        if (attacker instanceof Mob mob
                && mob.getPersistentData().hasUUID(NecroSummonService.OWNER_TAG)
                && victim.getUUID().equals(mob.getPersistentData().getUUID(NecroSummonService.OWNER_TAG))) {
            event.setCanceled(true);
            mob.setTarget(null);
            return;
        }
        // Owner accidental swings shouldn't enrage thralls into revenge on owner (handled by target cancel).
        if (victim instanceof Mob thrall
                && thrall.getPersistentData().hasUUID(NecroSummonService.OWNER_TAG)
                && attacker.getUUID().equals(thrall.getPersistentData().getUUID(NecroSummonService.OWNER_TAG))) {
            thrall.setLastHurtByMob(null);
            thrall.setTarget(null);
        }
        // Sibling thralls.
        if (NecroSummonService.sameNecromancer(attacker, victim)) {
            event.setCanceled(true);
            if (attacker instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }
}
