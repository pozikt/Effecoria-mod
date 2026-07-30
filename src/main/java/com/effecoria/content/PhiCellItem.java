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
 * Portable Φ buffer. Drains on cast in low-Φ zones via {@link com.effecoria.core.phi.PhiHarness}.
 * Sneak-use with Essonite Dust in the offhand to refill.
 */
public class PhiCellItem extends Item {
    public PhiCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cell = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(cell);
        }
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.is(ModItems.ESSENITE_DUST.get())) {
            return InteractionResultHolder.pass(cell);
        }
        float charge = PhiHarnessItems.cellCharge(cell);
        if (charge >= 0.999f) {
            return InteractionResultHolder.fail(cell);
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                other.shrink(1);
            }
            PhiHarnessItems.setCellCharge(cell, charge + 0.25f);
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.6f,
                    1.4f);
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.phi_cell_charged",
                            Math.round(PhiHarnessItems.cellCharge(cell) * 100f)),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(cell, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PhiHarnessItems.appendCellTooltip(stack, tooltip);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * PhiHarnessItems.cellCharge(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float charge = PhiHarnessItems.cellCharge(stack);
        return charge > 0.35f ? 0x55AAFF : 0x8855AA;
    }
}
