package com.effecoria.network;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.magic.CastPipeline;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetworking {
    private ModNetworking() {}

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
