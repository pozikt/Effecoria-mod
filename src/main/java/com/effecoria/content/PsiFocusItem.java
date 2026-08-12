package com.effecoria.content;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.tower.TowerFacility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Curios amulet which reports the linked Mage Tower's live status. */
public final class PsiFocusItem extends JewelryItem {
    public PsiFocusItem(Properties properties) { super(properties, "item.effecoria.psi_focus.hint", 0.5f); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            PlayerPsiData data = PsiHelper.get(sp);
            ServerLevel tower = data.towerDim() == null ? null : sp.server.getLevel(data.towerDim());
            TowerAnchorBlockEntity computer = tower == null || data.towerPos() == null ? null
                    : TowerFacility.findComputer(tower, data.towerPos()).orElse(null);
            if (computer == null) sp.displayClientMessage(Component.translatable("message.effecoria.tower.unbound"), true);
            else sp.displayClientMessage(computer.statusLine(), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
