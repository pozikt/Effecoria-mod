package com.effecoria.effect.necromancy;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import javax.annotation.Nullable;

/** Last death site + cause for a player — used by death sense / echo / rest theft. */
public final class PlayerLastDeath {
    private boolean present;
    @Nullable
    private ResourceKey<Level> dimension;
    private double x;
    private double y;
    private double z;
    private String cause = "";
    private long gameTime;

    public static PlayerLastDeath createDefault() {
        return new PlayerLastDeath();
    }

    public boolean present() {
        return present && dimension != null;
    }

    public void record(ServerPlayer player) {
        present = true;
        dimension = player.level().dimension();
        x = player.getX();
        y = player.getY();
        z = player.getZ();
        gameTime = player.level().getGameTime();
        DamageSource source = player.getLastDamageSource();
        cause = source != null ? source.getLocalizedDeathMessage(player).getString() : "";
    }

    public Optional<DeathSite> site() {
        if (!present()) {
            return Optional.empty();
        }
        return Optional.of(new DeathSite(dimension, new Vec3(x, y, z), cause, gameTime));
    }

    public String cause() {
        return cause == null ? "" : cause;
    }

    public long gameTime() {
        return gameTime;
    }

    public void load(HolderLookup.Provider provider, CompoundTag tag) {
        present = tag.getBoolean("Present");
        if (tag.contains("Dim")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Dim"));
            dimension = id != null ? ResourceKey.create(Registries.DIMENSION, id) : null;
        } else {
            dimension = null;
        }
        x = tag.getDouble("X");
        y = tag.getDouble("Y");
        z = tag.getDouble("Z");
        cause = tag.contains("Cause") ? tag.getString("Cause") : "";
        gameTime = tag.contains("GameTime") ? tag.getLong("GameTime") : 0L;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Present", present);
        if (dimension != null) {
            tag.putString("Dim", dimension.location().toString());
        }
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putString("Cause", cause == null ? "" : cause);
        tag.putLong("GameTime", gameTime);
        return tag;
    }

    public record DeathSite(ResourceKey<Level> dimension, Vec3 pos, String cause, long gameTime) {}
}
