package com.effecoria.core.psi;

import com.effecoria.EffecoriaMod;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentHolder;

import java.util.function.Supplier;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EffecoriaMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerPsiData>> PSI = ATTACHMENT_TYPES.register("psi", () ->
            AttachmentType.builder(PlayerPsiData::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public PlayerPsiData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            PlayerPsiData data = PlayerPsiData.createDefault();
                            data.load(provider, tag);
                            return data;
                        }

                        @Override
                        public CompoundTag write(PlayerPsiData attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .sync(PlayerPsiData.STREAM_CODEC, (holder, player) -> holder == player)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(PSI.get());
    }
}
