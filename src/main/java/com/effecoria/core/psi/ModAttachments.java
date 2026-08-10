package com.effecoria.core.psi;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.disease.DiseaseProfile;
import com.effecoria.core.technomagic.TechnomagicProgress;
import com.effecoria.core.seal.ChunkSealData;
import com.effecoria.effect.necromancy.PlayerLastDeath;
import com.effecoria.effect.organic.gene.GeneProfile;
import com.effecoria.effect.spatial.SubspaceVoyageData;

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

    public static final Supplier<AttachmentType<ChunkSealData>> CHUNK_SEALS = ATTACHMENT_TYPES.register(
            "chunk_seals",
            () -> AttachmentType.builder(ChunkSealData::new)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public ChunkSealData read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            ChunkSealData data = new ChunkSealData();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(ChunkSealData attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .sync(ChunkSealData.STREAM_CODEC)
                    .build());

    public static final Supplier<AttachmentType<SubspaceVoyageData>> SUBSPACE_VOYAGE = ATTACHMENT_TYPES.register(
            "subspace_voyage",
            () -> AttachmentType.builder(SubspaceVoyageData::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public SubspaceVoyageData read(
                                IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            SubspaceVoyageData data = SubspaceVoyageData.createDefault();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(SubspaceVoyageData attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<PlayerLastDeath>> LAST_DEATH = ATTACHMENT_TYPES.register(
            "last_death",
            () -> AttachmentType.builder(PlayerLastDeath::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public PlayerLastDeath read(
                                IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            PlayerLastDeath data = PlayerLastDeath.createDefault();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(PlayerLastDeath attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    /** Gene grafts on any living host (Organic gene engineering). */
    public static final Supplier<AttachmentType<GeneProfile>> GENE_PROFILE = ATTACHMENT_TYPES.register(
            "gene_profile",
            () -> AttachmentType.builder(GeneProfile::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public GeneProfile read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            GeneProfile data = GeneProfile.createDefault();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(GeneProfile attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    /** Φ-disease profile — survives death unless clear_on_death is set. */
    public static final Supplier<AttachmentType<DiseaseProfile>> DISEASE_PROFILE = ATTACHMENT_TYPES.register(
            "disease_profile",
            () -> AttachmentType.builder(DiseaseProfile::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public DiseaseProfile read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            DiseaseProfile data = DiseaseProfile.createDefault();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(DiseaseProfile attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, DiseaseProfile.STREAM_CODEC)
                    .build());

    /** Cosmetically discovered technomagic nodes (no recipe gating). */
    public static final Supplier<AttachmentType<TechnomagicProgress>> TECHNOMAGIC = ATTACHMENT_TYPES.register(
            "technomagic",
            () -> AttachmentType.builder(TechnomagicProgress::createDefault)
                    .serialize(new IAttachmentSerializer<>() {
                        @Override
                        public TechnomagicProgress read(
                                IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                            TechnomagicProgress data = TechnomagicProgress.createDefault();
                            if (tag instanceof CompoundTag compound) {
                                data.load(provider, compound);
                            }
                            return data;
                        }

                        @Override
                        public Tag write(TechnomagicProgress attachment, HolderLookup.Provider provider) {
                            return attachment.save(provider);
                        }
                    })
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, TechnomagicProgress.STREAM_CODEC)
                    .build());

    /** Spatial pocket inventory — dumps on death (no copyOnDeath). */
    public static final Supplier<AttachmentType<com.effecoria.effect.spatial.SpatialPocketData>> SPATIAL_POCKET =
            ATTACHMENT_TYPES.register(
                    "spatial_pocket",
                    () -> AttachmentType.builder(com.effecoria.effect.spatial.SpatialPocketData::createDefault)
                            .serialize(new IAttachmentSerializer<>() {
                                @Override
                                public com.effecoria.effect.spatial.SpatialPocketData read(
                                        IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
                                    com.effecoria.effect.spatial.SpatialPocketData data =
                                            com.effecoria.effect.spatial.SpatialPocketData.createDefault();
                                    if (tag instanceof CompoundTag compound) {
                                        data.load(provider, compound);
                                    }
                                    return data;
                                }

                                @Override
                                public Tag write(
                                        com.effecoria.effect.spatial.SpatialPocketData attachment,
                                        HolderLookup.Provider provider) {
                                    return attachment.save(provider);
                                }
                            })
                            .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(PSI.get());
    }
}
