package com.effecoria.core.technomagic;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Marks catalog nodes discovered when the player crafts their icon item. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class TechnomagicDiscoveryEvents {
    private TechnomagicDiscoveryEvents() {}

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crafted = event.getCrafting();
        discoverForItem(player, crafted.getItem());
    }

    public static void discoverForItem(ServerPlayer player, ItemLike itemLike) {
        Item item = itemLike.asItem();
        for (TechnomagicNode node : TechnomagicCatalog.all().values()) {
            if (node.status() != TechnomagicNode.TechnomagicStatus.AVAILABLE) {
                continue;
            }
            Item iconItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(node.icon());
            if (iconItem != null && iconItem == item) {
                if (TechnomagicProgress.tryDiscover(player, node.id())) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.effecoria.technomagic_discovered",
                                    net.minecraft.network.chat.Component.translatable(node.translationKey())),
                            true);
                }
            }
        }
        // Also map a few block items that share node icons
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (key != null && "effecoria".equals(key.getNamespace())) {
            TechnomagicCatalog.get(EffecoriaMod.id(key.getPath()))
                    .ifPresent(node -> TechnomagicProgress.tryDiscover(player, node.id()));
        }
    }

    /** Seed discovery for already-owned starter tech when opening the catalog. */
    public static void discoverOwned(ServerPlayer player) {
        for (TechnomagicNode node : TechnomagicCatalog.all().values()) {
            if (node.status() != TechnomagicNode.TechnomagicStatus.AVAILABLE) {
                continue;
            }
            Item icon = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(node.icon());
            if (icon == null || icon == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            if (player.getInventory().contains(new ItemStack(icon))) {
                TechnomagicProgress.tryDiscover(player, node.id());
            }
        }
        // Known core machines present as blocks in inventory as items
        discoverIfHas(player, ModItems.MORTAR_AND_PESTLE.get(), "mortar_and_pestle");
        discoverIfHas(player, ModItems.ESSENCE_BURNER.get(), "essence_burner");
        discoverIfHas(player, ModItems.SPARK_REACTOR.get(), "spark_reactor");
        discoverIfHas(player, ModItems.HEART_REACTOR_CORE.get(), "heart_reactor");
        discoverIfHas(player, ModItems.PHI_BUS.get(), "phi_bus");
        discoverIfHas(player, ModItems.ESSENCE_ALEMBIC.get(), "essence_alembic");
        discoverIfHas(player, ModItems.PHI_CELL.get(), "phi_cell");
        discoverIfHas(player, ModItems.RESONANCE_FOCUS.get(), "resonance_focus");
        discoverIfHas(player, ModItems.GOLD_FILTER.get(), "gold_filter");
        discoverIfHas(player, ModBlocks.PHI_GLASS.get().asItem(), "glass_workshop");
    }

    private static void discoverIfHas(ServerPlayer player, Item item, String path) {
        if (player.getInventory().contains(new ItemStack(item))) {
            TechnomagicProgress.tryDiscover(player, EffecoriaMod.id(path));
        }
    }
}
