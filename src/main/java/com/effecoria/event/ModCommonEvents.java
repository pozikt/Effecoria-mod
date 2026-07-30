package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.command.EffecoriaCommands;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.magic.ShadeService;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.progression.ProgressionService;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.effect.elemental.ElementalBlockService;
import com.effecoria.effect.elemental.SteamCloudService;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ElementalBlockService.tick(serverLevel);
            SteamCloudService.tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SteamCloudService.syncToPlayer(player, serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SteamCloudService.syncToPlayer(player, serverLevel);
        }
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
        ShadeService.tick(player);
        data.mergeMissingSpells(SpellProgression.spellsForSchool(data.school()));

        if (CreativeGodMode.isActive(player)) {
            data.setCurrentPsi(data.maxPsi());
            data.setEntropyB(0f);
            data.setExhaustion(0f);
            PsiHelper.set(player, data);
            player.syncData(ModAttachments.PSI.get());
            return;
        }

        ExhaustionService.tick(player, data);

        float regen = FormulaEngine.regenPsi(
                PsiHelper.toContext(player, data),
                PhiFieldService.sample(player.level(), player.position(), player),
                10f);
        if (regen > 0f) {
            data.setCurrentPsi(data.currentPsi() + regen);
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
    }
}
