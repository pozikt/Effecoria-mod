package com.effecoria.core.technomagic;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.effecoria.core.psi.ModAttachments;

/**
 * Technomagic discovery + era completion. Crafting stays free; operating later-era machines
 * requires all available nodes of earlier eras to be discovered.
 */
public final class TechnomagicProgress {
    public static final StreamCodec<RegistryFriendlyByteBuf, TechnomagicProgress> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.VAR_INT.encode(buf, data.discovered.size());
                for (String id : data.discovered) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, id);
                }
            },
            buf -> {
                TechnomagicProgress data = createDefault();
                int n = ByteBufCodecs.VAR_INT.decode(buf);
                for (int i = 0; i < n; i++) {
                    data.discovered.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                return data;
            });

    private final Set<String> discovered = new HashSet<>();

    public static TechnomagicProgress createDefault() {
        return new TechnomagicProgress();
    }

    public boolean isDiscovered(ResourceLocation id) {
        return discovered.contains(id.toString());
    }

    public boolean isDiscovered(TechnomagicNode node) {
        return isDiscovered(node.id());
    }

    /** @return true if newly discovered */
    public boolean discover(ResourceLocation id) {
        return discovered.add(id.toString());
    }

    public Set<String> discovered() {
        return discovered;
    }

    /** True when every {@code available} catalog node of this era is discovered. */
    public boolean isEraComplete(TechnomagicEra era) {
        boolean sawAvailable = false;
        for (TechnomagicNode node : TechnomagicCatalog.byEra(era)) {
            if (node.status() != TechnomagicNode.TechnomagicStatus.AVAILABLE) {
                continue;
            }
            sawAvailable = true;
            if (!isDiscovered(node)) {
                return false;
            }
        }
        // Eras with only planned stubs count as incomplete until playable content exists.
        return sawAvailable;
    }

    /**
     * Era I is always operable. Era N requires eras 1..N-1 complete
     * (all available nodes discovered).
     */
    public boolean canOperateEra(TechnomagicEra era) {
        for (TechnomagicEra prior : TechnomagicEra.values()) {
            if (prior.number() >= era.number()) {
                break;
            }
            if (!isEraComplete(prior)) {
                return false;
            }
        }
        return true;
    }

    /** First incomplete era strictly before {@code era}, or null. */
    public TechnomagicEra firstIncompleteEraBefore(TechnomagicEra era) {
        for (TechnomagicEra prior : TechnomagicEra.values()) {
            if (prior.number() >= era.number()) {
                return null;
            }
            if (!isEraComplete(prior)) {
                return prior;
            }
        }
        return null;
    }

    public int discoveredCount(TechnomagicEra era) {
        int n = 0;
        for (TechnomagicNode node : TechnomagicCatalog.byEra(era)) {
            if (node.status() == TechnomagicNode.TechnomagicStatus.AVAILABLE && isDiscovered(node)) {
                n++;
            }
        }
        return n;
    }

    public int availableCount(TechnomagicEra era) {
        int n = 0;
        for (TechnomagicNode node : TechnomagicCatalog.byEra(era)) {
            if (node.status() == TechnomagicNode.TechnomagicStatus.AVAILABLE) {
                n++;
            }
        }
        return n;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String id : discovered) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("discovered", list);
        return tag;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        discovered.clear();
        ListTag list = tag.getList("discovered", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            discovered.add(list.getString(i));
        }
    }

    public static TechnomagicProgress get(ServerPlayer player) {
        return player.getData(ModAttachments.TECHNOMAGIC.get());
    }

    public static void set(ServerPlayer player, TechnomagicProgress progress) {
        player.setData(ModAttachments.TECHNOMAGIC.get(), progress);
        player.syncData(ModAttachments.TECHNOMAGIC.get());
    }

    public static boolean tryDiscover(ServerPlayer player, ResourceLocation nodeId) {
        TechnomagicProgress progress = get(player);
        TechnomagicEra eraBefore = TechnomagicCatalog.get(nodeId).map(TechnomagicNode::era).orElse(null);
        boolean wasComplete = eraBefore != null && progress.isEraComplete(eraBefore);
        if (!progress.discover(nodeId)) {
            return false;
        }
        set(player, progress);
        if (eraBefore != null && !wasComplete && progress.isEraComplete(eraBefore)) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.technomagic_era_complete",
                            Component.translatable(eraBefore.translationKey())),
                    false);
        }
        return true;
    }
}
