package com.effecoria.content;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Consumed on use — grants a burst of breathing technique mastery. */
public class BreathingScrollItem extends Item {
    public BreathingScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        PlayerPsiData data = PsiHelper.get(serverPlayer);
        if (!data.initiated()) {
            serverPlayer.displayClientMessage(Component.translatable("message.effecoria.not_initiated"), true);
            return InteractionResultHolder.fail(stack);
        }

        float before = data.breathingMastery();
        float gained = BreathingService.addMastery(
                data,
                BalanceConfig.BREATHING_SCROLL_GAIN.get().floatValue());
        if (gained <= 0f) {
            serverPlayer.displayClientMessage(Component.translatable("message.effecoria.breathing_max"), true);
            return InteractionResultHolder.fail(stack);
        }

        stack.shrink(1);
        BreathingService.sync(serverPlayer, data);
        BreathingService.notifyGain(serverPlayer, gained, "message.effecoria.breathing_scroll");
        BreathingService.notifyMilestones(serverPlayer, before, data.breathingMastery());
        return InteractionResultHolder.consume(stack);
    }
}
