package com.effecoria.command;

import com.effecoria.entity.MirageHorrorEntity;
import com.effecoria.config.BalanceConfig;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.magic.MobMagicService;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.progression.BiologyService;
import com.effecoria.core.progression.BreathingService;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.core.seal.SealWordDefinition;
import com.effecoria.core.seal.SealWordRegistry;
import com.effecoria.magic.CastPipeline;
import com.effecoria.magic.SpellRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class EffecoriaCommands {
    private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(new String[]{
                    "psi", "max_psi", "essence", "breathing", "soul", "biology_q",
                    "phi_mult", "entropy", "training_xp"
            }, builder);

    private static final SuggestionProvider<CommandSourceStack> SCHOOL_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(MagicSchool.values())
                            .filter(MagicSchool::isPlayable)
                            .map(MagicSchool::getSerializedName)
                            .collect(Collectors.toList()),
                    builder);

    private static final SuggestionProvider<CommandSourceStack> RACE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(com.effecoria.core.progression.PlayerRace.values())
                            .map(com.effecoria.core.progression.PlayerRace::getSerializedName)
                            .collect(Collectors.toList()),
                    builder);

    private EffecoriaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("effecoria")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("version")
                        .executes(ctx -> version(ctx.getSource())))
                .then(Commands.literal("debug")
                        .executes(ctx -> debug(ctx.getSource())))
                .then(Commands.literal("race")
                        .executes(ctx -> showRace(ctx.getSource()))
                        .then(Commands.argument("race", StringArgumentType.word())
                                .requires(source -> source.hasPermission(2))
                                .suggests(RACE_SUGGESTIONS)
                                .executes(ctx -> setRace(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "race"),
                                        false))))
                .then(Commands.literal("rerace")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("race", StringArgumentType.word())
                                .suggests(RACE_SUGGESTIONS)
                                .executes(ctx -> setRace(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "race"),
                                        true))))
                .then(Commands.literal("initiate")
                        .then(Commands.argument("school", StringArgumentType.word())
                                .suggests(SCHOOL_SUGGESTIONS)
                                .executes(ctx -> initiate(ctx.getSource(), StringArgumentType.getString(ctx, "school")))))
                .then(Commands.literal("reschool")
                        .then(Commands.argument("school", StringArgumentType.word())
                                .suggests(SCHOOL_SUGGESTIONS)
                                .executes(ctx -> reschool(ctx.getSource(), StringArgumentType.getString(ctx, "school")))))
                .then(Commands.literal("cast")
                        .then(Commands.argument("spell", ResourceLocationArgument.id())
                                .executes(ctx -> cast(
                                        ctx.getSource(),
                                        ResourceLocationArgument.getId(ctx, "spell"),
                                        null))
                                .then(Commands.literal("self")
                                        .executes(ctx -> cast(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "spell"),
                                                ctx.getSource().getPlayerOrException())))
                                .then(Commands.literal("nearest")
                                        .executes(ctx -> castAtNearby(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "spell"),
                                                false)))
                                .then(Commands.literal("random")
                                        .executes(ctx -> castAtNearby(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "spell"),
                                                true)))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> cast(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "spell"),
                                                EntityArgument.getEntity(ctx, "target"))))))
                .then(Commands.literal("initiateMob")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> initiateMob(ctx.getSource(), null))
                        .then(Commands.argument("school", StringArgumentType.word())
                                .suggests(SCHOOL_SUGGESTIONS)
                                .executes(ctx -> initiateMob(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "school")))))
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
                                                FloatArgumentType.getFloat(ctx, "value"))))))
                .then(Commands.literal("max")
                        .executes(ctx -> maxMagic(ctx.getSource(), null))
                        .then(Commands.argument("school", StringArgumentType.word())
                                .suggests(SCHOOL_SUGGESTIONS)
                                .executes(ctx -> maxMagic(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "school")))))
                .then(Commands.literal("horror")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> spawnHorrorPreview(ctx.getSource())))
                .then(Commands.literal("subspaceSpeed")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> subspaceSpeedGet(ctx.getSource()))
                        .then(Commands.argument("speed", IntegerArgumentType.integer(0, 4096))
                                .executes(ctx -> subspaceSpeedSet(
                                        ctx.getSource(), IntegerArgumentType.getInteger(ctx, "speed"))))));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Effecoria: help | version | debug | race [id] | rerace <id> | initiate <school> | reschool <school> | initiateMob [school] | cast <id> [self|nearest|random|@e] | spells | max [school] | set <stat> <value> | horror | subspaceSpeed [0-4096]"),
                false);
        return 1;
    }

    private static int version(CommandSourceStack source) {
        String ver = net.neoforged.fml.ModList.get()
                .getModContainerById(com.effecoria.EffecoriaMod.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        source.sendSuccess(() -> Component.literal("Effecoria " + ver), false);
        return 1;
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
                String.format("%.2f", data.effectiveBiologyQ() * BiologyService.bodyFactor(player)),
                String.format("%.0f", data.exhaustion())), false);
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.debug_mult",
                String.format("%.2f", data.phiMultiplier())), false);
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.debug_race",
                data.race().map(r -> r.getSerializedName()).orElse("none"),
                String.format("%.2f", data.biologyQ())), false);
        String ver = net.neoforged.fml.ModList.get()
                .getModContainerById(com.effecoria.EffecoriaMod.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
        source.sendSuccess(() -> Component.literal("Effecoria build " + ver), false);
        return 1;
    }

    private static int showRace(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);
        if (data.race().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.effecoria.race_none"), false);
            return 1;
        }
        var race = data.race().get();
        source.sendSuccess(() -> Component.translatable(
                "message.effecoria.race_current",
                race.title(),
                String.format("%.2f", BiologyService.baselineFor(race))), false);
        return 1;
    }

    private static int setRace(CommandSourceStack source, String raceName, boolean force) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var raceOpt = com.effecoria.core.progression.PlayerRace.byId(raceName);
        if (raceOpt.isEmpty()) {
            source.sendFailure(Component.translatable("message.effecoria.invalid_race"));
            return 0;
        }
        boolean ok = com.effecoria.core.progression.RaceService.assign(player, raceOpt.get(), force);
        if (!ok) {
            source.sendFailure(Component.translatable("message.effecoria.race_already_chosen"));
            return 0;
        }
        com.effecoria.core.progression.RaceService.notifyAssigned(player, raceOpt.get());
        source.sendSuccess(() -> Component.translatable("message.effecoria.race_set", raceOpt.get().title()), true);
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
            case "exhaustion" -> {
                data.setExhaustion(value);
                display = String.format("exhaustion = %.0f", data.exhaustion());
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

    /**
     * Test helper: push magic stats to progression caps and unlock the full school spell list.
     * Optional {@code school} initiates/reschools first. Requires permission level 2.
     */
    private static int maxMagic(CommandSourceStack source, String schoolName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);

        if (schoolName != null) {
            MagicSchool school = resolveSchool(source, schoolName);
            if (school == null) {
                return 0;
            }
            if (data.race().isEmpty()) {
                source.sendFailure(Component.translatable("message.effecoria.race_required"));
                return 0;
            }
            if (data.initiated()) {
                PsiHelper.reschool(player, school);
            } else {
                PsiHelper.initiate(player, school);
            }
            data = PsiHelper.get(player);
        } else if (!data.initiated()) {
            source.sendFailure(Component.translatable("message.effecoria.max_need_school"));
            return 0;
        }

        float hard = BalanceConfig.BREATHING_HARD_CAP.get().floatValue();
        float maxBreath = hard > 0f
                ? hard
                : BalanceConfig.BREATHING_MAX_MASTERY.get().floatValue() * 5f;
        float maxSoul = BalanceConfig.TRAINING_MAX_SOUL.get().floatValue();
        float maxPsi = BalanceConfig.TRAINING_MAX_PSI_CAP.get().floatValue();

        data.setBreathingMastery(maxBreath);
        data.setSoulStrength(maxSoul);
        data.setMaxPsi(maxPsi);
        data.setCurrentPsi(maxPsi);
        data.setEssence(999);
        data.setBiologyQ(1f);
        data.setPhiMultiplier(1f);
        data.setEntropyB(0f);
        data.setExhaustion(0f);
        data.setTrainingXp(0f);

        for (ResourceLocation spellId : SpellProgression.spellsForSchool(data.school())) {
            if (SpellRegistry.contains(spellId)) {
                data.unlockSpell(spellId);
            }
        }
        if (data.school() == MagicSchool.SEALS) {
            for (SealWordDefinition word : SealWordRegistry.all().values()) {
                data.unlockSealWord(word.id());
            }
        }

        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());
        String schoolKey = data.school().getSerializedName();
        int unlockCount = data.school() == MagicSchool.SEALS
                ? data.knownSealWords().size()
                : data.knownSpells().size();
        String breathPct = BreathingService.formatTotalPercent(data.breathingMastery());
        int psiCap = (int) data.maxPsi();
        int essence = data.essence();
        String messageKey = data.school() == MagicSchool.SEALS
                ? "message.effecoria.max_magic_seals"
                : "message.effecoria.max_magic";
        source.sendSuccess(
                () -> Component.translatable(
                        messageKey,
                        Component.translatable("school.effecoria." + schoolKey),
                        unlockCount,
                        breathPct,
                        psiCap,
                        essence),
                true);
        return 1;
    }

    private static int initiate(CommandSourceStack source, String schoolName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerPsiData data = PsiHelper.get(player);
        MagicSchool school = resolveSchool(source, schoolName);
        if (school == null) {
            return 0;
        }
        if (data.race().isEmpty()) {
            source.sendFailure(Component.translatable("message.effecoria.race_required"));
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

    private static final double NEARBY_MOB_RADIUS = 24.0;

    /**
     * Op test: initiate a random nearby mob with a random (or chosen) school so they cast in combat.
     */
    private static int initiateMob(CommandSourceStack source, @Nullable String schoolName)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LivingEntity found = findNearbyMob(player, true);
        if (!(found instanceof Mob mob)) {
            source.sendFailure(Component.translatable("message.effecoria.cast_command_no_mob"));
            return 0;
        }

        MagicSchool school;
        if (schoolName == null || schoolName.equalsIgnoreCase("random")) {
            school = MobMagicService.randomSchool(mob.getRandom());
        } else {
            school = resolveSchool(source, schoolName);
            if (school == null) {
                return 0;
            }
        }

        MobMagicService.initiate(mob, school);
        // Immediate demo cast at the operator so FX is visible without waiting for AI.
        MobMagicService.castAt(mob, player);
        if (mob.getTarget() == null) {
            mob.setTarget(player);
        }

        MagicSchool applied = MobMagicService.schoolOf(mob);
        source.sendSuccess(
                () -> Component.translatable(
                        "message.effecoria.mob_magic.initiated",
                        mob.getDisplayName(),
                        Component.translatable("school.effecoria." + applied.getSerializedName())),
                true);
        return 1;
    }

    private static int castAtNearby(CommandSourceStack source, ResourceLocation spellId, boolean random)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LivingEntity target = findNearbyMob(player, random);
        if (target == null) {
            source.sendFailure(Component.translatable("message.effecoria.cast_command_no_mob"));
            return 0;
        }
        return cast(source, spellId, target);
    }

    @Nullable
    private static LivingEntity findNearbyMob(ServerPlayer player, boolean random) {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(NEARBY_MOB_RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, Mob::isAlive);
        if (mobs.isEmpty()) {
            return null;
        }
        if (random) {
            // Bias toward nearer mobs: pick among the closest half (min 1).
            mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(player)));
            int pool = Math.max(1, (mobs.size() + 1) / 2);
            return mobs.get(level.random.nextInt(pool));
        }
        return mobs.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(player))).orElse(null);
    }

    private static int cast(CommandSourceStack source, ResourceLocation spellId, Entity targetEntity)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LivingEntity forcedTarget = null;
        if (targetEntity != null) {
            if (!(targetEntity instanceof LivingEntity living) || !living.isAlive()) {
                source.sendFailure(Component.translatable("message.effecoria.cast_command_bad_target"));
                return 0;
            }
            forcedTarget = living;
        }
        CastPipeline.CastResult result = CastPipeline.tryCast(player, spellId, 1f, forcedTarget);
        if (result == CastPipeline.CastResult.SUCCESS) {
            if (forcedTarget != null) {
                LivingEntity shown = forcedTarget;
                source.sendSuccess(
                        () -> Component.translatable(
                                "message.effecoria.cast_command_targeted",
                                Component.translatable("spell.effecoria." + spellId.getPath()),
                                shown.getDisplayName()),
                        true);
            }
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
        if (data.school() == MagicSchool.SEALS) {
            for (ResourceLocation wordId : data.knownSealWords()) {
                source.sendSuccess(() -> Component.literal(wordId.toString()), false);
            }
            return data.knownSealWords().size();
        }
        for (ResourceLocation spellId : data.knownSpells()) {
            SpellRegistry.get(spellId).ifPresent(spell -> source.sendSuccess(
                    () -> Component.literal(spellId + " [" + spell.requiredSchool().getSerializedName() + "]"),
                    false));
        }
        return 1;
    }

    /** Spawns a visible mirage-horror preview for model/animation inspection (op only). */
    private static int spawnHorrorPreview(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MirageHorrorEntity horror = MirageHorrorEntity.spawnPreview(player);
        if (horror == null) {
            source.sendFailure(Component.translatable("message.effecoria.horror_preview_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.effecoria.horror_preview"), true);
        return 1;
    }

    private static int subspaceSpeedGet(CommandSourceStack source) {
        int speed = source.getLevel()
                .getGameRules()
                .getInt(com.effecoria.world.ModGameRules.SUBSPACE_ESSENTIALIZE_SPEED);
        source.sendSuccess(
                () -> Component.translatable("message.effecoria.subspace.speed_get", speed), false);
        return speed;
    }

    private static int subspaceSpeedSet(CommandSourceStack source, int speed) {
        source.getServer()
                .getGameRules()
                .getRule(com.effecoria.world.ModGameRules.SUBSPACE_ESSENTIALIZE_SPEED)
                .set(speed, source.getServer());
        source.sendSuccess(
                () -> Component.translatable("message.effecoria.subspace.speed_set", speed), true);
        return speed;
    }
}
