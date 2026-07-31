package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.SpellCombat;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Ensures mobs treat caster-attributed (or mid-cast anonymous) spell damage as an attack.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class SpellAggroEvents {
    private SpellAggroEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            SpellCombat.alert(victim, attacker);
            return;
        }
        // Mid-resolve cast used anonymous magic/wither — still provoke from active caster.
        if (BreathDebuffs.currentCaster() != null) {
            SpellCombat.alert(victim, BreathDebuffs.currentCaster());
        }
    }
}
