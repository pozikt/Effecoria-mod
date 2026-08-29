package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.artifact.JewelryAffixService;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.effect.mental.MentalityService;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Passive and reactive jewelry affix effects. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class JewelryAffixEvents {
    private JewelryAffixEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        long now = player.level().getGameTime();
        if (player.tickCount % 20 != 0) {
            return;
        }

        if (JewelryAffixService.hasEffect(player, "mental_ward")) {
            MentalityService.setShield(player, now + 60);
        }

        if (JewelryAffixService.hasEffect(player, "weight")) {
            int amp = Math.min(2, (int) JewelryAffixService.sumEffect(player, "weight", "per_tier", 1f));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, amp, false, false, true));
        }

        if (player.tickCount % 40 == 0) {
            tickRegenAndDrain(serverPlayer);
        }

        if (player.tickCount % 200 == 0 && JewelryAffixService.hasEffect(player, "psi_whisper")) {
            serverPlayer.displayClientMessage(Component.translatable("message.effecoria.affix_psi_whisper"), true);
        }
    }

    private static void tickRegenAndDrain(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        float psi = data.currentPsi();

        float hunger = JewelryAffixService.sumEffect(player, "phi_hunger", "per_tier", 0.35f);
        if (hunger > 0f && psi > 0f) {
            data.setCurrentPsi(psi - hunger);
            PsiHelper.set(player, data);
        }

        float regen = JewelryAffixService.sumEffect(player, "phi_regen", "per_tier", 0.25f);
        if (JewelryAffixService.hasEffect(player, "user_fed")) {
            regen += 0.35f;
        }
        if (JewelryAffixService.hasEffect(player, "autonomous")) {
            regen += 0.2f;
        }
        if (regen > 0f && psi >= 15f && player.getHealth() < player.getMaxHealth()) {
            player.heal(regen);
        }

        if (JewelryAffixService.hasEffect(player, "omega_pull")
                && (player.level().dimension().location().getPath().contains("nether")
                        || player.level().dimension().location().getPath().contains("end"))) {
            data.setEntropyB(data.entropyB() + 0.02f);
            PsiHelper.set(player, data);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        float reduction = JewelryAffixService.physicalDamageReduction(player);
        if (reduction <= 0f) {
            return;
        }
        event.setAmount(event.getAmount() * (1f - reduction));
    }
}
