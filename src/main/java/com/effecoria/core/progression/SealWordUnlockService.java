package com.effecoria.core.progression;

import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.seal.SealWordDefinition;
import com.effecoria.core.seal.SealWordRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Unlocks seal lexicon words as breathing mastery rises. */
public final class SealWordUnlockService {
    private SealWordUnlockService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        if (!data.initiated() || data.school() != MagicSchool.SEALS) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        for (SealWordDefinition word : SealWordRegistry.progressionOrder()) {
            ResourceLocation id = word.id();
            if (data.knowsSealWord(id)) {
                continue;
            }
            if (data.breathingMastery() < word.minMastery()) {
                continue;
            }
            data.unlockSealWord(id);
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.seal.word_unlocked",
                            Component.translatable("seal_word.effecoria." + id.getPath())),
                    true);
            return;
        }
    }
}
