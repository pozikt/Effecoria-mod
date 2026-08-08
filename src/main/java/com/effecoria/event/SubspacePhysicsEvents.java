package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModBlocks;
import com.effecoria.effect.spatial.SubspaceEssentializationService;
import com.effecoria.effect.spatial.SubspacePhysicsService;
import com.effecoria.world.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Hyperspace fluid lock + zero-G entity hooks. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class SubspacePhysicsEvents {
    private SubspacePhysicsEvents() {}

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!ModDimensions.isSubspace(level)) {
            return;
        }
        BlockState constrained = SubspacePhysicsService.constrainFluidPlacement(event.getNewState());
        if (constrained != event.getNewState()) {
            event.setNewState(constrained);
        }
        if (constrained.is(ModBlocks.PHI_WATER.get()) && level instanceof ServerLevel server) {
            SubspaceEssentializationService.watch(server, event.getPos(), server.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!ModDimensions.isSubspace(level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState placed = event.getPlacedBlock();
        BlockState constrained = SubspacePhysicsService.constrainFluidPlacement(placed);
        if (SubspacePhysicsService.isVanillaWater(placed)) {
            event.setCanceled(true);
            level.setBlock(pos, ModBlocks.PHI_WATER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            if (level instanceof ServerLevel server) {
                SubspaceEssentializationService.watch(server, pos, server.getGameTime());
            }
            return;
        }
        if (!constrained.getFluidState().isEmpty() && !constrained.getFluidState().isSource()) {
            event.setCanceled(true);
            level.setBlock(pos, constrained, Block.UPDATE_CLIENTS);
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        SubspacePhysicsService.onJoinLevel(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        SubspacePhysicsService.tickEntity(entity);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (ModDimensions.isSubspace(event.getEntity().level())) {
            event.setCanceled(true);
            event.setDistance(0f);
        }
    }
}
