package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.BloodVialEmptyItem;
import com.effecoria.content.ModItems;
import com.effecoria.entity.EssenceWyvernEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class BloodEvents {
    private BloodEvents() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        BloodVialEmptyItem.onEntityInteract(event);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof EssenceWyvernEntity) {
            if (entity.getRandom().nextFloat() < 0.65f) {
                drop(event, new ItemStack(ModItems.WYVERN_BLOOD_VIAL.get(), 1 + entity.getRandom().nextInt(2)));
            }
            return;
        }
        if (entity instanceof Animal && entity.getRandom().nextFloat() < 0.08f) {
            drop(event, new ItemStack(ModItems.BLOOD_VIAL.get()));
        }
    }

    private static void drop(LivingDropsEvent event, ItemStack stack) {
        LivingEntity entity = event.getEntity();
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
    }
}
