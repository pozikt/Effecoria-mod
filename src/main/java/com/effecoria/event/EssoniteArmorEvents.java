package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.armor.EssoniteArmorData;
import com.effecoria.armor.EssoniteArmorService;
import com.effecoria.config.BalanceConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class EssoniteArmorEvents {
    private EssoniteArmorEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(victim instanceof ServerPlayer player)) {
            return;
        }
        if (EssoniteArmorService.tryAbnegatioAbsorb(player)) {
            event.setCanceled(true);
            return;
        }
        if (EssoniteArmorService.isCrystalSkinActive(player)) {
            event.setCanceled(true);
            return;
        }
        if (EssoniteArmorService.isOmegaActive(player)
                && (event.getSource().is(DamageTypes.MAGIC)
                        || event.getSource().is(DamageTypes.WITHER)
                        || event.getSource().is(DamageTypes.INDIRECT_MAGIC))) {
            event.setCanceled(true);
            return;
        }
        if (EssoniteArmorService.hasAny(player)
                && EssoniteArmorData.poolCharge(player) > 0.05f
                && (event.getSource().is(DamageTypes.ON_FIRE)
                        || event.getSource().is(DamageTypes.IN_FIRE)
                        || event.getSource().is(DamageTypes.LAVA)
                        || event.getSource().is(DamageTypes.HOT_FLOOR))) {
            float red = BalanceConfig.ESSONITE_ARMOR_FIRE_REDUCTION.get().floatValue();
            event.setAmount(event.getAmount() * (1f - red));
        }
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        EssoniteArmorService.onPiezoHit(player, event.getNewDamage());
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        EquipmentSlot slot = event.getSlot();
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return;
        }
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();
        if (!EssoniteArmorData.isEssonite(from) || !to.isEmpty()) {
            return;
        }
        if (EssoniteArmorService.canUnequip(player, from)) {
            return;
        }
        // Clausura: put the piece back.
        player.setItemSlot(slot, from.copy());
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.armor_clausura_locked"), true);
    }
}
