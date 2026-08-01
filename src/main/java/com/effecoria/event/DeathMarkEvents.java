package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.necromancy.DeathMarkService;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class DeathMarkEvents {
    private DeathMarkEvents() {}

    /** High priority so gear is snapshotted before loot drops strip the corpse. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(com.effecoria.core.psi.ModAttachments.LAST_DEATH.get()).record(player);
        }
        if (event.getEntity() instanceof Mob mob && NecroSummonService.isNecroThrall(mob)) {
            var ownerId = mob.getPersistentData().getUUID(NecroSummonService.OWNER_TAG);
            if (mob.level().getServer() != null) {
                ServerPlayer owner = mob.level().getServer().getPlayerList().getPlayer(ownerId);
                if (owner != null) {
                    PlayerPsiData data = PsiHelper.get(owner);
                    data.untrackThrall(mob.getUUID());
                    PsiHelper.set(owner, data);
                    DeathMarkService.syncReservedPsi(owner);
                }
            }
        }
        DeathMarkService.onMarkedDeath(event.getEntity());
    }

    @SubscribeEvent
    public static void onWorldMarkTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ArmorStand stand && DeathMarkService.isWorldMark(stand)) {
            DeathMarkService.tickWorldMarkEntity(stand);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (DeathMarkService.tryRaise(player, event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (DeathMarkService.tryRaise(player, event.getTarget())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
