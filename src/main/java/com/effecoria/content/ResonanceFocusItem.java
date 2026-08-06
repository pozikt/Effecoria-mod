package com.effecoria.content;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Initiation focus + tiered resonance gear.
 * Sneak-use with Essonite Dust (cost scales) or Pure Essonite (1) in the offhand to raise focus tier.
 * Client GUI opens via reflection so this class stays dedicated-server safe.
 */
public class ResonanceFocusItem extends Item {
    public ResonanceFocusItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return tryUpgrade(level, player, hand, stack);
        }
        if (level.isClientSide()) {
            openFocusScreenClient(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void openFocusScreenClient(Player player) {
        try {
            Class.forName("com.effecoria.client.ClientGuiHooks")
                    .getMethod("openResonanceFocusScreen", Player.class)
                    .invoke(null, player);
        } catch (ReflectiveOperationException ignored) {
            // Client-only; never expected on dedicated server.
        }
    }

    private InteractionResultHolder<ItemStack> tryUpgrade(
            Level level, Player player, InteractionHand hand, ItemStack focus) {
        int tier = PhiHarnessItems.focusTier(focus);
        if (tier >= PhiHarnessItems.FOCUS_MAX_TIER) {
            return InteractionResultHolder.fail(focus);
        }
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

        if (other.is(ModItems.PURE_ESSONITE.get())) {
            if (!level.isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    other.shrink(1);
                }
                PhiHarnessItems.setFocusTier(focus, tier + 1);
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS,
                        0.8f,
                        0.9f + tier * 0.1f);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.focus_upgraded_pure",
                                PhiHarnessItems.focusTier(focus)),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(focus, level.isClientSide());
        }

        if (!other.is(ModItems.ESSENITE_DUST.get())) {
            return InteractionResultHolder.pass(focus);
        }
        int need = dustForUpgrade(tier);
        if (other.getCount() < need && !player.getAbilities().instabuild) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("message.effecoria.focus_need_dust", need), true);
            }
            return InteractionResultHolder.fail(focus);
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                other.shrink(need);
            }
            PhiHarnessItems.setFocusTier(focus, tier + 1);
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS,
                    0.7f,
                    1.1f + tier * 0.1f);
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.focus_upgraded", PhiHarnessItems.focusTier(focus)),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(focus, level.isClientSide());
    }

    private static int dustForUpgrade(int currentTier) {
        return switch (currentTier) {
            case 0 -> 4;
            case 1 -> 8;
            default -> 16;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PhiHarnessItems.appendFocusTooltip(stack, tooltip);
    }
}
