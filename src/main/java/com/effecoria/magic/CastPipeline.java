package com.effecoria.magic;

import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.CastBlockReason;
import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.formula.PsiContext;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.phi.PhiHarness;
import com.effecoria.core.progression.EntropyService;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.progression.FirstHourTips;
import com.effecoria.core.progression.OvercastService;
import com.effecoria.core.progression.ProgressionService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.CastPresentation;
import com.effecoria.effect.SpellEffectExecutor;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

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
        return tryCast(player, spellId, 1f, null);
    }

    public static CastResult tryCast(ServerPlayer player, ResourceLocation spellId, float charge) {
        return tryCast(player, spellId, charge, null);
    }

    /**
     * @param forcedTarget when non-null, living-targeted effects hit this entity (including the caster)
     *                     instead of the look ray. Ops (perm 2) may also cast unknown spells this way for testing.
     */
    public static CastResult tryCast(
            ServerPlayer player, ResourceLocation spellId, float charge, @Nullable LivingEntity forcedTarget) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            player.displayClientMessage(Component.translatable("message.effecoria.not_initiated"), true);
            return CastResult.NOT_INITIATED;
        }

        Optional<SpellDefinition> spellOpt = SpellRegistry.get(spellId);
        if (spellOpt.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.effecoria.unknown_spell"), true);
            return CastResult.UNKNOWN_SPELL;
        }

        SpellDefinition spell = spellOpt.get();
        boolean opTestCast = player.hasPermissions(2) && forcedTarget != null;
        if (!data.knownSpells().contains(spellId) && !opTestCast) {
            player.displayClientMessage(Component.translatable("message.effecoria.spell_not_known"), true);
            return CastResult.UNKNOWN_SPELL;
        }

        float charge01 = Math.clamp(charge, 0f, 1f);
        float minPower = BalanceConfig.CAST_CHARGE_MIN_POWER.get().floatValue();
        float chargeScale = minPower + (1f - minPower) * charge01;

        PsiContext ctx = PsiHelper.toContext(player, data);
        boolean godMode = CreativeGodMode.isActive(player);
        PhiSample phi = PhiFieldService.sample(player.level(), player.position(), player);
        if (!godMode) {
            phi = PhiHarness.assistCast(player, phi);
        }

        float usablePsi = NecroSummonService.usablePsi(player, data);
        if (!godMode) {
            var block = FormulaEngine.diagnoseCannotCast(ctx, phi, spell, usablePsi);
            if (block.isPresent()) {
                if (block.get() == CastBlockReason.ZERO_FLUX
                        && com.effecoria.world.DeadWastelandService.isIn(player.level(), player.position())) {
                    player.displayClientMessage(
                            Component.translatable("message.effecoria.dead_wasteland_cast"), true);
                } else {
                    player.displayClientMessage(Component.translatable(block.get().messageKey()), true);
                }
                if (block.get() == CastBlockReason.LOW_PHI) {
                    FirstHourTips.tryShow(player, FirstHourTips.Tip.HARNESS);
                } else if (block.get() == CastBlockReason.ZERO_FLUX) {
                    FirstHourTips.tryShow(player, FirstHourTips.Tip.ZNPHI);
                }
                return CastResult.CANNOT_CAST;
            }
        }

        float fullCost = godMode ? 0f : FormulaEngine.spellCost(ctx, phi, spell) * chargeScale;
        fullCost *= com.effecoria.world.EssencePlateauService.spellCostMultiplier(player.level(), player.position());
        boolean overcasting = !godMode && usablePsi + 0.001f < fullCost;
        PsiContext powerCtx = overcasting ? ctx.withCurrentPsi(Math.max(usablePsi, fullCost)) : ctx;
        float power = FormulaEngine.spellPower(powerCtx, phi, spell) * chargeScale;
        power *= com.effecoria.world.EssencePlateauService.spellPowerMultiplier(player.level(), player.position());
        power *= com.effecoria.world.PhiGlassPlainService.spellPowerMultiplier(
                player.level(), player.position(), spell);
        power = CreativeGodMode.clampSpellPower(player, power);
        float hardCap = BalanceConfig.SPELL_POWER_HARD_CAP.get().floatValue();
        if (hardCap > 0f) {
            power = Math.min(power, hardCap);
        }

        CastPresentation.playWindUp(player, spell);
        CastDelivery delivery = SpellEffectExecutor.applyAll(player, spell, power, forcedTarget);
        CastPresentation.playResolve(player, spell, power, delivery);

        data.recordSpellCast(spellId, player.level().getGameTime());
        ProgressionService.onCastResolved(player, data, delivery);

        if (!godMode) {
            float costFraction = delivery == CastDelivery.WHIFF_NO_TARGET || delivery == CastDelivery.WHIFF_NO_BLOCK
                    ? BalanceConfig.WHIFF_COST_FRACTION.get().floatValue()
                    : 1f;
            float actualCost = fullCost * costFraction;
            float paid = Math.min(usablePsi, actualCost);
            float deficit = Math.max(0f, actualCost - usablePsi);
            data.setCurrentPsi(Math.max(0f, data.currentPsi() - paid));

            if (deficit > 0.05f && delivery == CastDelivery.FULL) {
                OvercastService.apply(player, data, deficit, actualCost);
            } else if (deficit > 0.05f) {
                OvercastService.apply(player, data, deficit * 0.5f, Math.max(actualCost, 0.01f));
            } else {
                ExhaustionService.onHealthyCast(player, data);
            }

            float entropyPower = delivery == CastDelivery.FULL ? power : actualCost;
            float newEntropy = FormulaEngine.accumulateEntropy(data.entropyB(), entropyPower, spell.sideEntropyRatio());
            newEntropy += com.effecoria.world.PhiGlassPlainService.spatialStormEntropyBonus(
                    player.level(), player.position(), spell.requiredSchool());
            // Spatial storm glitch — brief random look jitter
            if (spell.requiredSchool() == com.effecoria.core.magic.MagicSchool.SPATIAL
                    && com.effecoria.world.PhiGlassStormService.isStorming(
                            player.level(), player.blockPosition())
                    && player.getRandom().nextFloat() < 0.25f) {
                player.setYRot(player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * 24f);
            }
            data.setEntropyB(newEntropy);
            EntropyService.maybeWarnRising(player, data);

            if (FormulaEngine.isBacklashTriggered(newEntropy)) {
                applyBacklash(player, data);
                data.setEntropyB(0f);
            }
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());

        Component spellName = Component.translatable("spell.effecoria." + spellId.getPath());
        if (delivery == CastDelivery.WHIFF_NO_TARGET) {
            int whiffPercent = Math.round(BalanceConfig.WHIFF_COST_FRACTION.get().floatValue() * 100f);
            player.displayClientMessage(
                    Component.translatable("message.effecoria.cast_whiff_no_target", spellName, whiffPercent),
                    true);
            FirstHourTips.tryShow(player, FirstHourTips.Tip.FIRST_WHIFF);
        } else if (delivery == CastDelivery.WHIFF_NO_BLOCK) {
            int whiffPercent = Math.round(BalanceConfig.WHIFF_COST_FRACTION.get().floatValue() * 100f);
            player.displayClientMessage(
                    Component.translatable("message.effecoria.cast_whiff_no_block", spellName, whiffPercent),
                    true);
            FirstHourTips.tryShow(player, FirstHourTips.Tip.FIRST_WHIFF);
        } else if (overcasting) {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.cast_overcast_success", spellName),
                    true);
            FirstHourTips.tryShow(player, FirstHourTips.Tip.FIRST_CAST);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.cast_success", spellName),
                    true);
            FirstHourTips.tryShow(player, FirstHourTips.Tip.FIRST_CAST);
        }
        return CastResult.SUCCESS;
    }

    public static CastResult tryCastSelected(ServerPlayer player) {
        return tryCastSelected(player, 1f);
    }

    public static CastResult tryCastSelected(ServerPlayer player, float charge) {
        PlayerPsiData data = PsiHelper.get(player);
        ResourceLocation selected = data.selectedSpell();
        if (selected == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.no_spell_selected"), true);
            return CastResult.NO_SPELL_SELECTED;
        }
        return tryCast(player, selected, charge);
    }

    private static void applyBacklash(ServerPlayer player, PlayerPsiData data) {
        float damage = BalanceConfig.BACKLASH_DAMAGE.get().floatValue();
        player.hurt(player.level().damageSources().magic(), damage);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        ExhaustionService.onBacklash(player, data);
        player.displayClientMessage(Component.translatable("message.effecoria.backlash"), true);
        EntropyService.onBacklash(player, data);
    }
}
