package com.effecoria.command;

import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.magic.CastPipeline;
import com.effecoria.magic.SpellRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class EffecoriaCommands {
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
                        .executes(ctx -> listSpells(ctx.getSource()))));
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
                data.breathingTier(),
                String.format("%.1f", data.trainingXp()),
                String.format("%.2f", data.soulStrength()),
                String.format("%.2f", data.effectiveBiologyQ())), false);
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
