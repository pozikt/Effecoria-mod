package com.effecoria.network;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.EffecoriaMod;
import com.effecoria.client.ClientSteamCloudEffects;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.effect.elemental.SteamCloudService;
import com.effecoria.effect.spatial.SpatialSenseService;
import com.effecoria.magic.CastPipeline;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

public final class ModNetworking {
    private ModNetworking() {}

    public record InitiateSchoolPayload(String school) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<InitiateSchoolPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "initiate_school"));

        public static final StreamCodec<RegistryFriendlyByteBuf, InitiateSchoolPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, InitiateSchoolPayload::school,
                InitiateSchoolPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(InitiateSchoolPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                if (data.initiated()) {
                    return;
                }
                if (data.race().isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.race_required"));
                    return;
                }
                MagicSchool school = MagicSchool.fromSerializedName(payload.school());
                if (!school.isPlayable()) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.invalid_school"));
                    return;
                }
                if (!SpellProgression.schoolHasLoadedSpells(school)) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.spells_not_loaded"));
                    return;
                }
                PsiHelper.initiate(player, school);
                player.syncData(ModAttachments.PSI.get());
                com.effecoria.core.progression.FirstHourTips.onInitiated(player, school);
                player.sendSystemMessage(Component.translatable(
                        "message.effecoria.initiated",
                        Component.translatable("school.effecoria." + school.getSerializedName())));
            });
        }
    }

    public record SelectRacePayload(String race, boolean force, boolean continueInitiation) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SelectRacePayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "select_race"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SelectRacePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SelectRacePayload::race,
                ByteBufCodecs.BOOL, SelectRacePayload::force,
                ByteBufCodecs.BOOL, SelectRacePayload::continueInitiation,
                SelectRacePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SelectRacePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                var raceOpt = com.effecoria.core.progression.PlayerRace.byId(payload.race());
                if (raceOpt.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.invalid_race"));
                    return;
                }
                boolean force = payload.force() && player.hasPermissions(2);
                boolean ok = com.effecoria.core.progression.RaceService.assign(player, raceOpt.get(), force);
                if (!ok) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.race_already_chosen"));
                    return;
                }
                com.effecoria.core.progression.RaceService.notifyAssigned(player, raceOpt.get());
                PlayerPsiData data = PsiHelper.get(player);
                if (payload.continueInitiation() && !data.initiated() && !data.schoolChoiceDeferred()) {
                    PacketDistributor.sendToPlayer(player, new OpenSchoolSelectPayload(true));
                }
            });
        }
    }

    public record OpenSchoolSelectPayload(boolean mandatory) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenSchoolSelectPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "open_school_select"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSchoolSelectPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, OpenSchoolSelectPayload::mandatory,
                OpenSchoolSelectPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(OpenSchoolSelectPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> com.effecoria.client.ClientGuiHooks.openSchoolSelect(payload.mandatory()));
        }
    }

    /** Harpy space-flap while fall-flying. */
    public record HarpyFlapPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HarpyFlapPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "harpy_flap"));

        public static final StreamCodec<RegistryFriendlyByteBuf, HarpyFlapPayload> STREAM_CODEC =
                StreamCodec.unit(new HarpyFlapPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HarpyFlapPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.core.progression.HarpyFlightService.tryFlap(player);
                }
            });
        }
    }

    /** Varanagi sprint+jump scramble dash on walls/trees. */
    public record VaranagiClimbDashPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VaranagiClimbDashPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "varanagi_climb_dash"));

        public static final StreamCodec<RegistryFriendlyByteBuf, VaranagiClimbDashPayload> STREAM_CODEC =
                StreamCodec.unit(new VaranagiClimbDashPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(VaranagiClimbDashPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.core.progression.VaranagiClimbService.tryDash(player);
                }
            });
        }
    }

    /** Varanagi jump-held sync for vine-like wall climb (server cannot read protected jumping). */
    public record VaranagiClimbJumpPayload(boolean jumpHeld) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VaranagiClimbJumpPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "varanagi_climb_jump"));

        public static final StreamCodec<RegistryFriendlyByteBuf, VaranagiClimbJumpPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL,
                        VaranagiClimbJumpPayload::jumpHeld,
                        VaranagiClimbJumpPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(VaranagiClimbJumpPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.core.progression.VaranagiClimbService.setJumpHeld(player, payload.jumpHeld());
                }
            });
        }
    }

    public record DeferSchoolPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DeferSchoolPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "defer_school"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DeferSchoolPayload> STREAM_CODEC =
                StreamCodec.unit(new DeferSchoolPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(DeferSchoolPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                if (data.initiated()) {
                    return;
                }
                data.setSchoolChoiceDeferred(true);
                player.syncData(ModAttachments.PSI.get());
                player.sendSystemMessage(Component.translatable("message.effecoria.school_deferred"));
            });
        }
    }

    public record MatterLinkPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MatterLinkPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "matter_link"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MatterLinkPayload> STREAM_CODEC =
                StreamCodec.unit(new MatterLinkPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MatterLinkPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.effect.elemental.MatterBondService.tryLink(player);
                }
            });
        }
    }

    public record MatterChannelPayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MatterChannelPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "matter_channel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MatterChannelPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, MatterChannelPayload::active,
                MatterChannelPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MatterChannelPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.effect.elemental.MatterBondService.setChanneling(player, payload.active());
                }
            });
        }
    }

    public record MatterThrowPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MatterThrowPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "matter_throw"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MatterThrowPayload> STREAM_CODEC =
                StreamCodec.unit(new MatterThrowPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MatterThrowPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.effect.elemental.MatterBondService.tryThrow(player);
                }
            });
        }
    }

    public record ArmorAbilityPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ArmorAbilityPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "armor_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ArmorAbilityPayload> STREAM_CODEC =
                StreamCodec.unit(new ArmorAbilityPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ArmorAbilityPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.armor.EssoniteArmorService.activateSelected(player);
                }
            });
        }
    }

    public record ArmorAbilityCyclePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ArmorAbilityCyclePayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "armor_ability_cycle"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ArmorAbilityCyclePayload> STREAM_CODEC =
                StreamCodec.unit(new ArmorAbilityCyclePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ArmorAbilityCyclePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    com.effecoria.armor.EssoniteArmorService.cycleAbility(player);
                }
            });
        }
    }

    public record MatterBondSyncPayload(
            boolean active, int x, int y, int z, String kind, float strength) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MatterBondSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "matter_bond_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MatterBondSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, MatterBondSyncPayload::active,
                ByteBufCodecs.VAR_INT, MatterBondSyncPayload::x,
                ByteBufCodecs.VAR_INT, MatterBondSyncPayload::y,
                ByteBufCodecs.VAR_INT, MatterBondSyncPayload::z,
                ByteBufCodecs.STRING_UTF8, MatterBondSyncPayload::kind,
                ByteBufCodecs.FLOAT, MatterBondSyncPayload::strength,
                MatterBondSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MatterBondSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> com.effecoria.client.ClientMatterBondState.apply(payload));
        }
    }

    public record TelegraphPulsePayload(int x1, int y1, int z1, int x2, int y2, int z2)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TelegraphPulsePayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "telegraph_pulse"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TelegraphPulsePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        TelegraphPulsePayload::x1,
                        ByteBufCodecs.VAR_INT,
                        TelegraphPulsePayload::y1,
                        ByteBufCodecs.VAR_INT,
                        TelegraphPulsePayload::z1,
                        ByteBufCodecs.VAR_INT,
                        TelegraphPulsePayload::x2,
                        ByteBufCodecs.VAR_INT,
                        TelegraphPulsePayload::y2,
                        ByteBufCodecs.VAR_INT,
                        TelegraphPulsePayload::z2,
                        TelegraphPulsePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(TelegraphPulsePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> com.effecoria.client.ClientTelegraphFx.pulse(payload));
        }
    }

    public record SelectSpellPayload(int spellIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SelectSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "select_spell"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SelectSpellPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SelectSpellPayload::spellIndex,
                SelectSpellPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SelectSpellPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                if (!data.initiated() || data.knownSpells().isEmpty()) {
                    return;
                }
                data.setSelectedSpellIndex(payload.spellIndex());
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
                ResourceLocation selected = data.selectedSpell();
                if (selected != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.effecoria.spell_selected",
                                    Component.translatable("spell.effecoria." + selected.getPath())),
                            true);
                }
            });
        }
    }

    public record CastSpellPayload(int spellIndex, float charge) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "cast_spell"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CastSpellPayload::spellIndex,
                ByteBufCodecs.FLOAT, CastSpellPayload::charge,
                CastSpellPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(CastSpellPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                if (payload.spellIndex() >= 0) {
                    data.setSelectedSpellIndex(payload.spellIndex());
                    PsiHelper.set(player, data);
                    player.syncData(ModAttachments.PSI.get());
                }
                float charge = Math.clamp(payload.charge(), 0f, 1f);
                CastPipeline.tryCastSelected(player, charge);
            });
        }
    }

    public record CycleSpellPayload(int delta) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CycleSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "cycle_spell"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CycleSpellPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CycleSpellPayload::delta,
                CycleSpellPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(CycleSpellPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                data.cycleSpell(payload.delta());
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
            });
        }
    }

    /** Server → client: active steam fog volumes for density fog + local particles. */
    public record SteamCloudsPayload(List<SteamCloudService.CloudSnapshot> clouds) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SteamCloudsPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "steam_clouds"));

        private static final StreamCodec<RegistryFriendlyByteBuf, SteamCloudService.CloudSnapshot> SNAPSHOT_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.DOUBLE, SteamCloudService.CloudSnapshot::x,
                        ByteBufCodecs.DOUBLE, SteamCloudService.CloudSnapshot::y,
                        ByteBufCodecs.DOUBLE, SteamCloudService.CloudSnapshot::z,
                        ByteBufCodecs.FLOAT, SteamCloudService.CloudSnapshot::radius,
                        ByteBufCodecs.VAR_LONG, SteamCloudService.CloudSnapshot::expireAt,
                        ByteBufCodecs.VAR_INT, SteamCloudService.CloudSnapshot::modeId,
                        SteamCloudService.CloudSnapshot::new);

        public static final StreamCodec<RegistryFriendlyByteBuf, SteamCloudsPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.collection(ArrayList::new, SNAPSHOT_CODEC),
                        SteamCloudsPayload::clouds,
                        SteamCloudsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SteamCloudsPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> ClientSteamCloudEffects.setClouds(payload.clouds()));
        }
    }

    /** Server → caster: blurred structure coordinates for Locus Echo HUD. */
    public record BlurredLocusPayload(int x, int y, int z, int displayTicks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlurredLocusPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "blurred_locus"));

        public static final StreamCodec<RegistryFriendlyByteBuf, BlurredLocusPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        BlurredLocusPayload::x,
                        ByteBufCodecs.VAR_INT,
                        BlurredLocusPayload::y,
                        ByteBufCodecs.VAR_INT,
                        BlurredLocusPayload::z,
                        ByteBufCodecs.VAR_INT,
                        BlurredLocusPayload::displayTicks,
                        BlurredLocusPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BlurredLocusPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.hud.BlurredLocusHud.show(
                            payload.x(), payload.y(), payload.z(), payload.displayTicks()));
        }
    }

    /** Server → victim: enter client-only illusory space (no world edits). */
    public record MirageStartPayload(int durationTicks, float maxHp, float intensity) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MirageStartPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "mirage_start"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MirageStartPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        MirageStartPayload::durationTicks,
                        ByteBufCodecs.FLOAT,
                        MirageStartPayload::maxHp,
                        ByteBufCodecs.FLOAT,
                        MirageStartPayload::intensity,
                        MirageStartPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MirageStartPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.MirageClient.onStart(
                            payload.durationTicks(), payload.maxHp(), payload.intensity()));
        }
    }

    /** Server → victim: illusory hurt pulse (sound/flash/HUD only). */
    public record MirageHurtPayload(float amount, float remainingHp, float maxHp) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MirageHurtPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "mirage_hurt"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MirageHurtPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.FLOAT,
                        MirageHurtPayload::amount,
                        ByteBufCodecs.FLOAT,
                        MirageHurtPayload::remainingHp,
                        ByteBufCodecs.FLOAT,
                        MirageHurtPayload::maxHp,
                        MirageHurtPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MirageHurtPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.MirageClient.onHurt(
                            payload.amount(), payload.remainingHp(), payload.maxHp()));
        }
    }

    /** Server → victim: leave illusory space. */
    public record MirageEndPayload(boolean collapsed) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MirageEndPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "mirage_end"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MirageEndPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, MirageEndPayload::collapsed, MirageEndPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MirageEndPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> com.effecoria.client.MirageClient.onEnd(payload.collapsed()));
        }
    }

    /** Server → nearby clients: world-anchored spatial singularity pulse. */
    public record SingularityFxPayload(double x, double y, double z, float intensity, int durationTicks)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SingularityFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "singularity_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SingularityFxPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.DOUBLE,
                        SingularityFxPayload::x,
                        ByteBufCodecs.DOUBLE,
                        SingularityFxPayload::y,
                        ByteBufCodecs.DOUBLE,
                        SingularityFxPayload::z,
                        ByteBufCodecs.FLOAT,
                        SingularityFxPayload::intensity,
                        ByteBufCodecs.VAR_INT,
                        SingularityFxPayload::durationTicks,
                        SingularityFxPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SingularityFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.SpatialSingularityClient.trigger(
                            payload.x(),
                            payload.y(),
                            payload.z(),
                            payload.intensity(),
                            payload.durationTicks()));
        }
    }

    /** Server → nearby clients: elemental quasar heat-haze / layered eye FX. */
    public record QuasarFxPayload(
            double x, double y, double z, float intensity, float radius, int durationTicks, boolean refresh)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<QuasarFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "quasar_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, QuasarFxPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.x());
                            buf.writeDouble(p.y());
                            buf.writeDouble(p.z());
                            buf.writeFloat(p.intensity());
                            buf.writeFloat(p.radius());
                            buf.writeVarInt(p.durationTicks());
                            buf.writeBoolean(p.refresh());
                        },
                        buf -> new QuasarFxPayload(
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readVarInt(),
                                buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(QuasarFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> {
                        if (payload.refresh()) {
                            com.effecoria.client.QuasarClient.pulse(
                                    payload.x(),
                                    payload.y(),
                                    payload.z(),
                                    payload.intensity(),
                                    payload.radius(),
                                    payload.durationTicks());
                        } else {
                            com.effecoria.client.QuasarClient.trigger(
                                    payload.x(),
                                    payload.y(),
                                    payload.z(),
                                    payload.intensity(),
                                    payload.radius(),
                                    payload.durationTicks());
                        }
                    });
        }
    }

    /** Server → nearby clients: world-anchored dimensional cut (line or around). */
    public record SpatialCutFxPayload(
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            float intensity,
            int slashCount,
            int mode)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpatialCutFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "spatial_cut_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpatialCutFxPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.x0());
                            buf.writeDouble(p.y0());
                            buf.writeDouble(p.z0());
                            buf.writeDouble(p.x1());
                            buf.writeDouble(p.y1());
                            buf.writeDouble(p.z1());
                            buf.writeFloat(p.intensity());
                            buf.writeVarInt(p.slashCount());
                            buf.writeVarInt(p.mode());
                        },
                        buf -> new SpatialCutFxPayload(
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readFloat(),
                                buf.readVarInt(),
                                buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpatialCutFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.SpatialCutClient.trigger(
                            payload.x0(),
                            payload.y0(),
                            payload.z0(),
                            payload.x1(),
                            payload.y1(),
                            payload.z1(),
                            payload.intensity(),
                            payload.slashCount(),
                            payload.mode()));
        }
    }

    /** Server → nearby clients: slanted lightning arc from hand to strike. */
    public record LightningArcFxPayload(
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1,
            float intensity,
            int durationTicks)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<LightningArcFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "lightning_arc_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, LightningArcFxPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.x0());
                            buf.writeDouble(p.y0());
                            buf.writeDouble(p.z0());
                            buf.writeDouble(p.x1());
                            buf.writeDouble(p.y1());
                            buf.writeDouble(p.z1());
                            buf.writeFloat(p.intensity());
                            buf.writeVarInt(p.durationTicks());
                        },
                        buf -> new LightningArcFxPayload(
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readFloat(),
                                buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(LightningArcFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.LightningArcClient.trigger(
                            payload.x0(),
                            payload.y0(),
                            payload.z0(),
                            payload.x1(),
                            payload.y1(),
                            payload.z1(),
                            payload.intensity(),
                            payload.durationTicks()));
        }
    }

    /** Server → nearby clients: world-anchored space ripple (blink / jump). */
    public record SpatialRippleFxPayload(double x, double y, double z, float intensity, int durationTicks)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpatialRippleFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "spatial_ripple_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpatialRippleFxPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.DOUBLE,
                        SpatialRippleFxPayload::x,
                        ByteBufCodecs.DOUBLE,
                        SpatialRippleFxPayload::y,
                        ByteBufCodecs.DOUBLE,
                        SpatialRippleFxPayload::z,
                        ByteBufCodecs.FLOAT,
                        SpatialRippleFxPayload::intensity,
                        ByteBufCodecs.VAR_INT,
                        SpatialRippleFxPayload::durationTicks,
                        SpatialRippleFxPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpatialRippleFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.SpatialRippleClient.trigger(
                            payload.x(),
                            payload.y(),
                            payload.z(),
                            payload.intensity(),
                            payload.durationTicks()));
        }
    }

    /** Server → caster: spatial sonar hits (relative cavity/trap marks with fade strength). */
    public record SpatialSensePayload(
            int originX, int originY, int originZ, int durationTicks, List<SpatialSenseService.Hit> hits)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpatialSensePayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "spatial_sense_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpatialSensePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeVarInt(p.originX());
                            buf.writeVarInt(p.originY());
                            buf.writeVarInt(p.originZ());
                            buf.writeVarInt(p.durationTicks());
                            buf.writeVarInt(p.hits().size());
                            for (SpatialSenseService.Hit hit : p.hits()) {
                                buf.writeByte(hit.dx());
                                buf.writeByte(hit.dy());
                                buf.writeByte(hit.dz());
                                buf.writeByte(hit.strength());
                                buf.writeByte(hit.kind());
                            }
                        },
                        buf -> {
                            int ox = buf.readVarInt();
                            int oy = buf.readVarInt();
                            int oz = buf.readVarInt();
                            int duration = buf.readVarInt();
                            int count = Math.min(900, Math.max(0, buf.readVarInt()));
                            List<SpatialSenseService.Hit> hits = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                hits.add(new SpatialSenseService.Hit(
                                        buf.readByte(),
                                        buf.readByte(),
                                        buf.readByte(),
                                        buf.readByte(),
                                        buf.readByte()));
                            }
                            return new SpatialSensePayload(ox, oy, oz, duration, hits);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpatialSensePayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.ClientSpatialSense.activate(
                            payload.originX(),
                            payload.originY(),
                            payload.originZ(),
                            payload.durationTicks(),
                            payload.hits()));
        }
    }

    /** Server → nearby clients: area spacetime curvature (gravity well bowl, not a black hole). */
    public record SpatialWarpFxPayload(
            double x, double y, double z, float intensity, float radius, int durationTicks, boolean refresh)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpatialWarpFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "spatial_warp_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpatialWarpFxPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.x());
                            buf.writeDouble(p.y());
                            buf.writeDouble(p.z());
                            buf.writeFloat(p.intensity());
                            buf.writeFloat(p.radius());
                            buf.writeVarInt(p.durationTicks());
                            buf.writeBoolean(p.refresh());
                        },
                        buf -> new SpatialWarpFxPayload(
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readVarInt(),
                                buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpatialWarpFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> {
                        if (payload.refresh()) {
                            com.effecoria.client.SpatialWarpClient.pulse(
                                    payload.x(),
                                    payload.y(),
                                    payload.z(),
                                    payload.intensity(),
                                    payload.radius(),
                                    payload.durationTicks());
                        } else {
                            com.effecoria.client.SpatialWarpClient.trigger(
                                    payload.x(),
                                    payload.y(),
                                    payload.z(),
                                    payload.intensity(),
                                    payload.radius(),
                                    payload.durationTicks());
                        }
                    });
        }
    }

    /** Server → nearby clients: expanding gravitational wave fronts. */
    public record SpatialWaveFxPayload(
            double x, double y, double z, float intensity, float radius, int durationTicks)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpatialWaveFxPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "spatial_wave_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SpatialWaveFxPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.x());
                            buf.writeDouble(p.y());
                            buf.writeDouble(p.z());
                            buf.writeFloat(p.intensity());
                            buf.writeFloat(p.radius());
                            buf.writeVarInt(p.durationTicks());
                        },
                        buf -> new SpatialWaveFxPayload(
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpatialWaveFxPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.SpatialWaveClient.trigger(
                            payload.x(),
                            payload.y(),
                            payload.z(),
                            payload.intensity(),
                            payload.radius(),
                            payload.durationTicks()));
        }
    }

    /** Client reports a successful timing hit; server validates fatigue and grants rewards. */
    public record BreathTrainHitPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BreathTrainHitPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "breath_train_hit"));

        public static final StreamCodec<RegistryFriendlyByteBuf, BreathTrainHitPayload> STREAM_CODEC =
                StreamCodec.unit(new BreathTrainHitPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BreathTrainHitPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                if (!data.initiated()) {
                    return;
                }
                if (data.isBreathTrainFatigued()) {
                    int sec = (int) Math.ceil(data.breathTrainFatigueRemainingMs() / 1000.0);
                    player.displayClientMessage(
                            Component.translatable("message.effecoria.breath_train_fatigued", sec), true);
                    return;
                }
                long minGap = com.effecoria.config.BalanceConfig.BREATHING_TRAIN_MIN_INTERVAL_MS.get();
                if (!data.tryAcceptBreathTrainHit(minGap)) {
                    return;
                }
                float before = data.breathingMastery();
                data.recordSuccessfulBreathTrain();
                com.effecoria.core.progression.ProgressionService.onBreathTrainHit(player, data);
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
                float bonusPct = data.breathTrainRegenBonus() * 100f;
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.breath_train_success",
                                String.format("%.1f", bonusPct),
                                com.effecoria.core.progression.BreathingService.formatTotalPercent(data.breathingMastery())),
                        true);
                com.effecoria.core.progression.BreathingService.notifyMilestones(
                        player, before, data.breathingMastery());
            });
        }
    }

    /** Client reports a missed timing click; after the miss limit, fatigue applies. */
    public record BreathTrainMissPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BreathTrainMissPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "breath_train_miss"));

        public static final StreamCodec<RegistryFriendlyByteBuf, BreathTrainMissPayload> STREAM_CODEC =
                StreamCodec.unit(new BreathTrainMissPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BreathTrainMissPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                if (!data.initiated() || data.isBreathTrainFatigued()) {
                    return;
                }
                long minGap = com.effecoria.config.BalanceConfig.BREATHING_TRAIN_MIN_INTERVAL_MS.get();
                if (!data.tryAcceptBreathTrainHit(minGap)) {
                    return;
                }
                boolean fatigued = data.recordBreathTrainMiss();
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
                if (fatigued) {
                    int sec = (int) Math.ceil(data.breathTrainFatigueRemainingMs() / 1000.0);
                    player.displayClientMessage(
                            Component.translatable("message.effecoria.breath_train_miss_fatigue", sec), true);
                }
            });
        }
    }

    /** Client → server: apply a seal word program to a looked-at block. */
    public record ApplySealProgramPayload(BlockPos pos, List<ResourceLocation> tokens)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ApplySealProgramPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "apply_seal_program"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ApplySealProgramPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeBlockPos(p.pos());
                            buf.writeVarInt(p.tokens().size());
                            for (ResourceLocation id : p.tokens()) {
                                ResourceLocation.STREAM_CODEC.encode(buf, id);
                            }
                        },
                        buf -> {
                            BlockPos pos = buf.readBlockPos();
                            int n = buf.readVarInt();
                            List<ResourceLocation> tokens = new ArrayList<>(n);
                            for (int i = 0; i < n; i++) {
                                tokens.add(ResourceLocation.STREAM_CODEC.decode(buf));
                            }
                            return new ApplySealProgramPayload(pos, tokens);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ApplySealProgramPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (payload.tokens().size() > 16) {
                    return;
                }
                if (player.blockPosition().distSqr(payload.pos()) > 16 * 16) {
                    return;
                }
                var status = com.effecoria.core.seal.SealProgramService.apply(
                        player, payload.pos(), payload.tokens());
                if (status != com.effecoria.core.seal.SealProgramService.ApplyStatus.OK) {
                    player.displayClientMessage(
                            Component.translatable("message.effecoria.seal.apply_failed." + status.name().toLowerCase()),
                            true);
                } else {
                    com.effecoria.core.progression.FirstHourTips.tryShow(
                            player, com.effecoria.core.progression.FirstHourTips.Tip.SEALS);
                }
            });
        }
    }

    /** Client → server: clear seals on a block. */
    public record ClearSealProgramPayload(BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClearSealProgramPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "clear_seal_program"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClearSealProgramPayload> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, ClearSealProgramPayload::pos, ClearSealProgramPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ClearSealProgramPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (player.blockPosition().distSqr(payload.pos()) > 16 * 16) {
                    return;
                }
                com.effecoria.core.seal.SealProgramService.clear(player, payload.pos());
            });
        }
    }

    /** Client → server: save current seal expression into a reusable slot. */
    public record SaveSealExpressionPayload(int slot, List<ResourceLocation> tokens) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SaveSealExpressionPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "save_seal_expression"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SaveSealExpressionPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeVarInt(p.slot());
                            buf.writeVarInt(p.tokens().size());
                            for (ResourceLocation id : p.tokens()) {
                                ResourceLocation.STREAM_CODEC.encode(buf, id);
                            }
                        },
                        buf -> {
                            int slot = buf.readVarInt();
                            int n = buf.readVarInt();
                            List<ResourceLocation> tokens = new ArrayList<>(n);
                            for (int i = 0; i < n; i++) {
                                tokens.add(ResourceLocation.STREAM_CODEC.decode(buf));
                            }
                            return new SaveSealExpressionPayload(slot, tokens);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SaveSealExpressionPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (payload.slot() < 0 || payload.slot() > 7 || payload.tokens().size() > 16) {
                    return;
                }
                PlayerPsiData data = PsiHelper.get(player);
                data.saveSealExpression(payload.slot(), payload.tokens());
                PsiHelper.set(player, data);
                player.syncData(ModAttachments.PSI.get());
                player.displayClientMessage(
                        Component.translatable("message.effecoria.seal.expression_saved", payload.slot() + 1),
                        true);
            });
        }
    }

    /** Server → client: full spell datapack catalog (remote clients have no server reload listener). */
    public record SpellCatalogPayload(List<com.effecoria.core.magic.SpellDefinition> spells)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpellCatalogPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "spell_catalog"));

        private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

        public static final StreamCodec<RegistryFriendlyByteBuf, SpellCatalogPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.spells().size());
                            for (com.effecoria.core.magic.SpellDefinition spell : payload.spells()) {
                                writeSpell(buf, spell);
                            }
                        },
                        buf -> {
                            int count = buf.readVarInt();
                            List<com.effecoria.core.magic.SpellDefinition> list = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                list.add(readSpell(buf));
                            }
                            return new SpellCatalogPayload(list);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SpellCatalogPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                java.util.HashMap<ResourceLocation, com.effecoria.core.magic.SpellDefinition> map =
                        new java.util.HashMap<>();
                for (com.effecoria.core.magic.SpellDefinition spell : payload.spells()) {
                    map.put(spell.id(), spell);
                }
                com.effecoria.magic.SpellRegistry.replaceAll(map);
            });
        }

        private static void writeSpell(RegistryFriendlyByteBuf buf, com.effecoria.core.magic.SpellDefinition spell) {
            ResourceLocation.STREAM_CODEC.encode(buf, spell.id());
            buf.writeUtf(spell.requiredSchool().getSerializedName(), 64);
            buf.writeFloat(spell.frequencyHz());
            buf.writeFloat(spell.baseCost());
            buf.writeFloat(spell.powerMultiplier());
            buf.writeFloat(spell.sideEntropyRatio());
            buf.writeFloat(spell.minPhi());
            buf.writeFloat(spell.minMastery());
            buf.writeFloat(spell.minPower());
            buf.writeVarInt(spell.unlockEssenceCost());
            buf.writeUtf(spell.radialCategory().getSerializedName(), 64);
            buf.writeVarInt(spell.effects().size());
            for (com.effecoria.core.magic.SpellEffectEntry effect : spell.effects()) {
                ResourceLocation.STREAM_CODEC.encode(buf, effect.type());
                buf.writeUtf(GSON.toJson(effect.params()), 32767);
            }
        }

        private static com.effecoria.core.magic.SpellDefinition readSpell(RegistryFriendlyByteBuf buf) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
            MagicSchool school = MagicSchool.fromSerializedName(buf.readUtf(64));
            float frequency = buf.readFloat();
            float baseCost = buf.readFloat();
            float powerMultiplier = buf.readFloat();
            float sideEntropy = buf.readFloat();
            float minPhi = buf.readFloat();
            float minMastery = buf.readFloat();
            float minPower = buf.readFloat();
            int unlockEssence = buf.readVarInt();
            com.effecoria.core.magic.RadialCategory radial =
                    com.effecoria.core.magic.RadialCategory.fromSerializedName(buf.readUtf(64));
            int effectCount = buf.readVarInt();
            List<com.effecoria.core.magic.SpellEffectEntry> effects = new ArrayList<>(effectCount);
            for (int i = 0; i < effectCount; i++) {
                ResourceLocation type = ResourceLocation.STREAM_CODEC.decode(buf);
                com.google.gson.JsonObject params =
                        GSON.fromJson(buf.readUtf(32767), com.google.gson.JsonObject.class);
                if (params == null) {
                    params = new com.google.gson.JsonObject();
                }
                effects.add(new com.effecoria.core.magic.SpellEffectEntry(type, params));
            }
            return new com.effecoria.core.magic.SpellDefinition(
                    id,
                    school,
                    frequency,
                    baseCost,
                    powerMultiplier,
                    sideEntropy,
                    minPhi,
                    minMastery,
                    minPower,
                    unlockEssence,
                    radial,
                    effects);
        }
    }

    /** Server → client: seal word lexicon for the programming UI. */
    public record SealWordCatalogPayload(List<com.effecoria.core.seal.SealWordDefinition> words)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SealWordCatalogPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "seal_word_catalog"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SealWordCatalogPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.words().size());
                            for (com.effecoria.core.seal.SealWordDefinition word : payload.words()) {
                                ResourceLocation.STREAM_CODEC.encode(buf, word.id());
                                buf.writeUtf(word.kind().serializedName(), 32);
                                buf.writeUtf(word.effect() == null ? "" : word.effect(), 128);
                                buf.writeFloat(word.numberValue());
                                buf.writeBoolean(word.soundEvent() != null);
                                if (word.soundEvent() != null) {
                                    ResourceLocation.STREAM_CODEC.encode(buf, word.soundEvent());
                                }
                                buf.writeFloat(word.psiCost());
                                buf.writeFloat(word.minMastery());
                                buf.writeBoolean(word.starter());
                            }
                        },
                        buf -> {
                            int count = buf.readVarInt();
                            List<com.effecoria.core.seal.SealWordDefinition> list = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                                com.effecoria.core.seal.SealWordKind kind =
                                        com.effecoria.core.seal.SealWordKind.fromSerialized(buf.readUtf(32));
                                String effect = buf.readUtf(128);
                                float numberValue = buf.readFloat();
                                ResourceLocation sound = buf.readBoolean()
                                        ? ResourceLocation.STREAM_CODEC.decode(buf)
                                        : null;
                                float psiCost = buf.readFloat();
                                float minMastery = buf.readFloat();
                                boolean starter = buf.readBoolean();
                                list.add(new com.effecoria.core.seal.SealWordDefinition(
                                        id, kind, effect, numberValue, sound, psiCost, minMastery, starter));
                            }
                            return new SealWordCatalogPayload(list);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(SealWordCatalogPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                java.util.HashMap<ResourceLocation, com.effecoria.core.seal.SealWordDefinition> map =
                        new java.util.HashMap<>();
                for (com.effecoria.core.seal.SealWordDefinition word : payload.words()) {
                    map.put(word.id(), word);
                }
                com.effecoria.core.seal.SealWordRegistry.replaceAll(map);
            });
        }
    }

    /** Server → client: Lex Loci word lexicon for the console editor. */
    public record LociWordCatalogPayload(List<com.effecoria.core.loci.LociWordDefinition> words)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<LociWordCatalogPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "loci_word_catalog"));

        public static final StreamCodec<RegistryFriendlyByteBuf, LociWordCatalogPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.words().size());
                            for (com.effecoria.core.loci.LociWordDefinition word : payload.words()) {
                                ResourceLocation.STREAM_CODEC.encode(buf, word.id());
                                buf.writeUtf(word.kind().serializedName(), 32);
                                buf.writeUtf(word.effect() == null ? "" : word.effect(), 128);
                            }
                        },
                        buf -> {
                            int count = buf.readVarInt();
                            List<com.effecoria.core.loci.LociWordDefinition> list = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
                                com.effecoria.core.seal.SealWordKind kind =
                                        com.effecoria.core.seal.SealWordKind.fromSerialized(buf.readUtf(32));
                                String effect = buf.readUtf(128);
                                list.add(new com.effecoria.core.loci.LociWordDefinition(id, kind, effect));
                            }
                            return new LociWordCatalogPayload(list);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(LociWordCatalogPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                java.util.HashMap<ResourceLocation, com.effecoria.core.loci.LociWordDefinition> map =
                        new java.util.HashMap<>();
                for (com.effecoria.core.loci.LociWordDefinition word : payload.words()) {
                    map.put(word.id(), word);
                }
                com.effecoria.core.loci.LociWordRegistry.replaceAll(map);
            });
        }
    }

    /** Client notifies server that the spell hub was opened — first-hour tip. */
    public record HubOpenedPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HubOpenedPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "hub_opened"));

        public static final StreamCodec<RegistryFriendlyByteBuf, HubOpenedPayload> STREAM_CODEC =
                StreamCodec.unit(new HubOpenedPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(HubOpenedPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                // One tip per open — avoid action-bar overwrite of OPEN_HUB by BREATHING.
                PlayerPsiData data = PsiHelper.get(player);
                if (!com.effecoria.core.progression.FirstHourTips.hasSeen(
                        data, com.effecoria.core.progression.FirstHourTips.Tip.OPEN_HUB)) {
                    com.effecoria.core.progression.FirstHourTips.tryShow(
                            player, com.effecoria.core.progression.FirstHourTips.Tip.OPEN_HUB);
                } else {
                    com.effecoria.core.progression.FirstHourTips.tryShow(
                            player, com.effecoria.core.progression.FirstHourTips.Tip.BREATHING);
                }
            });
        }
    }

    /** Client marks a Magic Primer chapter as read — NEW badges / foil stay in sync. */
    public record MarkPrimerChapterSeenPayload(int chapterBit) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MarkPrimerChapterSeenPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "primer_chapter_seen"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MarkPrimerChapterSeenPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        MarkPrimerChapterSeenPayload::chapterBit,
                        MarkPrimerChapterSeenPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MarkPrimerChapterSeenPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                com.effecoria.core.progression.PrimerChapters.Chapter chapter =
                        com.effecoria.core.progression.PrimerChapters.byBitIndex(payload.chapterBit());
                if (chapter == null) {
                    return;
                }
                var data = com.effecoria.core.psi.PsiHelper.get(player);
                int next = data.primerSeenMask() | chapter.mask();
                if (next == data.primerSeenMask()) {
                    return;
                }
                data.setPrimerSeenMask(next);
                com.effecoria.core.psi.PsiHelper.set(player, data);
                player.syncData(com.effecoria.core.psi.ModAttachments.PSI.get());
            });
        }
    }

    /** Server opens Organic gene editor for a living host. */
    public record OpenGeneEditorPayload(
            int entityId, String targetName, List<String> current, List<String> unlocked, int maxSlots)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenGeneEditorPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "open_gene_editor"));

        private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRING_LIST =
                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8);

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenGeneEditorPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        OpenGeneEditorPayload::entityId,
                        ByteBufCodecs.STRING_UTF8,
                        OpenGeneEditorPayload::targetName,
                        STRING_LIST,
                        OpenGeneEditorPayload::current,
                        STRING_LIST,
                        OpenGeneEditorPayload::unlocked,
                        ByteBufCodecs.VAR_INT,
                        OpenGeneEditorPayload::maxSlots,
                        OpenGeneEditorPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(OpenGeneEditorPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> com.effecoria.client.ClientGuiHooks.openGeneEditor(
                    payload.entityId(),
                    payload.targetName(),
                    payload.current(),
                    payload.unlocked(),
                    payload.maxSlots()));
        }
    }

    /** Client applies selected gene grafts to a host. */
    public record ApplyGeneModsPayload(int entityId, List<String> mods) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ApplyGeneModsPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "apply_gene_mods"));

        private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRING_LIST =
                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8);

        public static final StreamCodec<RegistryFriendlyByteBuf, ApplyGeneModsPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        ApplyGeneModsPayload::entityId,
                        STRING_LIST,
                        ApplyGeneModsPayload::mods,
                        ApplyGeneModsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ApplyGeneModsPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                var level = player.serverLevel();
                var entity = level.getEntity(payload.entityId());
                if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
                    player.displayClientMessage(
                            Component.translatable("message.effecoria.gene.no_host"), true);
                    return;
                }
                boolean ok = com.effecoria.effect.organic.gene.GeneEngineeringService.applyFromEngineer(
                        player, living, payload.mods(), 8.0);
                player.displayClientMessage(
                        Component.translatable(
                                ok ? "message.effecoria.gene.applied" : "message.effecoria.gene.failed"),
                        true);
            });
        }
    }

    /** Client clears gene grafts from a host. */
    public record ClearGeneModsPayload(int entityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClearGeneModsPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "clear_gene_mods"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ClearGeneModsPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ClearGeneModsPayload::entityId, ClearGeneModsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ClearGeneModsPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                var entity = player.serverLevel().getEntity(payload.entityId());
                if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
                    return;
                }
                com.effecoria.effect.organic.gene.GeneEngineeringService.clearFromEngineer(player, living, 8.0);
                player.displayClientMessage(Component.translatable("message.effecoria.gene.cleared"), true);
            });
        }
    }

    /** Sync dominant Φ/Ω weather to the client for particles / HUD. */
    public record PhiWeatherSyncPayload(String kind, float intensity, long untilTick)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PhiWeatherSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "phi_weather_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PhiWeatherSyncPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        PhiWeatherSyncPayload::kind,
                        ByteBufCodecs.FLOAT,
                        PhiWeatherSyncPayload::intensity,
                        ByteBufCodecs.VAR_LONG,
                        PhiWeatherSyncPayload::untilTick,
                        PhiWeatherSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(PhiWeatherSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                var kind = com.effecoria.world.weather.PhiWeatherKind.fromId(payload.kind());
                com.effecoria.world.weather.PhiWeatherService.setClientSnapshot(
                        new com.effecoria.world.weather.PhiWeatherService.Snapshot(
                                kind, payload.intensity(), payload.untilTick()));
            });
        }
    }

    public record PhiBeaconRenamePayload(BlockPos pos, String name) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PhiBeaconRenamePayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "phi_beacon_rename"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PhiBeaconRenamePayload> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        PhiBeaconRenamePayload::pos,
                        ByteBufCodecs.STRING_UTF8,
                        PhiBeaconRenamePayload::name,
                        PhiBeaconRenamePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(PhiBeaconRenamePayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (!(player.level().getBlockEntity(payload.pos())
                        instanceof com.effecoria.block.PhiBeaconBlockEntity beacon)) {
                    return;
                }
                if (!beacon.setBeaconName(payload.name())) {
                    player.displayClientMessage(Component.translatable("message.effecoria.phi_beacon_name_taken"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.effecoria.phi_beacon_named"), true);
                }
            });
        }
    }

    public record PortalModulatorConfigPayload(
            BlockPos pos, int mode, int x, int y, int z, String beacon) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PortalModulatorConfigPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "portal_modulator_config"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PortalModulatorConfigPayload> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        PortalModulatorConfigPayload::pos,
                        ByteBufCodecs.VAR_INT,
                        PortalModulatorConfigPayload::mode,
                        ByteBufCodecs.VAR_INT,
                        PortalModulatorConfigPayload::x,
                        ByteBufCodecs.VAR_INT,
                        PortalModulatorConfigPayload::y,
                        ByteBufCodecs.VAR_INT,
                        PortalModulatorConfigPayload::z,
                        ByteBufCodecs.STRING_UTF8,
                        PortalModulatorConfigPayload::beacon,
                        PortalModulatorConfigPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(PortalModulatorConfigPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (!(player.level().getBlockEntity(payload.pos())
                        instanceof com.effecoria.block.PortalModulatorBlockEntity mod)) {
                    return;
                }
                if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                        player, com.effecoria.core.technomagic.TechnomagicEra.V)) {
                    return;
                }
                mod.setMode(payload.mode());
                mod.setCoords(payload.x(), payload.y(), payload.z());
                mod.setBeaconName(payload.beacon());
            });
        }
    }

    public record EssenceGlueSyncPayload(
            java.util.List<BlockPos> glued, java.util.List<BlockPos> session, java.util.List<BlockPos> pending)
            implements CustomPacketPayload {
        public static final Type<EssenceGlueSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "essence_glue_sync"));

        public static EssenceGlueSyncPayload of(
                java.util.Set<BlockPos> glued, java.util.Set<BlockPos> session, @Nullable BlockPos pending) {
            java.util.List<BlockPos> pendingList = new java.util.ArrayList<>(1);
            if (pending != null) {
                pendingList.add(pending.immutable());
            }
            return new EssenceGlueSyncPayload(
                    new java.util.ArrayList<>(glued), new java.util.ArrayList<>(session), pendingList);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, EssenceGlueSyncPayload> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
                        EssenceGlueSyncPayload::glued,
                        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
                        EssenceGlueSyncPayload::session,
                        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
                        EssenceGlueSyncPayload::pending,
                        EssenceGlueSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(EssenceGlueSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                BlockPos pending =
                        payload.pending().isEmpty() ? null : payload.pending().getFirst();
                com.effecoria.client.glue.EssenceGlueClient.apply(
                        new java.util.HashSet<>(payload.glued()),
                        new java.util.HashSet<>(payload.session()),
                        pending);
            });
        }
    }

    /** Combat Φ-dome outline sync (ultramarine AABB around a Mage Tower). */
    public record TowerDomeSyncPayload(
            BlockPos anchor, boolean combat, double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
            implements CustomPacketPayload {
        public static final Type<TowerDomeSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "tower_dome_sync"));

        public static TowerDomeSyncPayload active(BlockPos anchor, AABB box) {
            return new TowerDomeSyncPayload(
                    anchor.immutable(), true, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        }

        public static TowerDomeSyncPayload clear(BlockPos anchor) {
            return new TowerDomeSyncPayload(anchor.immutable(), false, 0, 0, 0, 0, 0, 0);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, TowerDomeSyncPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            BlockPos.STREAM_CODEC.encode(buf, payload.anchor());
                            ByteBufCodecs.BOOL.encode(buf, payload.combat());
                            buf.writeDouble(payload.minX());
                            buf.writeDouble(payload.minY());
                            buf.writeDouble(payload.minZ());
                            buf.writeDouble(payload.maxX());
                            buf.writeDouble(payload.maxY());
                            buf.writeDouble(payload.maxZ());
                        },
                        buf -> new TowerDomeSyncPayload(
                                BlockPos.STREAM_CODEC.decode(buf),
                                ByteBufCodecs.BOOL.decode(buf),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble(),
                                buf.readDouble()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(TowerDomeSyncPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!payload.combat()) {
                    com.effecoria.client.tower.ClientTowerDomeOutline.apply(payload.anchor(), false, null);
                    return;
                }
                AABB box = new AABB(
                        payload.minX(),
                        payload.minY(),
                        payload.minZ(),
                        payload.maxX(),
                        payload.maxY(),
                        payload.maxZ());
                com.effecoria.client.tower.ClientTowerDomeOutline.apply(payload.anchor(), true, box);
            });
        }
    }

    /** Client → server: request a Φ-sonar sweep (mode) from console or cartography table. */
    public record PhiSonarRequestPayload(BlockPos accessPos, int modeId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PhiSonarRequestPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "phi_sonar_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PhiSonarRequestPayload> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        PhiSonarRequestPayload::accessPos,
                        ByteBufCodecs.VAR_INT,
                        PhiSonarRequestPayload::modeId,
                        PhiSonarRequestPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(PhiSonarRequestPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                        player, com.effecoria.core.technomagic.TechnomagicEra.VI)) {
                    return;
                }
                com.effecoria.core.tower.PhiSonarService.requestScan(
                        player, payload.accessPos(), payload.modeId());
            });
        }
    }

    /** Server → client: heightmap + terrain class + blips for Map / cartography GUIs. */
    public record PhiSonarMapPayload(
            int originX,
            int originY,
            int originZ,
            int radius,
            int step,
            int width,
            int modeId,
            byte[] heights,
            byte[] terrain,
            List<com.effecoria.core.tower.PhiSonarService.Blip> blips)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PhiSonarMapPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "phi_sonar_map"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PhiSonarMapPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeVarInt(p.originX());
                            buf.writeVarInt(p.originY());
                            buf.writeVarInt(p.originZ());
                            buf.writeVarInt(p.radius());
                            buf.writeVarInt(p.step());
                            buf.writeVarInt(p.width());
                            buf.writeVarInt(p.modeId());
                            buf.writeVarInt(p.heights().length);
                            buf.writeBytes(p.heights());
                            buf.writeVarInt(p.terrain().length);
                            buf.writeBytes(p.terrain());
                            buf.writeVarInt(p.blips().size());
                            for (var blip : p.blips()) {
                                buf.writeShort(blip.relX());
                                buf.writeShort(blip.relZ());
                                buf.writeByte(blip.kind());
                            }
                        },
                        buf -> {
                            int ox = buf.readVarInt();
                            int oy = buf.readVarInt();
                            int oz = buf.readVarInt();
                            int radius = buf.readVarInt();
                            int step = buf.readVarInt();
                            int width = buf.readVarInt();
                            int modeId = buf.readVarInt();
                            int heightLen = Math.min(128 * 128, Math.max(0, buf.readVarInt()));
                            byte[] heights = new byte[heightLen];
                            buf.readBytes(heights);
                            int terrainLen = Math.min(128 * 128, Math.max(0, buf.readVarInt()));
                            byte[] terrain = new byte[terrainLen];
                            buf.readBytes(terrain);
                            int blipCount = Math.min(128, Math.max(0, buf.readVarInt()));
                            List<com.effecoria.core.tower.PhiSonarService.Blip> blips = new ArrayList<>(blipCount);
                            for (int i = 0; i < blipCount; i++) {
                                blips.add(new com.effecoria.core.tower.PhiSonarService.Blip(
                                        buf.readShort(), buf.readShort(), buf.readByte()));
                            }
                            return new PhiSonarMapPayload(
                                    ox, oy, oz, radius, step, width, modeId, heights, terrain, blips);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(PhiSonarMapPayload payload, IPayloadContext context) {
            context.enqueueWork(
                    () -> com.effecoria.client.ClientPhiSonarMap.accept(
                            payload.originX(),
                            payload.originY(),
                            payload.originZ(),
                            payload.radius(),
                            payload.step(),
                            payload.width(),
                            payload.modeId(),
                            payload.heights(),
                            payload.terrain(),
                            payload.blips()));
        }
    }

    /** Client → server: remote tower command (scan / turret / beacon / open console). */
    public record TowerRemoteCommandPayload(BlockPos accessPos, int actionId, BlockPos targetPos, int modeId)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TowerRemoteCommandPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "tower_remote_cmd"));

        public static final StreamCodec<RegistryFriendlyByteBuf, TowerRemoteCommandPayload> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        TowerRemoteCommandPayload::accessPos,
                        ByteBufCodecs.VAR_INT,
                        TowerRemoteCommandPayload::actionId,
                        BlockPos.STREAM_CODEC,
                        TowerRemoteCommandPayload::targetPos,
                        ByteBufCodecs.VAR_INT,
                        TowerRemoteCommandPayload::modeId,
                        TowerRemoteCommandPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(TowerRemoteCommandPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                        player, com.effecoria.core.technomagic.TechnomagicEra.VI)) {
                    return;
                }
                BlockPos target = payload.targetPos().equals(BlockPos.ZERO) ? null : payload.targetPos();
                com.effecoria.core.tower.TowerRemoteService.execute(
                        player, payload.accessPos(), payload.actionId(), target, payload.modeId());
            });
        }
    }

    /** Client → server: apply a Lex Loci Phoenix word program via the tower console. */
    public record ApplyLociProgramPayload(BlockPos consolePos, List<String> tokens)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ApplyLociProgramPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "apply_loci_program"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ApplyLociProgramPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeBlockPos(p.consolePos());
                            buf.writeVarInt(p.tokens().size());
                            for (String token : p.tokens()) {
                                buf.writeUtf(token == null ? "" : token, 128);
                            }
                        },
                        buf -> {
                            BlockPos pos = buf.readBlockPos();
                            int n = buf.readVarInt();
                            List<String> tokens = new ArrayList<>(n);
                            for (int i = 0; i < n; i++) {
                                tokens.add(buf.readUtf(128));
                            }
                            return new ApplyLociProgramPayload(pos, tokens);
                        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ApplyLociProgramPayload payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return;
                }
                if (!(player.containerMenu instanceof com.effecoria.alchemy.menu.TowerConsoleMenu menu)
                        || !menu.blockEntity().getBlockPos().equals(payload.consolePos())) {
                    return;
                }
                if (payload.tokens().size() > com.effecoria.core.loci.LexLociCompiler.MAX_TOKENS) {
                    return;
                }
                var status = com.effecoria.core.loci.LexLociService.apply(
                        player, payload.consolePos(), payload.tokens());
                if (status == com.effecoria.core.loci.LexLociService.ApplyStatus.BAD_PROGRAM) {
                    player.displayClientMessage(
                            Component.translatable("message.effecoria.tower.loci_invalid"), true);
                }
            });
        }
    }
}
