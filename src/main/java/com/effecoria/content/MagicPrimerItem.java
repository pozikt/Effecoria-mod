package com.effecoria.content;

import java.util.List;

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

/**
 * In-world magic guide. Client GUI opens via reflection so this class stays
 * safe to load on a dedicated server (no {@code net.minecraft.client} refs).
 */
public class MagicPrimerItem extends Item {
    public MagicPrimerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openGuideClient();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return FMLEnvironment.dist == Dist.CLIENT && hasUnseenPagesClient();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.magic_primer.hint"));
        if (FMLEnvironment.dist == Dist.CLIENT && hasUnseenPagesClient()) {
            tooltip.add(Component.translatable("item.effecoria.magic_primer.new_pages"));
        }
    }

    private static void openGuideClient() {
        try {
            Class.forName("com.effecoria.client.ClientGuiHooks")
                    .getMethod("openMagicGuide", Class.forName("net.minecraft.client.gui.screens.Screen"))
                    .invoke(null, new Object[] {null});
        } catch (ReflectiveOperationException ignored) {
            // Client-only; never expected on dedicated server.
        }
    }

    private static boolean hasUnseenPagesClient() {
        try {
            Object result = Class.forName("com.effecoria.client.ClientGuiHooks")
                    .getMethod("primerHasUnseenPages")
                    .invoke(null);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
