package com.effecoria;

import org.slf4j.Logger;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModCreativeTabs;
import com.effecoria.content.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(EffecoriaMod.MOD_ID)
public class EffecoriaMod {
    public static final String MOD_ID = "effecoria";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EffecoriaMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, BalanceConfig.SPEC);

        LOGGER.info("Effecoria loaded — Φ-field systems initializing (phase 0)");
    }
}
