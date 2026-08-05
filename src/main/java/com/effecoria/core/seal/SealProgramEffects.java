package com.effecoria.core.seal;

import com.effecoria.content.ModItems;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** One-shot action helpers for reactive seal programs. */
public final class SealProgramEffects {
    private SealProgramEffects() {}

    public static void hurtOnce(ServerLevel level, BlockPos pos, SealInstance seal, float damage) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            entity.hurt(level.damageSources().magic(), Math.max(0.5f, damage));
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    pos.getX() + 0.5,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.5,
                    4,
                    0.15,
                    0.05,
                    0.15,
                    0.01);
        }
    }

    public static void slowOnce(ServerLevel level, BlockPos pos, SealInstance seal, int amp) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.45, 0.25, 0.45);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, amp - 1)));
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, 40, 1));
        }
    }

    public static void pushOnce(ServerLevel level, BlockPos pos, SealInstance seal, float force) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.4, 0.2, 0.4);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, force, 0));
            entity.hurtMarked = true;
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    pos.getX() + 0.5,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5,
                    6,
                    0.2,
                    0.1,
                    0.2,
                    0.02);
        }
    }

    public static void calorOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        int seconds = Math.max(1, Math.round(magnitude));
        AABB box = new AABB(pos).inflate(1.1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            entity.igniteForSeconds(seconds);
        }
        level.sendParticles(
                ParticleTypes.FLAME,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                10,
                0.3,
                0.2,
                0.3,
                0.01);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.35f, 1.1f);
    }

    public static void extrahereOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        float drain = Math.max(2f, magnitude);
        AABB box = new AABB(pos).inflate(4.0);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
            PlayerPsiData data = player.getData(ModAttachments.PSI.get());
            float next = Math.max(0f, data.currentPsi() - drain);
            data.setCurrentPsi(next);
            level.sendParticles(
                    ParticleTypes.WITCH,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    6,
                    0.2,
                    0.3,
                    0.2,
                    0.02);
        }
        level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.6f);
    }

    public static void imprimereOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        AABB box = new AABB(pos).inflate(2.5);
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
            double d = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < best) {
                best = d;
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }
        ItemStack stack = new ItemStack(ModItems.ESSENITE_DUST.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.effecoria.seal_imprint"));
        stack.set(
                DataComponents.LORE,
                new ItemLore(List.of(Component.translatable(
                        "item.effecoria.seal_imprint.lore",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        Math.round(magnitude)))));
        if (!nearest.addItem(stack)) {
            nearest.drop(stack, false);
        }
        nearest.displayClientMessage(Component.translatable("message.effecoria.seal.imprimere"), true);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
    }

    public static void ordoOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        float damp = Math.max(1f, magnitude);
        AABB box = new AABB(pos).inflate(5.0);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
            PlayerPsiData data = player.getData(ModAttachments.PSI.get());
            data.setEntropyB(Math.max(0f, data.entropyB() - damp));
            BreathDebuffs.apply(level, seal.casterId(), player, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0));
        }
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                12,
                0.4,
                0.3,
                0.4,
                0.01);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 1.0f);
    }

    public static void abnegatioOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        int ticks = 40 + Math.round(magnitude * 10f);
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.6, 0.4, 0.6);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            entity.getPersistentData().putLong("effecoria_abnegatio_until", level.getGameTime() + ticks);
            entity.noPhysics = true;
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
        }
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                16,
                0.25,
                0.25,
                0.25,
                0.02);
    }

    public static void absolutumOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        hurtOnce(level, pos, seal, Math.max(6f, magnitude));
        level.sendParticles(
                ParticleTypes.EXPLOSION,
                pos.getX() + 0.5,
                pos.getY() + 0.6,
                pos.getZ() + 0.5,
                1,
                0,
                0,
                0,
                0);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.55f, 1.35f);
        SealService.remove(level, pos);
        if (level.getBlockState(pos).getDestroySpeed(level, pos) >= 0f
                && !level.getBlockState(pos).is(Blocks.BEDROCK)) {
            level.destroyBlock(pos, true);
        }
    }

    /**
     * Remote sentinel: alert the seal's caster (message + chime at caster), local particles on the glyph.
     * Distinct from {@code sound}, which only plays at the block.
     */
    public static void vigilOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 1.1,
                pos.getZ() + 0.5,
                10,
                0.25,
                0.15,
                0.25,
                0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.45f, 1.35f);

        if (seal.casterId() == null || level.getServer() == null) {
            return;
        }
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(seal.casterId());
        if (caster == null) {
            return;
        }
        float vol = Math.min(1.0f, 0.45f + magnitude * 0.05f);
        caster.displayClientMessage(
                Component.translatable("message.effecoria.seal.vigil", pos.getX(), pos.getY(), pos.getZ()), true);
        caster.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, vol, 1.2f);
    }

    /**
     * Standing siphon: drain Ψ from players on the seal block and return a portion to the caster.
     * Tighter than {@code extrahere} (radius AoE) — trap-lane identity.
     */
    public static void haustusOnce(ServerLevel level, BlockPos pos, SealInstance seal, float magnitude) {
        float drain = Math.max(3f, magnitude);
        float refundRatio = 0.5f;
        float refunded = 0f;
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            if (seal.casterId() != null && seal.casterId().equals(player.getUUID())) {
                continue;
            }
            PlayerPsiData data = player.getData(ModAttachments.PSI.get());
            float taken = Math.min(drain, data.currentPsi());
            if (taken <= 0.05f) {
                continue;
            }
            data.setCurrentPsi(Math.max(0f, data.currentPsi() - taken));
            refunded += taken * refundRatio;
            player.displayClientMessage(Component.translatable("message.effecoria.seal.haustus_victim"), true);
            level.sendParticles(
                    ParticleTypes.WITCH,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    8,
                    0.2,
                    0.25,
                    0.2,
                    0.02);
        }
        if (refunded > 0.05f && seal.casterId() != null && level.getServer() != null) {
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(seal.casterId());
            if (caster != null) {
                PlayerPsiData casterData = caster.getData(ModAttachments.PSI.get());
                float next = Math.min(casterData.maxPsi(), casterData.currentPsi() + refunded);
                casterData.setCurrentPsi(next);
                caster.displayClientMessage(
                        Component.translatable("message.effecoria.seal.haustus_refund", Math.round(refunded)), true);
            }
        }
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.4f, 1.4f);
    }

    public static void applyStandingHurt(ServerLevel level, BlockPos pos, SealInstance seal, float damage) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            entity.hurt(level.damageSources().magic(), damage);
        }
    }

    public static void applyStandingSlow(ServerLevel level, BlockPos pos, SealInstance seal, int amp) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.45, 0.25, 0.45);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            BreathDebuffs.apply(level, seal.casterId(), entity, new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 25, Math.max(0, amp - 1)));
        }
    }

    public static void applyStandingPush(ServerLevel level, BlockPos pos, SealInstance seal, float force) {
        AABB box = new AABB(pos).move(0, 1, 0).inflate(0.35, 0.25, 0.35);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            BlockPos standingOn = BlockPos.containing(entity.getX(), entity.getY() - 0.05, entity.getZ());
            if (!standingOn.equals(pos)) {
                continue;
            }
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, force, 0));
            entity.hurtMarked = true;
        }
    }

    /** Clears abnegatio noPhysics when the buff window ends. */
    public static void tickAbnegatio(LivingEntity living, long gameTime) {
        var pd = living.getPersistentData();
        if (!pd.contains("effecoria_abnegatio_until")) {
            return;
        }
        if (gameTime >= pd.getLong("effecoria_abnegatio_until")) {
            pd.remove("effecoria_abnegatio_until");
            living.noPhysics = false;
        } else {
            living.noPhysics = true;
        }
    }
}
