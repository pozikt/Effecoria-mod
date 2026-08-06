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
 * Rare golem heart — sneak-use with a Phi Cell in the other hand to fully recharge the cell
 * (consumes the core).
 */
public final class VitrifiedGolemCoreItem extends Item {
    public VitrifiedGolemCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack core = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(core);
        }
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.is(ModItems.PHI_CELL.get())) {
            return InteractionResultHolder.pass(core);
        }
        float charge = PhiHarnessItems.cellCharge(other);
        if (charge >= 0.999f) {
            return InteractionResultHolder.fail(core);
        }
        if (!level.isClientSide()) {
            PhiHarnessItems.setCellCharge(other, 1.0f);
            if (!player.getAbilities().instabuild) {
                core.shrink(1);
            }
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS,
                    0.7f,
                    1.35f);
            player.displayClientMessage(
                    Component.translatable("message.effecoria.golem_core_cell_charged"), true);
        }
        return InteractionResultHolder.sidedSuccess(core, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.vitrified_golem_core.hint"));
    }
}
