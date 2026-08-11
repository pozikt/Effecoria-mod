package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

/** Anvil repair + brewing sinks for crusher byproducts. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class ByproductSinkEvents {
    private ByproductSinkEvents() {}

    @SubscribeEvent
    public static void onBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        builder.addMix(Potions.AWKWARD, ModItems.PHI_BONE_PASTE.get(), Potions.REGENERATION);
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || !right.is(ModItems.PHI_BONE_PASTE.get()) || !left.isDamaged()) {
            return;
        }
        boolean chitinArmor = left.is(ModItems.PHI_CHITIN_HELMET.get())
                || left.is(ModItems.PHI_CHITIN_CHESTPLATE.get())
                || left.is(ModItems.PHI_CHITIN_LEGGINGS.get())
                || left.is(ModItems.PHI_CHITIN_BOOTS.get());
        if (!chitinArmor) {
            return;
        }
        ItemStack out = left.copy();
        int repair = Math.max(1, out.getMaxDamage() / 4);
        out.setDamageValue(Math.max(0, out.getDamageValue() - repair));
        event.setOutput(out);
        event.setCost(2);
        event.setMaterialCost(1);
    }
}
