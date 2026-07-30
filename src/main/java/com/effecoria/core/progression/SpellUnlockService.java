package com.effecoria.core.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /** Next spells in school progression that are not yet known (for hub ghost nodes). */
    public static List<ResourceLocation> upcomingLocked(PlayerPsiData data, int limit) {
        if (!data.initiated() || data.school() == MagicSchool.NONE || limit <= 0) {
            return List.of();
        }
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation spellId : SpellProgression.spellsForSchool(data.school())) {
            if (data.knownSpells().contains(spellId)) {
                continue;
            }
            if (!SpellRegistry.contains(spellId)) {
                continue;
            }
            out.add(spellId);
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    /** First unknown spell in progression — the only one research can unlock next. */
    public static Optional<ResourceLocation> nextUnlockCandidate(PlayerPsiData data) {
        List<ResourceLocation> upcoming = upcomingLocked(data, 1);
        return upcoming.isEmpty() ? Optional.empty() : Optional.of(upcoming.getFirst());
    }

    public static UnlockHint hintFor(PlayerPsiData data, ResourceLocation spellId) {
        if (data.knownSpells().contains(spellId)) {
            return UnlockHint.alreadyKnown();
        }
        Optional<SpellDefinition> defOpt = SpellRegistry.get(spellId);
        if (defOpt.isEmpty()) {
            return UnlockHint.missing();
        }
        SpellDefinition def = defOpt.get();
        Optional<ResourceLocation> next = nextUnlockCandidate(data);
        boolean nextInLine = next.isPresent() && next.get().equals(spellId);
        int needEssence = resolveUnlockCost(spellId, data.school(), def);
        return new UnlockHint(
                false,
                nextInLine,
                def.minMastery(),
                data.breathingMastery(),
                needEssence,
                data.essence());
    }

    public record UnlockHint(
            boolean known,
            boolean nextInLine,
            float needMastery,
            float haveMastery,
            int needEssence,
            int haveEssence) {
        public static UnlockHint alreadyKnown() {
            return new UnlockHint(true, false, 0f, 0f, 0, 0);
        }

        public static UnlockHint missing() {
            return new UnlockHint(false, false, 0f, 0f, 0, 0);
        }

        public boolean masteryMet() {
            return haveMastery >= needMastery;
        }

        public boolean essenceMet() {
            return needEssence <= 0 || haveEssence >= needEssence;
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
