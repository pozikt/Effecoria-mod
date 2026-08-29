package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModItems;
import com.effecoria.core.artifact.ModularPartData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Stamps modular part NBT when jewelry blanks are crafted from tagged materials. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class ArtifactCraftEvents {
    private ArtifactCraftEvents() {}

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) {
            return;
        }
        Container matrix = event.getInventory();
        if (result.is(ModItems.JEWELRY_BAND.get())) {
            stampJewelry(result, matrix, ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "jewelry_band_materials"), true);
        } else if (result.is(ModItems.JEWELRY_GEM.get())) {
            stampJewelry(result, matrix, ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "jewelry_gem_materials"), false);
        }
    }

    private static void stampJewelry(
            ItemStack result, Container matrix, ResourceLocation materialTagId, boolean band) {
        Item material = findTaggedMaterial(matrix, materialTagId);
        if (material == null) {
            return;
        }
        ItemStack stamped = band ? ModularPartData.createBand(material) : ModularPartData.createGem(material);
        result.applyComponents(stamped.getComponents());
    }

    private static Item findTaggedMaterial(Container matrix, ResourceLocation tagId) {
        TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack stack = matrix.getItem(i);
            if (!stack.isEmpty() && stack.is(tag)) {
                return stack.getItem();
            }
        }
        return null;
    }
}
