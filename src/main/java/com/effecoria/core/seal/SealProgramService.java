package com.effecoria.core.seal;

import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PsiContext;
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
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/** Server-side apply / clear for seal word programs. */
public final class SealProgramService {
    private SealProgramService() {}

    public enum ApplyStatus {
        OK,
        NOT_SEALS,
        NO_BLOCK,
        TOO_MANY_TOKENS,
        UNKNOWN_WORD,
        LOCKED_WORD,
        BAD_PROGRAM,
        NO_PSI
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
        int glow = 0;
        var passives = params.getList(SealProgramCompiler.PASSIVES_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < passives.size(); i++) {
            var a = passives.getCompound(i);
            String e = a.getString(SealProgramCompiler.EFFECT);
            if ("glow".equals(e) || "light".equals(e)) {
                glow = Math.max(glow, Math.round(a.getFloat(SealProgramCompiler.MAGNITUDE)));
            }
        }
        if (glow > 0) {
            SealService.attachGlowLight(level, pos, glow * 5f, params);
        }

        SealService.placeProgram(level, pos, player.getUUID(), Math.max(20f, cost * 8f), params);

        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.15f);
        player.displayClientMessage(
                Component.translatable("message.effecoria.seal.program_applied", tokens.size(), Math.round(cost)),
                true);
        return ApplyStatus.OK;
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
