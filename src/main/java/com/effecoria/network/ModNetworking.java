package com.effecoria.network;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.psi.SpellProgression;
import com.effecoria.magic.CastPipeline;

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
                if (school == MagicSchool.NONE || school == MagicSchool.SEALS) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.invalid_school"));
                    return;
                }
                if (!SpellProgression.schoolHasLoadedSpells(school)) {
                    player.sendSystemMessage(Component.translatable("message.effecoria.spells_not_loaded"));
                    return;
                }
                PsiHelper.initiate(player, school);
                player.syncData(ModAttachments.PSI.get());
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
}
