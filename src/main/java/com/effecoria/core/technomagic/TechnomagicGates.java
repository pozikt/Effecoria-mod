package com.effecoria.core.technomagic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Soft progression gates: craft stays free; operating Era N+ gear requires completing all
 * available nodes of earlier eras. Resource chains (heat, cells, flux slugs) still apply.
 */
public final class TechnomagicGates {
    private TechnomagicGates() {}

    /**
     * @return true if the player may open / start Era {@code era} machines
     */
    public static boolean canOperate(ServerPlayer player, TechnomagicEra era) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return TechnomagicProgress.get(player).canOperateEra(era);
    }

    /**
     * Like {@link #canOperate} but shows a lock message when false.
     */
    public static boolean checkOperate(ServerPlayer player, TechnomagicEra era) {
        if (canOperate(player, era)) {
            return true;
        }
        TechnomagicEra missing = TechnomagicProgress.get(player).firstIncompleteEraBefore(era);
        if (missing != null) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.technomagic_era_locked",
                            Component.translatable(era.translationKey()),
                            Component.translatable(missing.translationKey())),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.technomagic_era_locked_generic",
                            Component.translatable(era.translationKey())),
                    true);
        }
        return false;
    }
}
