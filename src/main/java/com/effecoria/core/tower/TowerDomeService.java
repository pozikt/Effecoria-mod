package com.effecoria.core.tower;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.entity.PhiConstructEntity;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/** Passive / combat Φ-dome around a consecrated soulbound Mage Tower. */
public final class TowerDomeService {
    private static final DustParticleOptions COMBAT_DUST =
            new DustParticleOptions(new Vector3f(0.25f, 0.35f, 0.95f), 1.1f);

    private TowerDomeService() {}

    public static void serverTick(Level level, BlockPos pos, TowerAnchorBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.isClientSide()) {
            return;
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }

        be.refreshIntegrity(server);
        boolean online = isOnline(server, be);
        if (!online) {
            if (be.domeCombat()) {
                be.setDomeCombat(false);
            }
            be.setDomePowered(false);
            if (be.clientDomeSynced()) {
                syncClear(server, pos);
                be.setClientDomeSynced(false);
            }
            return;
        }

        int baseLoad = Math.max(1, (int) Math.round(4.0 * Math.max(0.35, be.phiScatter())));
        int load = be.domeCombat() ? Math.max(1, Math.round(baseLoad * 2.5f)) : baseLoad;
        boolean powered = PhiPower.consumeTick(server, pos, load);
        be.setDomePowered(powered);
        if (!powered) {
            if (be.clientDomeSynced()) {
                syncClear(server, pos);
                be.setClientDomeSynced(false);
            }
            return;
        }

        AABB dome = domeVolume(be);
        applyEffects(server, be, dome);
        if (be.domeCombat()) {
            spawnCombatParticles(server, dome);
            syncCombat(server, pos, dome);
            be.setClientDomeSynced(true);
        } else if (be.clientDomeSynced()) {
            syncClear(server, pos);
            be.setClientDomeSynced(false);
        }
    }

    public static boolean isOnline(ServerLevel level, TowerAnchorBlockEntity be) {
        return be.consecrated()
                && be.bound()
                && be.ownerUuid() != null
                && be.integrity() >= TowerStructureValidator.MIN_INTEGRITY
                && PhiPower.hasPower(level, be.getBlockPos());
    }

    public static AABB domeVolume(TowerAnchorBlockEntity be) {
        double pad = Math.max(2.0, Math.min(8.0, 2.0 + be.gluedCells() / 80.0));
        return be.structureBounds().inflate(pad);
    }

    private static void applyEffects(ServerLevel level, TowerAnchorBlockEntity be, AABB dome) {
        UUID ownerId = be.ownerUuid();
        Vec3 center = dome.getCenter();
        boolean combat = be.domeCombat();

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, dome, LivingEntity::isAlive);
        for (LivingEntity entity : entities) {
            if (entity instanceof Player player) {
                if (ownerId != null && ownerId.equals(player.getUUID())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                }
                continue;
            }
            if (isFriendlyConstruct(entity, ownerId)) {
                continue;
            }
            if (!(entity instanceof Monster || entity instanceof Enemy)) {
                continue;
            }

            entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 40, combat ? 1 : 0, true, false, true));

            Vec3 push = entity.position().subtract(center);
            if (push.lengthSqr() < 1.0e-4) {
                push = new Vec3(0.15, 0.05, 0);
            } else {
                push = push.normalize();
            }
            double strength = combat ? 0.35 : 0.12;
            entity.push(push.x * strength, 0.08 + (combat ? 0.06 : 0.0), push.z * strength);
            entity.hurtMarked = true;

            if (combat && level.getGameTime() % 20 == 0) {
                entity.hurt(level.damageSources().magic(), 2.0f);
            }
        }
    }

    private static boolean isFriendlyConstruct(LivingEntity entity, UUID ownerId) {
        if (ownerId == null) {
            return false;
        }
        if (entity instanceof PhiConstructEntity construct) {
            return ownerId.equals(construct.getOwnerUUID());
        }
        if (entity instanceof OwnableEntity ownable) {
            return ownerId.equals(ownable.getOwnerUUID());
        }
        return false;
    }

    private static void spawnCombatParticles(ServerLevel level, AABB box) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        // Sparse edge samples — 8 corners + mid-edges.
        double[] xs = {box.minX, box.getCenter().x, box.maxX};
        double[] ys = {box.minY, box.getCenter().y, box.maxY};
        double[] zs = {box.minZ, box.getCenter().z, box.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    // Skip pure center point.
                    if (x == box.getCenter().x && y == box.getCenter().y && z == box.getCenter().z) {
                        continue;
                    }
                    level.sendParticles(COMBAT_DUST, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
                    if ((x == box.minX || x == box.maxX) && (y == box.minY || y == box.maxY)) {
                        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }
            }
        }
    }

    private static void syncCombat(ServerLevel level, BlockPos anchor, AABB dome) {
        ModNetworking.TowerDomeSyncPayload payload = ModNetworking.TowerDomeSyncPayload.active(anchor, dome);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(anchor) <= 96L * 96L) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private static void syncClear(ServerLevel level, BlockPos anchor) {
        ModNetworking.TowerDomeSyncPayload payload = ModNetworking.TowerDomeSyncPayload.clear(anchor);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(anchor) <= 96L * 96L) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
