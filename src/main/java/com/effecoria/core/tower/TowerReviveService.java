package com.effecoria.core.tower;

import com.effecoria.EffecoriaMod;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Death → tower revive pipeline (SubspaceVoyage-style deferred teleport). */
public final class TowerReviveService {
    private static final ResourceLocation BODY_HP = EffecoriaMod.id("tower_body_hp");
    private static final ResourceLocation BODY_SPEED = EffecoriaMod.id("tower_body_speed");
    private static final ResourceLocation BODY_DAMAGE = EffecoriaMod.id("tower_body_damage");

    private TowerReviveService() {}

    public static void handleDeath(ServerPlayer player) {
        if (!TowerSoulbindService.towerAliveFor(player)) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(player);
        data.prepareTowerRevive(player.totalExperience, player.experienceLevel, player.experienceProgress);
        PsiHelper.set(player, data);
    }

    public static void onRespawn(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.pendingTowerRevive() || data.towerDim() == null || data.towerPos() == null) {
            clearBodyModifiers(player);
            return;
        }

        ResourceKey<Level> dim = data.towerDim();
        BlockPos pos = data.towerPos().immutable();
        int savedXp = data.savedTowerXpTotal();

        data.clearPendingTowerRevive();
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());

        ServerLevel towerLevel = player.server.getLevel(dim);
        if (towerLevel == null) {
            return;
        }
        BlockEntity be = towerLevel.getBlockEntity(pos);
        if (!(be instanceof TowerAnchorBlockEntity anchor) || !anchor.bound()) {
            return;
        }

        TowerBodyType body = anchor.bodyType();
        if (!anchor.payBodyCosts(body)) {
            body = TowerBodyType.BASIC;
            player.displayClientMessage(Component.translatable("message.effecoria.tower.body_fallback"), true);
        }

        int delay = body.delayTicks();
        clearBodyModifiers(player);

        final TowerBodyType finalBody = body;
        Runnable finish = () -> {
            if (!player.isAlive()) {
                return;
            }
            BlockPos revive = anchor.revivePos();
            double x = revive.getX() + 0.5;
            double y = revive.getY() + 0.05;
            double z = revive.getZ() + 0.5;
            if (player.level() == towerLevel) {
                player.teleportTo(x, y, z);
            } else {
                player.changeDimension(new DimensionTransition(
                        towerLevel,
                        new Vec3(x, y, z),
                        Vec3.ZERO,
                        player.getYRot(),
                        player.getXRot(),
                        DimensionTransition.DO_NOTHING));
            }

            boolean keptXp = false;
            if (anchor.consumeSoulShardForXp() && savedXp > 0) {
                player.giveExperiencePoints(-player.totalExperience);
                player.giveExperiencePoints(savedXp);
                keptXp = true;
            }

            applyBodyModifiers(player, finalBody);
            applyReviveDrain(player, anchor.reviveCount() + 1);

            int omega = finalBody.omegaPercent();
            if (keptXp) {
                omega += 2;
            }
            anchor.addOmegaPercent(omega);
            anchor.onRevive();
            if (anchor.omegaPercent() >= 100) {
                player.displayClientMessage(Component.translatable("message.effecoria.tower.omega_critical"), true);
            }

            towerLevel.playSound(null, revive, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.85f, 1.15f);
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.tower.revived",
                            finalBody.getSerializedName(),
                            anchor.omegaPercent()),
                    true);

            PlayerPsiData after = PsiHelper.get(player);
            after.setPreferredBodyType(anchor.bodyType());
            PsiHelper.set(player, after);
            player.syncData(ModAttachments.PSI.get());
        };

        if (delay <= 0) {
            finish.run();
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, delay + 20, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, delay + 20, 6, false, false));
            player.displayClientMessage(
                    Component.translatable("message.effecoria.tower.reviving", delay / 20), true);
            player.server.tell(new net.minecraft.server.TickTask(player.server.getTickCount() + delay, finish));
        }
    }

    private static void applyBodyModifiers(ServerPlayer player, TowerBodyType body) {
        clearBodyModifiers(player);
        AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        switch (body) {
            case ENHANCED -> {
                if (hp != null) {
                    hp.addTransientModifier(new AttributeModifier(
                            BODY_HP, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                if (speed != null) {
                    speed.addTransientModifier(new AttributeModifier(
                            BODY_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                player.setHealth(player.getMaxHealth());
            }
            case COMBAT -> {
                if (hp != null) {
                    hp.addTransientModifier(new AttributeModifier(
                            BODY_HP, 0.40, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                if (damage != null) {
                    damage.addTransientModifier(new AttributeModifier(
                            BODY_DAMAGE, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60 * 30, 0, false, false));
                player.setHealth(player.getMaxHealth());
            }
            case ARCANE -> {
                PlayerPsiData psi = PsiHelper.get(player);
                float bonus = psi.maxPsi() * 0.50f;
                psi.setMaxPsi(psi.maxPsi() + bonus);
                psi.setCurrentPsi(psi.maxPsi());
                PsiHelper.set(player, psi);
                player.syncData(ModAttachments.PSI.get());
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60 * 10, 0, false, false));
            }
            case BASIC -> player.setHealth(player.getMaxHealth());
        }
    }

    public static void clearBodyModifiers(ServerPlayer player) {
        AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (hp != null) {
            hp.removeModifier(BODY_HP);
        }
        if (speed != null) {
            speed.removeModifier(BODY_SPEED);
        }
        if (damage != null) {
            damage.removeModifier(BODY_DAMAGE);
        }
    }

    private static void applyReviveDrain(ServerPlayer player, int reviveCount) {
        if (reviveCount < 4) {
            return;
        }
        PlayerPsiData psi = PsiHelper.get(player);
        float factor = reviveCount <= 5 ? 0.90f : 0.75f;
        psi.setMaxPsi(Math.max(10f, psi.maxPsi() * factor));
        psi.setCurrentPsi(Math.min(psi.currentPsi(), psi.maxPsi()));
        if (reviveCount >= 6) {
            psi.setSoulStrength(Math.max(0.4f, psi.soulStrength() * 0.95f));
        }
        PsiHelper.set(player, psi);
        player.syncData(ModAttachments.PSI.get());
    }
}
