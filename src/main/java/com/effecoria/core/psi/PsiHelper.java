package com.effecoria.core.psi;

import com.effecoria.core.formula.PsiContext;
import com.effecoria.core.magic.MagicSchool;
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
        float biology = data.effectiveBiologyQ() * BiologyService.bodyFactor(player);
        return new PsiContext(
                data.soulStrength(),
                data.currentPsi(),
                biology,
                data.frequencyHz(),
                data.school(),
                data.entropyB(),
                data.breathingMastery(),
                data.essence(),
                data.exhaustion(),
                data.breathTrainRegenBonus(),
                data.isBreathTrainFatigued());
    }

    public static void initiate(Player player, MagicSchool school) {
        PlayerPsiData data = get(player);
        data.initiate(school, SpellProgression.spellsForSchool(school));
        set(player, data);
    }

    public static void reschool(Player player, MagicSchool school) {
        PlayerPsiData data = get(player);
        data.reschool(school, SpellProgression.spellsForSchool(school));
        set(player, data);
    }
}
