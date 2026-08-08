package com.effecoria.content;

import com.effecoria.armor.EssoniteArmorData;
import com.effecoria.armor.EssonitePhoneme;

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

import java.util.List;

/** Seal Φ-phoneme scroll — sneak-use while looking / holding armor to inscribe. */
public class PhiPhonemeScrollItem extends Item {
    private final EssonitePhoneme phoneme;

    public PhiPhonemeScrollItem(Properties properties, EssonitePhoneme phoneme) {
        super(properties);
        this.phoneme = phoneme;
    }

    public EssonitePhoneme phoneme() {
        return phoneme;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack scroll = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!EssoniteArmorData.isEssonite(other)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("message.effecoria.phoneme_need_armor"), true);
            }
            return InteractionResultHolder.fail(scroll);
        }
        if (!level.isClientSide()) {
            EssoniteArmorData.setPhoneme(other, phoneme);
            if (!player.getAbilities().instabuild) {
                scroll.shrink(1);
            }
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.1f);
            player.displayClientMessage(
                    Component.translatable("message.effecoria.phoneme_applied", Component.translatable(
                            "item.effecoria.essonite_armor.phoneme." + phoneme.id())),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(scroll, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.phi_phoneme.hint"));
        tooltip.add(Component.translatable("item.effecoria.essonite_armor.phoneme." + phoneme.id()));
    }
}
