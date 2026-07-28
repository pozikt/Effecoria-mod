package com.effecoria.core.psi;

import com.effecoria.EffecoriaMod;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EffecoriaMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerPsiData>> PSI = ATTACHMENT_TYPES.register("psi", () ->
            AttachmentType.builder(PlayerPsiData::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public PlayerPsiData read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            PlayerPsiData data = PlayerPsiData.createDefault();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(PlayerPsiData attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, PlayerPsiData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(PSI.get());
    }
}
