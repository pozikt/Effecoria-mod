package com.effecoria.event;

import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.progression.ProgressionService;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.effecoria.EffecoriaMod;
import com.effecoria.command.EffecoriaCommands;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class ModCommonEvents {
    private ModCommonEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(SpellRegistry.RELOAD_LISTENER);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EffecoriaCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }

        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            return;
        }

        ProgressionService.tick(player, data);

        if (CreativeGodMode.isActive(player)) {
            data.setCurrentPsi(data.maxPsi());
            data.setEntropyB(0f);
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
            return;
        }

        PsiHelper.set(player, data);

        float regen = FormulaEngine.regenPsi(
                PsiHelper.toContext(data),
                PhiFieldService.sample(player.level(), player.position(), player),
                10f);
        if (regen <= 0f) {
            return;
        }

        data.setCurrentPsi(data.currentPsi() + regen);
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
    }
}
