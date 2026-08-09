package com.effecoria.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.effecoria.EffecoriaMod;
import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.world.CrystalForestService;
import com.effecoria.world.EssencePlateauService;
import com.effecoria.world.weather.PhiWeatherKind;
import com.effecoria.world.weather.PhiWeatherService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Morning harvest of Essence Dew from Φ foliage on plateau / crystal forest. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class EssenceDewEvents {
    private static final Map<Long, Long> BLOCK_COOLDOWN = new ConcurrentHashMap<>();

    private EssenceDewEvents() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getItemStack().isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!EssencePlateauService.isBiome(level, pos) && !CrystalForestService.isBiome(level, pos)) {
            return;
        }
        PhiWeatherKind kind = PhiWeatherService.dominantKind(level, pos);
        if (kind != PhiWeatherKind.ESSENCE_DEW && kind != PhiWeatherKind.ESSENCE_MIST && kind != PhiWeatherKind.ESSENCE_RAIN) {
            long dayTime = level.getDayTime() % 24000L;
            if (dayTime >= BalanceConfig.PHI_WEATHER_DEW_WINDOW_TICKS.get()) {
                return;
            }
        }
        if (!isDewSource(level.getBlockState(pos))) {
            return;
        }
        long key = pos.asLong();
        long now = level.getGameTime();
        Long until = BLOCK_COOLDOWN.get(key);
        if (until != null && until > now) {
            return;
        }
        if (player.getRandom().nextFloat() >= BalanceConfig.PHI_WEATHER_DEW_HARVEST_CHANCE.get().floatValue()) {
            BLOCK_COOLDOWN.put(key, now + 40);
            return;
        }
        BLOCK_COOLDOWN.put(key, now + 200);
        ItemStack dew = new ItemStack(ModItems.ESSENCE_DEW.get());
        if (!player.addItem(dew)) {
            player.drop(dew, false);
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5f, 1.3f);
        player.displayClientMessage(Component.translatable("message.effecoria.weather.dew_harvest"), true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean isDewSource(BlockState state) {
        return state.is(ModBlocks.PHI_LEAVES.get())
                || state.is(ModBlocks.PHI_BLADES.get())
                || state.is(ModBlocks.PHI_GRASS.get())
                || state.is(ModBlocks.PHI_SAPLING.get())
                || state.is(ModBlocks.PHI_GEYSER.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE.get());
    }
}
