package com.effecoria.core.technomagic;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.PhiTelegraphBlock;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Vector3f;

/** Pairing + same-dimension pulse for Φ-telegraphs. */
public final class TelegraphService {
    private static final String ANCHOR_TAG = "effecoria:telegraph_anchor";
    private static final int COOLDOWN = 40;

    private TelegraphService() {}

    public static void handlePairClick(
            ServerPlayer player,
            ServerLevel level,
            BlockPos pos,
            PhiTelegraphBlock.PhiTelegraphBlockEntity telegraph) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ANCHOR_TAG)) {
            CompoundTag anchor = new CompoundTag();
            anchor.putString("dim", level.dimension().location().toString());
            anchor.putInt("x", pos.getX());
            anchor.putInt("y", pos.getY());
            anchor.putInt("z", pos.getZ());
            data.put(ANCHOR_TAG, anchor);
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_anchor_set"), true);
            return;
        }
        CompoundTag anchor = data.getCompound(ANCHOR_TAG);
        ResourceLocation dimId = ResourceLocation.parse(anchor.getString("dim"));
        ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
        BlockPos otherPos = new BlockPos(anchor.getInt("x"), anchor.getInt("y"), anchor.getInt("z"));
        data.remove(ANCHOR_TAG);

        if (otherPos.equals(pos) && dim.equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_same"), true);
            return;
        }
        if (!dim.equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_cross_dim"), true);
            return;
        }
        BlockEntity otherBe = level.getBlockEntity(otherPos);
        if (!(otherBe instanceof PhiTelegraphBlock.PhiTelegraphBlockEntity other)) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_anchor_lost"), true);
            return;
        }
        telegraph.setLink(level.dimension(), otherPos);
        other.setLink(level.dimension(), pos);
        player.displayClientMessage(Component.translatable("message.effecoria.telegraph_linked"), true);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 1.4f);
    }

    public static boolean sendPulse(
            ServerPlayer player,
            ServerLevel level,
            PhiTelegraphBlock.PhiTelegraphBlockEntity source,
            String message) {
        if (source.onCooldown()) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_cooldown"), true);
            return false;
        }
        if (!source.hasLink() || source.linkedPos() == null || source.linkedDim() == null) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_unpaired"), true);
            return false;
        }
        if (!source.linkedDim().equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_cross_dim"), true);
            return false;
        }
        if (!source.tryConsumeCell()) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_need_cell"), true);
            return false;
        }
        BlockEntity destBe = level.getBlockEntity(source.linkedPos());
        if (!(destBe instanceof PhiTelegraphBlock.PhiTelegraphBlockEntity dest)) {
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_anchor_lost"), true);
            source.clearLink();
            return false;
        }
        String text = message == null || message.isBlank() ? "…" : message;
        dest.setLastMessage(text);
        dest.beginCooldown(COOLDOWN);
        source.beginCooldown(COOLDOWN);
        pulseFx(level, source.getBlockPos());
        pulseFx(level, dest.getBlockPos());
        // Notify nearby players at destination
        for (ServerPlayer near : level.players()) {
            if (near.distanceToSqr(dest.getBlockPos().getX() + 0.5, dest.getBlockPos().getY() + 0.5, dest.getBlockPos().getZ() + 0.5)
                    <= 64 * 64) {
                near.displayClientMessage(
                        Component.translatable("message.effecoria.telegraph_receive", text), false);
            }
        }
        player.displayClientMessage(Component.translatable("message.effecoria.telegraph_sent"), true);
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                source.getBlockPos().getX(),
                source.getBlockPos().getY(),
                source.getBlockPos().getZ(),
                48,
                new ModNetworking.TelegraphPulsePayload(
                        source.getBlockPos().getX(),
                        source.getBlockPos().getY(),
                        source.getBlockPos().getZ(),
                        dest.getBlockPos().getX(),
                        dest.getBlockPos().getY(),
                        dest.getBlockPos().getZ()));
        return true;
    }

    private static void pulseFx(ServerLevel level, BlockPos pos) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.55f, 0.75f, 1.0f), 1.2f);
        for (int i = 0; i < 12; i++) {
            level.sendParticles(
                    dust,
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    1,
                    0.25,
                    0.2,
                    0.25,
                    0.01);
        }
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                4,
                0.15,
                0.15,
                0.15,
                0.02);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
    }
}
