package com.effecoria.core.technomagic;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.effecoria.core.psi.ModAttachments;

/** Cosmetic discovery of technomagic nodes (does not gate recipes). */
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
        if (progress.discover(nodeId)) {
            set(player, progress);
            return true;
        }
        return false;
    }
}
