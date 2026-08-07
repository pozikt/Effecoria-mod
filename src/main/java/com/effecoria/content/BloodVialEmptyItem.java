package com.effecoria.content;

import java.util.UUID;

import com.effecoria.core.psi.PsiHelper;
import com.effecoria.entity.EssenceWyvernEntity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Empty Φ-blood vial — fill from self or a living target. */
public final class BloodVialEmptyItem extends Item {
    public static final String NBT_DONOR = "BloodDonor";
    public static final float SELF_DRAIN_DAMAGE = 2f;

    public BloodVialEmptyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        ItemStack filled = fillFromLiving(player, player);
        if (filled.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        player.hurt(player.damageSources().generic(), SELF_DRAIN_DAMAGE);
        player.getCooldowns().addCooldown(this, 40);
        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6f, 1.1f);
        return InteractionResultHolder.consume(exchange(player, hand, stack, filled));
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity living) || living instanceof Player) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BloodVialEmptyItem) || player.level().isClientSide()) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        ItemStack filled = fillFromLiving(player, living);
        if (filled.isEmpty()) {
            return;
        }
        living.hurt(living.damageSources().playerAttack(player), 1.5f);
        player.getCooldowns().addCooldown(stack.getItem(), 30);
        player.level().playSound(
                null, living.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.55f, 1.05f);
        InteractionHand hand = event.getHand();
        player.setItemInHand(hand, exchange(player, hand, stack, filled));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static ItemStack fillFromLiving(Player collector, LivingEntity donor) {
        Item filledItem;
        if (donor instanceof EssenceWyvernEntity) {
            filledItem = ModItems.WYVERN_BLOOD_VIAL.get();
        } else if (donor instanceof Player donorPlayer && PsiHelper.get(donorPlayer).initiated()) {
            filledItem = ModItems.MAGE_BLOOD_VIAL.get();
        } else {
            filledItem = ModItems.BLOOD_VIAL.get();
        }
        ItemStack out = new ItemStack(filledItem);
        CompoundTag tag = new CompoundTag();
        UUID id = donor.getUUID();
        tag.putUUID(NBT_DONOR, id);
        tag.putString("BloodDonorName", donor.getName().getString());
        out.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return out;
    }

    private static ItemStack exchange(Player player, InteractionHand hand, ItemStack empty, ItemStack filled) {
        empty.shrink(1);
        if (empty.isEmpty()) {
            return filled;
        }
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        return empty;
    }
}
