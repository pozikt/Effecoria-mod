package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.world.DeadWastelandHydrology;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Dead Wasteland stays bone-dry: reject fluid spread and bucket placement.
 *
 * <p>Do <b>not</b> dry on {@code ChunkEvent.Load} — {@code /locate biome} loads hundreds of
 * chunks and cascading {@code setBlock} neighbor updates will OOM the game.
 */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class DeadWastelandWaterEvents {
    private DeadWastelandWaterEvents() {}

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        // Border rim is left alone so ocean/river edges do not collapse into trenches.
        if (!DeadWastelandHydrology.isInteriorDryCell(level, pos)) {
            return;
        }
        BlockState proposed = event.getNewState();
        boolean forbidden = DeadWastelandHydrology.isForbiddenWater(proposed)
                || DeadWastelandHydrology.isForbiddenWater(proposed.getFluidState());
        if (!forbidden) {
            return;
        }
        event.setNewState(DeadWastelandHydrology.rejectOrDry(level, pos, proposed));
    }

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!DeadWastelandHydrology.isInteriorDryCell(level, pos)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        if (!DeadWastelandHydrology.isForbiddenWater(placed)) {
            return;
        }
        event.setCanceled(true);
        // Quiet flags — no neighbor fluid schedules
        level.setBlock(pos, DeadWastelandHydrology.rejectOrDry(level, pos, placed), Block.UPDATE_CLIENTS);
    }
}
