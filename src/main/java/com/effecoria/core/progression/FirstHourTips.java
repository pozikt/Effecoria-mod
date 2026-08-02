package com.effecoria.core.progression;

import com.effecoria.content.ModItems;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * First-hour teaching beats — short action-bar / chat tips pointing at the Magic Primer.
 * Tip bits live on {@link PlayerPsiData#primerTipsMask()}.
 */
public final class FirstHourTips {
    public enum Tip {
        INITIATED(0),
        OPEN_HUB(1),
        FIRST_CAST(2),
        FIRST_WHIFF(3),
        ENTROPY(4),
        SEALS(5);

        private final int bit;

        Tip(int bit) {
            this.bit = bit;
        }

        public int mask() {
            return 1 << bit;
        }
    }

    private FirstHourTips() {}

    public static boolean hasSeen(PlayerPsiData data, Tip tip) {
        return (data.primerTipsMask() & tip.mask()) != 0;
    }

    /** Shows tip once; syncs PSI attachment. Returns true if newly shown. */
    public static boolean tryShow(ServerPlayer player, Tip tip) {
        PlayerPsiData data = PsiHelper.get(player);
        if (hasSeen(data, tip)) {
            return false;
        }
        data.setPrimerTipsMask(data.primerTipsMask() | tip.mask());
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());

        Component body = Component.translatable("tip.effecoria." + tip.name().toLowerCase());
        player.displayClientMessage(body, true);
        player.sendSystemMessage(Component.translatable(
                "tip.effecoria.primer_hint",
                Component.translatable("item.effecoria.magic_primer")));
        return true;
    }

    public static void onInitiated(ServerPlayer player, MagicSchool school) {
        givePrimer(player);
        tryShow(player, Tip.INITIATED);
        if (school == MagicSchool.SEALS) {
            tryShow(player, Tip.SEALS);
        }
    }

    public static void givePrimer(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.MAGIC_PRIMER.get())) {
                return;
            }
        }
        if (player.getOffhandItem().is(ModItems.MAGIC_PRIMER.get())) {
            return;
        }
        ItemStack primer = new ItemStack(ModItems.MAGIC_PRIMER.get());
        if (!player.addItem(primer)) {
            player.drop(primer, false);
        }
    }
}