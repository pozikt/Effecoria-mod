package com.effecoria.command;

import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.magic.CastPipeline;
import com.effecoria.magic.SpellRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class EffecoriaCommands {
    private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{
                    "psi", "max_psi", "essence", "breathing", "soul", "biology_q",
                    "phi_mult", "entropy", "training_xp"
            }, builder);

    private EffecoriaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("effecoria")
                .then(Commands.literal("debug")
                        .executes(ctx -> debug(ctx.getSource())))
                .then(Commands.literal("initiate")
                        .then(Commands.argument("school", StringArgumentType.word())
                                .executes(ctx -> initiate(ctx.getSource(), StringArgumentType.getString(ctx, "school")))))
                .then(Commands.literal("reschool")
                        .then(Commands.argument("school", StringArgumentType.word())
                                .executes(ctx -> reschool(ctx.getSource(), StringArgumentType.getString(ctx, "school")))))
                .then(Commands.literal("cast")
                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                .executes(ctx -> cast(ctx.getSource(), ResourceLocationArgument.getId(ctx, "spell")))))
                .then(Commands.literal("spells")
                        .executes(ctx -> listSpells(ctx.getSource())))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests(STAT_SUGGESTIONS)
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0f))
                                        .executes(ctx -> setStat(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "stat"),
                                                FloatArgumentType.getFloat(ctx, "value")))))));
    }

    private static int debug(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);
        PhiSample phi = PhiFieldService.sample(player.level(), player.position(), player);

        String phiDisplay = phi.isInfinite() ? "∞" : String.format("%.2f", phi.effectiveValue());
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.debug",
                String.format("%.1f", data.currentPsi()),
                String.format("%.1f", data.maxPsi()),
                phiDisplay,
                data.school().getSerializedName(),
                String.format("%.2f", data.entropyB()),
                data.initiated()), false);
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.debug_progression",
                BreathingService.formatTotalPercent(data.breathingMastery()),
                data.essence(),
                String.format("%.2f", data.mastery()),
                String.format("%.1f", data.trainingXp()),
                String.format("%.2f", data.soulStrength()),
                String.format("%.2f", data.effectiveBiologyQ())), false);
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.debug_mult",
                String.format("%.2f", data.phiMultiplier())), false);
        return 1;
    }

    private static int setStat(CommandSourceStack source, String statName, float value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);
        String stat = statName.toLowerCase(Locale.ROOT);
        String display;

        switch (stat) {
            case "psi", "current_psi" -> {
                data.setCurrentPsi(value);
                display = String.format("Ψ = %.1f", data.currentPsi());
            }
            case "max_psi" -> {
                data.setMaxPsi(value);
                display = String.format("max Ψ = %.1f", data.maxPsi());
            }
            case "essence" -> {
                data.setEssence(Math.round(value));
                display = "essence = " + data.essence();
            }
            case "breathing", "breathing_mastery" -> {
                float mastery = value > 1f ? value / 100f : value;
                data.setBreathingMastery(mastery);
                display = "breathing = " + BreathingService.formatTotalPercent(data.breathingMastery()) + "%";
            }
            case "soul", "soul_strength" -> {
                data.setSoulStrength(value);
                display = String.format("Ψ_soul = %.2f", data.soulStrength());
            }
            case "biology_q", "q" -> {
                data.setBiologyQ(value);
                display = String.format("Q_biology = %.2f", data.biologyQ());
            }
            case "phi_mult", "phi", "phi_multiplier" -> {
                data.setPhiMultiplier(value);
                display = String.format("Φ mult = ×%.2f", data.phiMultiplier());
            }
            case "entropy", "entropy_b" -> {
                data.setEntropyB(value);
                display = String.format("entropy = %.2f", data.entropyB());
            }
            case "training_xp", "train_xp" -> {
                data.setTrainingXp(value);
                display = String.format("training XP = %.1f", data.trainingXp());
            }
            default -> {
                source.sendFailure(Component.translatable("message.effecoria.invalid_stat"));
                return 0;
            }
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        String finalDisplay = display;
        source.sendSuccess(() -> Component.translatable("message.effecoria.stat_set", finalDisplay), true);
        return 1;
    }

    private static int initiate(CommandSourceStack source, String schoolName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);
        MagicSchool school = resolveSchool(source, schoolName);
        if (school == null) {
            return 0;
        }
        if (data.initiated()) {
            return reschool(source, schoolName);
        }
        PsiHelper.initiate(player, school);
        player.syncData(ModAttachments.PSI.get());
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.initiated",
                Component.translatable("school.effecoria." + school.getSerializedName())), false);
        return 1;
    }

    private static int reschool(CommandSourceStack source, String schoolName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MagicSchool school = resolveSchool(source, schoolName);
        if (school == null) {
            return 0;
        }
        PsiHelper.reschool(player, school);
        player.syncData(ModAttachments.PSI.get());
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.reschool",
                Component.translatable("school.effecoria." + school.getSerializedName())), false);
        return 1;
    }

    private static MagicSchool resolveSchool(CommandSourceStack source, String schoolName) {
        MagicSchool school = MagicSchool.fromSerializedName(schoolName);
        if (!school.isPlayable()) {
            source.sendFailure(Component.translatable("message.effecoria.invalid_school"));
            return null;
        }
        if (!SpellProgression.schoolHasLoadedSpells(school)) {
            source.sendFailure(Component.translatable("message.effecoria.spells_not_loaded"));
            return null;
        }
        return school;
    }

    private static int cast(CommandSourceStack source, ResourceLocation spellId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CastPipeline.CastResult result = CastPipeline.tryCast(player, spellId);
        if (result == CastPipeline.CastResult.SUCCESS) {
            return 1;
        }
        source.sendFailure(Component.translatable("message.effecoria.cast_command_failed", result.name()));
        return 0;
    }

    private static int listSpells(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.initiated()) {
            source.sendFailure(Component.translatable("message.effecoria.not_initiated"));
            return 0;
        }
        for (ResourceLocation spellId : data.knownSpells()) {
            SpellRegistry.get(spellId).ifPresent(spell -> source.sendSuccess(
                    () -> Component.literal(spellId + " [" + spell.requiredSchool().getSerializedName() + "]"),
                    false));
        }
        return 1;
    }
}
