package com.effecoria.content;

import com.effecoria.client.ClientGuiHooks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ResonanceFocusItem extends Item {
    public ResonanceFocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientGuiHooks.openResonanceFocusScreen(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
