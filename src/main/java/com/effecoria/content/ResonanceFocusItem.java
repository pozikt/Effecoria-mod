package com.effecoria.content;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        PlayerPsiData data = PsiHelper.get(serverPlayer);

        if (!data.initiated()) {
            MagicSchool school = player.isShiftKeyDown() ? MagicSchool.MENTAL : MagicSchool.ELEMENTAL;
            if (!SpellProgression.schoolHasLoadedSpells(school)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.effecoria.spells_not_loaded"));
                return InteractionResultHolder.fail(stack);
            }
            PsiHelper.initiate(serverPlayer, school);
            serverPlayer.syncData(ModAttachments.PSI.get());
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.effecoria.initiated",
                    Component.translatable("school.effecoria." + school.getSerializedName())));
            return InteractionResultHolder.consume(stack);
        }

        data.cycleSpell(1);
        PsiHelper.set(serverPlayer, data);
        serverPlayer.syncData(ModAttachments.PSI.get());
        ResourceLocation selected = data.selectedSpell();
        if (selected != null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.effecoria.spell_selected",
                            Component.translatable("spell.effecoria." + selected.getPath())),
                    true);
        }
        return InteractionResultHolder.success(stack);
    }
}
