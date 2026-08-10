package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.artifact.AssembledGearData;
import com.effecoria.core.artifact.ItemSealCatalog;
import com.effecoria.core.artifact.ItemSealDefinition;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;

/** Applies Effecoria item-seal combat / armor effects. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class ItemSealEvents {
    private ItemSealEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Map<ResourceLocation, Integer> seals = AssembledGearData.seals(stack);
        if (seals.isEmpty()) {
            return;
        }
        List<Component> tip = event.getToolTip();
        tip.add(Component.empty());
        tip.add(Component.translatable(
                "tooltip.effecoria.item_seals_header",
                seals.size(),
                AssembledGearData.sealCapacity(stack)));
        seals.forEach((id, lvl) -> tip.add(Component.translatable(
                "tooltip.effecoria.item_seal_line",
                Component.translatable("item_seal.effecoria." + id.getPath()),
                lvl)));
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            applyWeaponSeals(event, attacker);
        }
        if (event.getEntity() instanceof Player player) {
            applyArmorSeals(event, player);
        }
    }

    private static void applyWeaponSeals(LivingIncomingDamageEvent event, LivingEntity attacker) {
        ItemStack weapon = attacker.getMainHandItem();
        Map<ResourceLocation, Integer> seals = AssembledGearData.seals(weapon);
        if (seals.isEmpty()) {
            return;
        }
        float bonus = 0f;
        for (Map.Entry<ResourceLocation, Integer> e : seals.entrySet()) {
            ItemSealDefinition def = ItemSealCatalog.get(e.getKey()).orElse(null);
            if (def == null) {
                continue;
            }
            int lvl = e.getValue();
            float per = def.params().getOrDefault("per_level", 0f);
            switch (def.effect()) {
                case "damage_bonus" -> bonus += per * lvl;
                case "smite" -> {
                    if (event.getEntity().getType().is(EntityTypeTags.UNDEAD)) {
                        bonus += per * lvl;
                    }
                }
                case "bane" -> {
                    if (event.getEntity().getType().is(EntityTypeTags.ARTHROPOD)) {
                        bonus += per * lvl;
                    }
                }
                case "void_edge" -> bonus += per * lvl;
                case "fire_aspect" -> event.getEntity().igniteForSeconds(per * lvl);
                case "knockback" -> event.getEntity()
                        .knockback(
                                per * lvl,
                                attacker.getX() - event.getEntity().getX(),
                                attacker.getZ() - event.getEntity().getZ());
                default -> {}
            }
        }
        if (bonus > 0f) {
            event.setAmount(event.getAmount() + bonus);
        }
    }

    private static void applyArmorSeals(LivingIncomingDamageEvent event, Player player) {
        float reduce = 0f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack armor = player.getItemBySlot(slot);
            for (Map.Entry<ResourceLocation, Integer> e : AssembledGearData.seals(armor).entrySet()) {
                ItemSealDefinition def = ItemSealCatalog.get(e.getKey()).orElse(null);
                if (def == null) {
                    continue;
                }
                float per = def.params().getOrDefault("per_level", 0f);
                int lvl = e.getValue();
                switch (def.effect()) {
                    case "protection" -> reduce += per * lvl;
                    case "fire_protection" -> {
                        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                            reduce += per * lvl;
                        }
                    }
                    case "blast_protection" -> {
                        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                            reduce += per * lvl;
                        }
                    }
                    case "projectile_protection" -> {
                        if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                            reduce += per * lvl;
                        }
                    }
                    case "thorns" -> {
                        if (event.getSource().getEntity() instanceof LivingEntity attacker
                                && player.getRandom().nextFloat() < per * lvl) {
                            attacker.hurt(player.damageSources().thorns(player), 1f + lvl);
                        }
                    }
                    case "essence_ward" -> reduce += per * lvl * 0.5f;
                    default -> {}
                }
            }
        }
        if (reduce > 0f) {
            event.setAmount(event.getAmount() * Math.max(0.2f, 1f - Math.min(0.8f, reduce)));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int lvl = AssembledGearData.sealLevel(
                boots, ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "feather_falling"));
        if (lvl > 0) {
            event.setDamageMultiplier(Math.max(0f, 1f - 0.12f * lvl));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 40 != 0) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            mend(player, stack);
        }
        for (ItemStack stack : player.getArmorSlots()) {
            mend(player, stack);
        }
        mend(player, player.getMainHandItem());
        mend(player, player.getOffhandItem());

        // Servare on held staff: slow Ψ regen
        ItemStack held = player.getMainHandItem();
        if (AssembledGearData.isStaff(held)
                && AssembledGearData.hasPhoneme(held, com.effecoria.armor.EssonitePhoneme.SERVARE)) {
            var data = PsiHelper.get(player);
            data.setCurrentPsi(data.currentPsi() + 0.4f);
            PsiHelper.set(player, data);
        }
    }

    private static void mend(Player player, ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamaged()) {
            return;
        }
        int lvl = AssembledGearData.sealLevel(
                stack, ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "psi_mend"));
        if (lvl <= 0) {
            return;
        }
        ItemSealDefinition def = ItemSealCatalog.get(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "psi_mend"))
                .orElse(null);
        float cost = def == null ? 0.5f : def.params().getOrDefault("psi_per_point", 0.5f);
        var data = PsiHelper.get(player);
        if (data.currentPsi() < cost) {
            return;
        }
        data.setCurrentPsi(data.currentPsi() - cost);
        PsiHelper.set(player, data);
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
    }
}
