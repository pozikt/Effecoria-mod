package com.effecoria.magic;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.formula.PsiContext;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.SpellEffectExecutor;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Optional;

public final class CastPipeline {
    public enum CastResult {
        SUCCESS,
        NOT_INITIATED,
        UNKNOWN_SPELL,
        CANNOT_CAST,
        NO_SPELL_SELECTED
    }

    private CastPipeline() {}

    public static CastResult tryCast(ServerPlayer player, ResourceLocation spellId) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            return CastResult.NOT_INITIATED;
        }

        Optional<SpellDefinition> spellOpt = SpellRegistry.get(spellId);
        if (spellOpt.isEmpty()) {
            return CastResult.UNKNOWN_SPELL;
        }

        SpellDefinition spell = spellOpt.get();
        if (!data.knownSpells().contains(spellId)) {
            return CastResult.UNKNOWN_SPELL;
        }

        PsiContext ctx = PsiHelper.toContext(data);
        boolean godMode = CreativeGodMode.isActive(player);
        PhiSample phi = PhiFieldService.sample(player.level(), player.position(), player);

        if (!godMode && !FormulaEngine.canCast(ctx, phi, spell, data.currentPsi())) {
            player.displayClientMessage(Component.translatable("message.effecoria.cast_failed"), true);
            return CastResult.CANNOT_CAST;
        }

        float cost = godMode ? 0f : FormulaEngine.spellCost(ctx, phi, spell);
        float power = FormulaEngine.spellPower(ctx, phi, spell);

        if (!godMode) {
            data.setCurrentPsi(data.currentPsi() - cost);
            float newEntropy = FormulaEngine.accumulateEntropy(data.entropyB(), power, spell.sideEntropyRatio());
            data.setEntropyB(newEntropy);

            if (FormulaEngine.isBacklashTriggered(newEntropy)) {
                applyBacklash(player);
                data.setEntropyB(0f);
            }
        }

        SpellEffectExecutor.applyAll(player, spell, power);
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());

        player.displayClientMessage(
                Component.translatable("message.effecoria.cast_success", Component.translatable("spell.effecoria." + spellId.getPath())),
                true);
        return CastResult.SUCCESS;
    }

    public static CastResult tryCastSelected(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        ResourceLocation selected = data.selectedSpell();
        if (selected == null) {
            return CastResult.NO_SPELL_SELECTED;
        }
        return tryCast(player, selected);
    }

    private static void applyBacklash(ServerPlayer player) {
        float damage = BalanceConfig.BACKLASH_DAMAGE.get().floatValue();
        player.hurt(player.level().damageSources().magic(), damage);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        player.displayClientMessage(Component.translatable("message.effecoria.backlash"), true);
    }
}
