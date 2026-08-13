package com.effecoria.content;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.tower.TowerFacility;
import com.effecoria.core.tower.TowerReturnProtocol;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Personal tower amulet (Curios).
 * Use: status. Sneak-use: channel the Return Protocol.
 */
public final class PsiFocusItem extends JewelryItem {
    public PsiFocusItem(Properties properties) {
        super(properties, "item.effecoria.psi_focus.hint", 0.5f);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return TowerReturnProtocol.CHANNEL_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                if (player.getCooldowns().isOnCooldown(this)) {
                    sp.displayClientMessage(Component.translatable("message.effecoria.tower.return_cooldown"), true);
                    return InteractionResultHolder.fail(stack);
                }
                TowerReturnProtocol.Quote quote = TowerReturnProtocol.quote(sp);
                if (quote.distance() <= 0 && !PsiHelper.get(sp).towerBound()) {
                    sp.displayClientMessage(Component.translatable("message.effecoria.tower.unbound"), true);
                    return InteractionResultHolder.fail(stack);
                }
                sp.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.tower.return_channel",
                                String.format("%.0f", quote.distance()),
                                String.format("%.0f", quote.psiCost()),
                                quote.phiCost()),
                        true);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            reportStatus(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer sp) {
            TowerReturnProtocol.tryReturn(sp);
        }
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide()
                && entity instanceof ServerPlayer sp
                && timeLeft > 0
                && entity.isShiftKeyDown()) {
            sp.displayClientMessage(Component.translatable("message.effecoria.tower.return_cancelled"), true);
        }
    }

    private static void reportStatus(ServerPlayer sp) {
        PlayerPsiData data = PsiHelper.get(sp);
        ServerLevel tower = data.towerDim() == null ? null : sp.server.getLevel(data.towerDim());
        TowerAnchorBlockEntity computer = tower == null || data.towerPos() == null
                ? null
                : TowerFacility.findComputer(tower, data.towerPos()).orElse(null);
        if (computer == null) {
            sp.displayClientMessage(Component.translatable("message.effecoria.tower.unbound"), true);
        } else {
            sp.displayClientMessage(computer.statusLine(), true);
            TowerReturnProtocol.Quote quote = TowerReturnProtocol.quote(sp);
            sp.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.tower.return_quote",
                            String.format("%.0f", quote.distance()),
                            String.format("%.0f", quote.psiCost()),
                            quote.phiCost()),
                    false);
        }
    }
}
