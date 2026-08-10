package com.effecoria.content;

import com.effecoria.core.artifact.ItemSealCatalog;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Discovers a random unknown item seal when used by a Seals mage. */
public class ItemSealPrimerItem extends Item {
    public ItemSealPrimerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        PlayerPsiData data = PsiHelper.get(player);
        if (data.school() != com.effecoria.core.magic.MagicSchool.SEALS && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.effecoria.item_seal_need_seals"), true);
            return InteractionResultHolder.fail(stack);
        }
        List<ResourceLocation> unknown = new ArrayList<>();
        for (var def : ItemSealCatalog.sorted()) {
            if (!data.knowsItemSeal(def.id())) {
                unknown.add(def.id());
            }
        }
        if (unknown.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.effecoria.item_seal_all_known"), true);
            return InteractionResultHolder.fail(stack);
        }
        ResourceLocation unlock = unknown.get(level.random.nextInt(unknown.size()));
        data.unlockItemSeal(unlock);
        PsiHelper.set(player, data);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.2f);
        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.item_seal_unlocked",
                        Component.translatable("item_seal.effecoria." + unlock.getPath())),
                true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.item_seal_primer.hint"));
    }
}
