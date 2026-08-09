package com.effecoria.core.progression;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Assign / clear player race and Orkanum baseline. */
public final class RaceService {
    private RaceService() {}

    public static boolean hasRace(PlayerPsiData data) {
        return data.race().isPresent();
    }

    /**
     * Permanent race pick. Returns false if already set and {@code force} is false.
     */
    public static boolean assign(ServerPlayer player, PlayerRace race, boolean force) {
        PlayerPsiData data = PsiHelper.get(player);
        if (hasRace(data) && !force) {
            return false;
        }
        PlayerRace previous = data.race().orElse(null);
        if (previous != null) {
            RaceTraitsService.clear(player, previous);
        }
        data.setRaceId(race.getSerializedName());
        BiologyService.applyRaceBaseline(data, BiologyService.baselineFor(race));
        RaceTraitsService.applyOnAssign(player, data, race);
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        return true;
    }

    public static void notifyAssigned(ServerPlayer player, PlayerRace race) {
        player.sendSystemMessage(Component.translatable(
                "message.effecoria.race_chosen",
                race.title()));
        FirstHourTips.tryShow(player, FirstHourTips.Tip.RACE);
    }
}
