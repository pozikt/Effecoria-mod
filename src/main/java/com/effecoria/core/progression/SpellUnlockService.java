package com.effecoria.core.progression;

import java.util.List;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.magic.SpellRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Unlocks school spells from progression when mastery and essence requirements are met. */
public final class SpellUnlockService {
    private SpellUnlockService() {}

    public static void tick(ServerPlayer player, PlayerPsiData data) {
        if (!data.initiated() || data.school() == MagicSchool.NONE) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        List<ResourceLocation> progression = SpellProgression.spellsForSchool(data.school());
        for (ResourceLocation spellId : progression) {
            if (data.knownSpells().contains(spellId)) {
                continue;
            }
            if (!SpellRegistry.contains(spellId)) {
                continue;
            }
            SpellDefinition def = SpellRegistry.get(spellId).orElseThrow();
            if (data.breathingMastery() < def.minMastery()) {
                continue;
            }
            int cost = resolveUnlockCost(spellId, data.school(), def);
            if (cost > 0 && data.essence() < cost) {
                continue;
            }
            if (cost > 0) {
                data.addEssence(-cost);
            }
            data.unlockSpell(spellId);
            player.displayClientMessage(
                    Component.translatable(
                            cost > 0
                                    ? "message.effecoria.spell_unlocked_essence"
                                    : "message.effecoria.spell_unlocked",
                            Component.translatable("spell.effecoria." + spellId.getPath()),
                            cost),
                    true);
            return;
        }
    }

    public static int resolveUnlockCost(ResourceLocation spellId, MagicSchool school, SpellDefinition def) {
        if (def.unlockEssenceCost() >= 0) {
            return def.unlockEssenceCost();
        }
        int index = SpellProgression.progressionIndex(school, spellId);
        if (index < 0) {
            return 99;
        }
        int starters = BalanceConfig.SPELL_STARTER_COUNT.get();
        if (index < starters) {
            return 0;
        }
        int step = BalanceConfig.SPELL_UNLOCK_ESSENCE_STEP.get();
        int tier = (index - starters) / 3;
        return 1 + tier * step;
    }
}
