package com.effecoria.core.psi;

import com.effecoria.core.formula.PsiContext;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.phi.PhiHarness;
import com.effecoria.core.progression.BiologyService;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class PsiHelper {
    private PsiHelper() {}

    public static PlayerPsiData get(Player player) {
        return player.getData(ModAttachments.PSI.get());
    }

    public static void set(Player player, PlayerPsiData data) {
        player.setData(ModAttachments.PSI.get(), data);
    }

    public static PsiContext toContext(Player player, PlayerPsiData data) {
        long now = player.level().getGameTime();
        float breathFactor = data.overcastBreathFactor(now);
        float biology = data.effectiveBiologyQ() * BiologyService.bodyFactor(player) * breathFactor;
        float breath = data.breathingMastery() * breathFactor;
        PhiHarness.FocusBonuses focus = PhiHarness.focusBonuses(player);
        return new PsiContext(
                data.soulStrength(),
                data.currentPsi(),
                biology,
                data.frequencyHz(),
                data.school(),
                data.entropyB(),
                breath,
                data.essence(),
                data.exhaustion(),
                data.breathTrainRegenBonus(),
                data.isBreathTrainFatigued(),
                focus.costFloorRatio(),
                focus.resonanceWidthBonus(),
                data.overcastRegenMultiplier(now));
    }

    public static void initiate(Player player, MagicSchool school) {
        PlayerPsiData data = get(player);
        java.util.ArrayList<ResourceLocation> starters = new java.util.ArrayList<>(SpellProgression.starterSpells(school));
        for (ResourceLocation id : SpellProgression.commonStarterSpells()) {
            if (!starters.contains(id)) {
                starters.add(id);
            }
        }
        data.initiate(school, starters);
        set(player, data);
    }

    public static void reschool(Player player, MagicSchool school) {
        PlayerPsiData data = get(player);
        java.util.List<ResourceLocation> progression = SpellProgression.spellsForSchool(school);
        java.util.ArrayList<ResourceLocation> kept = new java.util.ArrayList<>(SpellProgression.starterSpells(school));
        for (ResourceLocation id : SpellProgression.commonStarterSpells()) {
            if (!kept.contains(id)) {
                kept.add(id);
            }
        }
        for (ResourceLocation id : data.knownSpells()) {
            if ((progression.contains(id) || SpellProgression.isCommon(id)) && !kept.contains(id)) {
                kept.add(id);
            }
        }
        data.reschool(school, kept);
        set(player, data);
    }
}
