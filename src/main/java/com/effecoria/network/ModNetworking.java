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
import com.effecoria.magic.CastPipeline;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    public record CastSpellPayload(int spellIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EffecoriaMod.MOD_ID, "cast_spell"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CastSpellPayload::spellIndex,
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
                CastPipeline.tryCastSelected(player);
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
                com.effecoria.core.progression.FirstHourTips.tryShow(
                        player, com.effecoria.core.progression.FirstHourTips.Tip.OPEN_HUB);
            });
        }
    }
}
