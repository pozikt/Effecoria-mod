package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/** Vanilla crops fail on Φ-soil — only Φ-flora thrives there. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class PhiSoilCropEvents {
    private PhiSoilCropEvents() {}

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        BlockPos below = event.getPos().below();
        BlockState soil = level.getBlockState(below);
        if (soil.is(ModBlocks.PHI_DIRT.get()) || soil.is(ModBlocks.PHI_GRASS.get())) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    @SubscribeEvent
    public static void onCropGrowPost(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (!(event.getState().getBlock() instanceof CropBlock)) {
            return;
        }
        BlockState soil = level.getBlockState(event.getPos().below());
        if (soil.is(ModBlocks.PHI_DIRT.get()) || soil.is(ModBlocks.PHI_GRASS.get())) {
            level.destroyBlock(event.getPos(), true);
        }
    }
}
