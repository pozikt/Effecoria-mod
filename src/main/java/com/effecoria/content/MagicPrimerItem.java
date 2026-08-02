package com.effecoria.content;

import java.util.List;

import com.effecoria.client.ClientGuiHooks;
import com.effecoria.core.progression.PrimerChapters;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** In-world magic guide — leather-bound primer that gains pages over time. */
public class MagicPrimerItem extends Item {
    public MagicPrimerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientGuiHooks.openMagicGuide(null);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return false;
        }
        return hasUnseenClient();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.magic_primer.hint"));
        if (FMLEnvironment.dist == Dist.CLIENT && hasUnseenClient()) {
            tooltip.add(Component.translatable("item.effecoria.magic_primer.new_pages"));
        }
    }

    private static boolean hasUnseenClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return PrimerChapters.hasUnseen(PsiHelper.get(mc.player));
    }
}
