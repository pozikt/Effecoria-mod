package com.effecoria.event;

import com.effecoria.EffecoriaMod;
import com.effecoria.content.ModItems;
import com.effecoria.world.OmegaScarService;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Scar fauna leave Ω-stained bones. */
@EventBusSubscriber(modid = EffecoriaMod.MOD_ID)
public final class OmegaScarLootEvents {
    private OmegaScarLootEvents() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton skeleton)) {
            return;
        }
        if (!OmegaScarService.isBiome(skeleton.level(), skeleton.blockPosition())) {
            return;
        }
        if (skeleton.getRandom().nextFloat() > 0.55f) {
            return;
        }
        int count = 1 + skeleton.getRandom().nextInt(2);
        ItemStack stack = new ItemStack(ModItems.DISTORTED_BONE.get(), count);
        ItemEntity drop = new ItemEntity(
                skeleton.level(), skeleton.getX(), skeleton.getY(), skeleton.getZ(), stack);
        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }
}
