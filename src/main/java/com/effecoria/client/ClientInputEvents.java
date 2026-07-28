package com.effecoria.client;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.network.ModNetworking;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private ClientInputEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (KeyBindings.CAST_SPELL.consumeClick()) {
            PlayerPsiData data = minecraft.player.getData(ModAttachments.PSI.get());
            int index = data.initiated() ? data.selectedSpellIndex() : -1;
            PacketDistributor.sendToServer(new ModNetworking.CastSpellPayload(index));
        }

        while (KeyBindings.CYCLE_SPELL.consumeClick()) {
            PacketDistributor.sendToServer(new ModNetworking.CycleSpellPayload(1));
        }
    }
}
