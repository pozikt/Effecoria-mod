package com.effecoria.core.seal;

import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PsiContext;
import com.effecoria.core.glue.EssenceGlueData;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Server-side apply / clear for seal word programs. */
public final class SealProgramService {
    private SealProgramService() {}

    public enum ApplyStatus {
        OK,
        NOT_SEALS,
        NO_BLOCK,
        TOO_MANY_TOKENS,
        TOO_MANY_TARGETS,
        UNKNOWN_WORD,
        LOCKED_WORD,
        BAD_PROGRAM,
        NO_PSI,
        CONFLICT
    }

    public static ApplyStatus apply(ServerPlayer player, BlockPos pos, List<ResourceLocation> tokens) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated() || data.school() != com.effecoria.core.magic.MagicSchool.SEALS) {
            return ApplyStatus.NOT_SEALS;
        }
        ServerLevel level = player.serverLevel();
        if (level.getBlockState(pos).isAir()) {
            return ApplyStatus.NO_BLOCK;
        }

        float masteryRatio = BreathingService.referenceRatio(data.breathingMastery());
        int max = SealProgramCompiler.maxTokens(masteryRatio);
        if (tokens.size() > max) {
            return ApplyStatus.TOO_MANY_TOKENS;
        }

        for (ResourceLocation id : tokens) {
            var wordOpt = SealWordRegistry.get(id);
            if (wordOpt.isEmpty()) {
                return ApplyStatus.UNKNOWN_WORD;
            }
            SealWordDefinition word = wordOpt.get();
            if (!data.knowsSealWord(id)) {
                return ApplyStatus.LOCKED_WORD;
            }
            if (data.breathingMastery() < word.minMastery()) {
                return ApplyStatus.LOCKED_WORD;
            }
        }

        SealProgramCompiler.CompileResult compiled = SealProgramCompiler.compile(tokens);
        if (!compiled.ok()) {
            return ApplyStatus.BAD_PROGRAM;
        }

        PsiContext ctx = PsiHelper.toContext(player, data);
        float cost = compiled.totalPsiCost() * FormulaEngine.proficiencyCostFactor(ctx.breathingMastery(), 0f);
        cost = Math.max(1f, cost);
        if (data.currentPsi() < cost * 0.25f && data.currentPsi() <= 0f) {
            return ApplyStatus.NO_PSI;
        }

        // Spend Ψ (allow light overcast like spells)
        float before = data.currentPsi();
        data.setCurrentPsi(Math.max(0f, before - cost));
        PsiHelper.set(player, data);
        player.syncData(com.effecoria.core.psi.ModAttachments.PSI.get());

        CompoundTag params = compiled.params();
        SealProgramRuntime.migrateV1(params);
        placeCompiled(level, pos, player, cost, params);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.15f);
        player.displayClientMessage(
                Component.translatable("message.effecoria.seal.program_applied", tokens.size(), Math.round(cost)),
                true);
        return ApplyStatus.OK;
    }

    public static ApplyStatus applyScript(
            ServerPlayer player, BlockPos anchor, String source, Map<BlockPos, String> aliases) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated() || data.school() != com.effecoria.core.magic.MagicSchool.SEALS) {
            return ApplyStatus.NOT_SEALS;
        }
        ServerLevel level = player.serverLevel();
        if (level.getBlockState(anchor).isAir()) {
            return ApplyStatus.NO_BLOCK;
        }
        if (aliases != null && !aliases.isEmpty()) {
            EssenceGlueData glue = EssenceGlueData.get(level);
            for (Map.Entry<BlockPos, String> entry : aliases.entrySet()) {
                glue.setAlias(entry.getKey(), SealScriptLexicon.sanitizeAlias(entry.getValue()));
            }
        }
        SealSymbolTable table = SealSymbolTable.build(level, anchor, aliases == null ? Map.of() : aliases);
        SealScriptParser.Result parsed = SealScriptParser.parse(source, table);
        if (!parsed.ok()) {
            if (parsed.errors().stream().anyMatch(e -> e.startsWith("conflict:"))) {
                return ApplyStatus.CONFLICT;
            }
            return ApplyStatus.BAD_PROGRAM;
        }
        float masteryRatio = BreathingService.referenceRatio(data.breathingMastery());
        int maxTargets = SealProgramCompiler.maxTargets(masteryRatio);
        if (parsed.stanzas().size() > maxTargets) {
            return ApplyStatus.TOO_MANY_TARGETS;
        }
        for (SealScriptParser.Stanza stanza : parsed.stanzas()) {
            for (ResourceLocation id : stanza.usedWords()) {
                var wordOpt = SealWordRegistry.get(id);
                if (wordOpt.isEmpty()) {
                    return ApplyStatus.UNKNOWN_WORD;
                }
                if (!data.knowsSealWord(id) || data.breathingMastery() < wordOpt.get().minMastery()) {
                    return ApplyStatus.LOCKED_WORD;
                }
            }
        }
        float totalCost = 0f;
        for (SealScriptParser.Stanza stanza : parsed.stanzas()) {
            totalCost += stanza.psiCost();
        }
        PsiContext ctx = PsiHelper.toContext(player, data);
        totalCost = Math.max(1f, totalCost * FormulaEngine.proficiencyCostFactor(ctx.breathingMastery(), 0f));
        if (data.currentPsi() < totalCost * 0.25f && data.currentPsi() <= 0f) {
            return ApplyStatus.NO_PSI;
        }
        data.setCurrentPsi(Math.max(0f, data.currentPsi() - totalCost));
        PsiHelper.set(player, data);
        player.syncData(com.effecoria.core.psi.ModAttachments.PSI.get());

        int words = 0;
        for (SealScriptParser.Stanza stanza : parsed.stanzas()) {
            words += stanza.usedWords().size();
            placeCompiled(level, stanza.member().pos(), player, totalCost, stanza.params());
        }
        level.playSound(null, anchor, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.15f);
        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.seal.program_applied", words, Math.round(totalCost)),
                true);
        return ApplyStatus.OK;
    }

    private static void placeCompiled(
            ServerLevel level, BlockPos pos, ServerPlayer player, float cost, CompoundTag params) {
        SealService.placeProgram(level, pos, player.getUUID(), Math.max(20f, cost * 8f), params);
    }

    public static boolean clear(ServerPlayer player, BlockPos pos) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated() || data.school() != com.effecoria.core.magic.MagicSchool.SEALS) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        boolean removed = SealService.remove(level, pos);
        if (removed) {
            player.displayClientMessage(Component.translatable("message.effecoria.seal.program_cleared"), true);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.45f, 1.4f);
        }
        return removed;
    }

    public static boolean clearComponent(ServerPlayer player, BlockPos pos) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated() || data.school() != com.effecoria.core.magic.MagicSchool.SEALS) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        SealSymbolTable table = SealSymbolTable.build(level, pos);
        boolean removed = false;
        for (SealSymbolTable.Member member : table.members()) {
            removed |= SealService.remove(level, member.pos());
        }
        if (removed) {
            player.displayClientMessage(Component.translatable("message.effecoria.seal.program_cleared"), true);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.45f, 1.4f);
        }
        return removed;
    }

    public static float previewCost(PlayerPsiData data, List<ResourceLocation> tokens) {
        SealProgramCompiler.CompileResult compiled = SealProgramCompiler.compile(tokens);
        if (!compiled.ok()) {
            return 0f;
        }
        float mastery = data.breathingMastery();
        return Math.max(1f, compiled.totalPsiCost() * FormulaEngine.proficiencyCostFactor(mastery, 0f));
    }

    public static List<ResourceLocation> starterWordIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (SealWordDefinition word : SealWordRegistry.starters()) {
            ids.add(word.id());
        }
        return ids;
    }
}
