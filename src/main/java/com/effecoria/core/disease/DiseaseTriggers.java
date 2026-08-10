package com.effecoria.core.disease;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModItems;
import com.effecoria.effect.corruption.CorruptionCurseService;
import com.effecoria.entity.CrystalCrabEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Acquisition hooks for Φ-diseases. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class DiseaseTriggers {
    private DiseaseTriggers() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        BlockState state = event.getState();
        if (isDustyEssonite(state) && !DiseaseService.hasRespirator(player)) {
            DiseaseService.onDustInhale(player, 3);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (event.getSource().getEntity() instanceof CrystalCrabEntity) {
            DiseaseService.onCrystalCrabHit(player);
        }
        // Ω-blood / Ω-matter contact via damage from omega entities
        if (event.getSource().getEntity() != null) {
            var attacker = event.getSource().getEntity();
            String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(attacker.getType())
                    .getPath();
            if (path.contains("omega") && player.getRandom().nextFloat() < 0.08f) {
                DiseaseService.infect(player, PhiDisease.OMEGA_ROT, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 100 != 0) {
            return;
        }
        if (CorruptionCurseService.hasCurse(player)) {
            DiseaseService.onCorruptionPresent(player);
        }
        // Holding omega blood vial risks omega rot / sickness
        if (player.getMainHandItem().is(ModItems.OMEGA_BLOOD_VIAL.get())
                || player.getOffhandItem().is(ModItems.OMEGA_BLOOD_VIAL.get())) {
            if (player.getRandom().nextFloat() < 0.05f) {
                DiseaseService.infect(player, PhiDisease.OMEGA_SICKNESS, 1);
            }
        }
    }

    private static boolean isDustyEssonite(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null || !"effecoria".equals(key.getNamespace())) {
            return false;
        }
        String path = key.getPath();
        return path.contains("essonite")
                && (path.contains("ore")
                        || path.contains("crystal")
                        || path.contains("crust")
                        || path.contains("pointed")
                        || path.equals("essonite_block")
                        || path.equals("star_essonite_block")
                        || path.contains("dripstone"));
    }

    public static void onNecroRitual(ServerPlayer player) {
        DiseaseService.onGhostEchoRisk(player);
    }

    public static void onSubspaceTransit(ServerPlayer player) {
        DiseaseService.onGhostEchoRisk(player);
    }

    public static void onForeignFocus(ServerPlayer player) {
        DiseaseService.onSoulDissonanceRisk(player);
    }

    public static void onMentalConflict(ServerPlayer player) {
        DiseaseService.onSoulDissonanceRisk(player);
    }
}
