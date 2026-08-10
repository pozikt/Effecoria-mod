package com.effecoria.content;

import com.effecoria.armor.EssoniteArmorData;
import com.effecoria.armor.EssonitePhoneme;
import com.effecoria.core.artifact.AssembledGearData;
import com.effecoria.core.artifact.ModularPartData;

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

/** Seal Φ-phoneme scroll — use with armor or modular part in the other hand. */
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
        if (EssoniteArmorData.isEssonite(other)) {
            if (!level.isClientSide()) {
                EssoniteArmorData.setPhoneme(other, phoneme);
                consume(scroll, player, level);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.phoneme_applied",
                                Component.translatable("item.effecoria.essonite_armor.phoneme." + phoneme.id())),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(scroll, level.isClientSide());
        }
        if (ModularPartData.isPart(other)) {
            if (!level.isClientSide()) {
                ModularPartData.addPhoneme(other, phoneme);
                consume(scroll, player, level);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.phoneme_applied_part",
                                Component.translatable("item.effecoria.essonite_armor.phoneme." + phoneme.id())),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(scroll, level.isClientSide());
        }
        if (AssembledGearData.isStaff(other)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.effecoria.phoneme_need_part"), true);
            }
            return InteractionResultHolder.fail(scroll);
        }
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.effecoria.phoneme_need_armor"), true);
        }
        return InteractionResultHolder.fail(scroll);
    }

    private void consume(ItemStack scroll, Player player, Level level) {
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
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.phi_phoneme.hint"));
        tooltip.add(Component.translatable("item.effecoria.phi_phoneme.hint_part"));
        tooltip.add(Component.translatable("item.effecoria.essonite_armor.phoneme." + phoneme.id()));
    }
}
