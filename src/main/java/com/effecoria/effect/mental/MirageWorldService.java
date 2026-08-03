package com.effecoria.effect.mental;

import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-victim illusory terrain via client-only block updates.
 * Real body stays as an armor-stand shell at the cast point; the player (soul) walks the mirage.
 */
public final class MirageWorldService {
    public static final String BODY_TAG = "effecoria:mirage_body";

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final int RADIUS = 4;
    private static final int HEIGHT = 5;
    private static final int RESYNC_INTERVAL = 40;

    private MirageWorldService() {}

    public static void start(ServerPlayer victim, ServerPlayer caster, int durationTicks, float pulseDamage) {
        endQuiet(victim);
        float maxHp = Math.max(8f, victim.getMaxHealth());
        BlockPos origin = victim.blockPosition().immutable();
        Vec3 bodyPos = victim.position();
        float yRot = victim.getYRot();
        float xRot = victim.getXRot();

        Session session = new Session(
                victim.getUUID(),
                caster.getUUID(),
                victim.level().getGameTime() + Math.max(80, durationTicks),
                maxHp,
                maxHp,
                Mth.clamp(pulseDamage, 0.5f, 6f),
                origin,
                bodyPos,
                yRot,
                xRot);
        buildChamber(victim.serverLevel(), session, victim.getRandom());
        session.bodyId = spawnBodyShell(victim, session);
        enterSoul(victim);
        SESSIONS.put(victim.getUUID(), session);
        pushAll(victim, session);
        PacketDistributor.sendToPlayer(
                victim, new ModNetworking.MirageStartPayload(durationTicks, maxHp, 1f));
        victim.displayClientMessage(Component.translatable("message.effecoria.mental.mirage_enter"), true);
        caster.displayClientMessage(
                Component.translatable("message.effecoria.mental.mirage_cast", victim.getDisplayName()), true);
    }

    public static boolean isActive(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        return s != null && player.level().getGameTime() < s.endTick;
    }

    public static boolean isMirageBlock(ServerPlayer player, BlockPos pos) {
        Session s = SESSIONS.get(player.getUUID());
        return s != null && s.fakes.containsKey(pos.immutable());
    }

    public static void resend(ServerPlayer player, BlockPos pos) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) {
            return;
        }
        BlockState fake = s.fakes.get(pos.immutable());
        if (fake != null) {
            player.connection.send(new ClientboundBlockUpdatePacket(pos, fake));
        }
    }

    public static void playerTick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel) || player.level().getGameTime() >= session.endTick) {
            return;
        }
        // Soul ignores real collision; mirage solids are applied below.
        player.noPhysics = true;
        player.setInvisible(true);
        applyPhysics(player, session);
        keepBodyShell(player.serverLevel(), session);
        session.ticksAlive++;
        if (session.ticksAlive % RESYNC_INTERVAL == 0) {
            pushAll(player, session);
        }
        if (session.ticksAlive % 10 == 0) {
            tickHazards(player, session);
        }
    }

    public static void tick(ServerLevel level) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            Session session = entry.getValue();
            ServerPlayer victim = level.getServer().getPlayerList().getPlayer(session.victimId);
            if (victim == null) {
                if (now >= session.endTick) {
                    for (ServerLevel dim : level.getServer().getAllLevels()) {
                        discardBody(dim, session);
                    }
                    it.remove();
                }
                continue;
            }
            // Only the victim's dimension owns expiry — never tear down from other dims' ticks.
            if (victim.level() != level) {
                continue;
            }
            if (now >= session.endTick || session.illusoryHp <= 0f) {
                finish(victim, session, session.illusoryHp <= 0f);
                it.remove();
            }
        }
    }

    public static void onLogout(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            cleanup(player, session, false);
        }
    }

    private static void endQuiet(ServerPlayer victim) {
        Session prev = SESSIONS.remove(victim.getUUID());
        if (prev != null) {
            cleanup(victim, prev, false);
        }
    }

    private static void finish(ServerPlayer victim, Session session, boolean collapsed) {
        cleanup(victim, session, true);
        PacketDistributor.sendToPlayer(victim, new ModNetworking.MirageEndPayload(collapsed));
        if (collapsed) {
            victim.displayClientMessage(Component.translatable("message.effecoria.mental.mirage_collapse"), true);
            ServerPlayer caster = victim.server.getPlayerList().getPlayer(session.casterId);
            if (caster != null) {
                caster.displayClientMessage(
                        Component.translatable("message.effecoria.mental.mirage_kill", victim.getDisplayName()),
                        true);
            }
        } else {
            victim.displayClientMessage(Component.translatable("message.effecoria.mental.mirage_fade"), true);
        }
    }

    private static void cleanup(ServerPlayer victim, Session session, boolean returnToBody) {
        restoreBlocks(victim, session);
        discardBody(victim.serverLevel(), session);
        exitSoul(victim);
        if (returnToBody) {
            victim.teleportTo(session.bodyPos.x, session.bodyPos.y, session.bodyPos.z);
            victim.setYRot(session.bodyYRot);
            victim.setXRot(session.bodyXRot);
            victim.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void enterSoul(ServerPlayer victim) {
        victim.setInvisible(true);
        victim.noPhysics = true;
        victim.setDeltaMovement(Vec3.ZERO);
    }

    private static void exitSoul(ServerPlayer victim) {
        victim.setInvisible(false);
        victim.noPhysics = false;
        victim.setDeltaMovement(Vec3.ZERO);
    }

    private static UUID spawnBodyShell(ServerPlayer victim, Session session) {
        ServerLevel level = victim.serverLevel();
        ArmorStand body = new ArmorStand(level, session.bodyPos.x, session.bodyPos.y, session.bodyPos.z);
        body.setYRot(session.bodyYRot);
        body.setXRot(session.bodyXRot);
        body.setNoGravity(true);
        body.setInvulnerable(true);
        body.setSilent(true);
        body.setInvisible(false);
        body.setShowArms(true);
        body.setCustomName(victim.getDisplayName().copy());
        body.setCustomNameVisible(true);
        body.setNoBasePlate(true);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            body.setItemSlot(slot, victim.getItemBySlot(slot).copy());
        }
        body.getPersistentData().putBoolean(BODY_TAG, true);
        body.getPersistentData().putUUID("effecoria:mirage_victim", victim.getUUID());
        body.getPersistentData().putLong("effecoria:mirage_until", session.endTick);
        level.addFreshEntity(body);
        return body.getUUID();
    }

    private static void keepBodyShell(ServerLevel level, Session session) {
        if (session.bodyId == null) {
            return;
        }
        var entity = level.getEntity(session.bodyId);
        if (!(entity instanceof ArmorStand body) || !body.isAlive()) {
            return;
        }
        body.teleportTo(session.bodyPos.x, session.bodyPos.y, session.bodyPos.z);
        body.setYRot(session.bodyYRot);
        body.setXRot(session.bodyXRot);
        body.setDeltaMovement(Vec3.ZERO);
    }

    private static void discardBody(ServerLevel level, Session session) {
        if (session.bodyId == null) {
            return;
        }
        var entity = level.getEntity(session.bodyId);
        if (entity != null) {
            entity.discard();
        }
        session.bodyId = null;
    }

    private static void tickHazards(ServerPlayer victim, Session session) {
        BlockPos feet = victim.blockPosition();
        BlockPos below = feet.below();
        boolean hazard = isHazard(session.fakes.get(feet)) || isHazard(session.fakes.get(below));
        if (!hazard && session.ticksAlive % 40 != 0) {
            return;
        }
        float amount = hazard ? session.pulseDamage : session.pulseDamage * 0.35f;
        session.illusoryHp = Math.max(0f, session.illusoryHp - amount);
        PacketDistributor.sendToPlayer(
                victim,
                new ModNetworking.MirageHurtPayload(amount, session.illusoryHp, session.illusoryMaxHp));
    }

    private static boolean isHazard(BlockState state) {
        return state != null
                && (state.is(Blocks.MAGMA_BLOCK)
                        || state.is(Blocks.SOUL_FIRE)
                        || state.is(Blocks.FIRE)
                        || state.is(Blocks.LAVA));
    }

    private static void buildChamber(ServerLevel level, Session session, RandomSource random) {
        BlockPos origin = session.origin;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -1; dy <= HEIGHT; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    boolean shell = Math.abs(dx) == RADIUS
                            || Math.abs(dz) == RADIUS
                            || dy == -1
                            || dy == HEIGHT;
                    boolean pillar = (Math.abs(dx) == 2 && Math.abs(dz) == 2) && dy >= 0 && dy < HEIGHT;
                    BlockState real = level.getBlockState(pos);

                    if (shell) {
                        putFake(session, pos, pickShell(dy, dx, dz, random));
                    } else if (pillar) {
                        putFake(session, pos, Blocks.PURPUR_PILLAR.defaultBlockState());
                    } else if (!real.isAir() && !real.canBeReplaced()) {
                        putFake(session, pos, pickRetexture(random));
                    } else if (dy == 0 && dx == 0 && dz == 0) {
                        putFake(session, pos, Blocks.AIR.defaultBlockState());
                        putFake(session, pos.above(), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        putFake(session, origin.offset(RADIUS - 1, HEIGHT, RADIUS - 1), Blocks.SHROOMLIGHT.defaultBlockState());
        putFake(session, origin.offset(-(RADIUS - 1), HEIGHT, RADIUS - 1), Blocks.SHROOMLIGHT.defaultBlockState());
        putFake(session, origin.offset(RADIUS - 1, HEIGHT, -(RADIUS - 1)), Blocks.SHROOMLIGHT.defaultBlockState());
        putFake(session, origin.offset(-(RADIUS - 1), HEIGHT, -(RADIUS - 1)), Blocks.SHROOMLIGHT.defaultBlockState());
    }

    private static BlockState pickShell(int dy, int dx, int dz, RandomSource random) {
        if (dy == -1) {
            // Safe pad under spawn — no instant magma under the soul.
            if (dx == 0 && dz == 0) {
                return Blocks.AMETHYST_BLOCK.defaultBlockState();
            }
            if (((dx + dz) & 3) == 0) {
                return Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            return random.nextBoolean()
                    ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                    : Blocks.AMETHYST_BLOCK.defaultBlockState();
        }
        if (dy == HEIGHT) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (random.nextFloat() < 0.18f) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        if (random.nextFloat() < 0.22f) {
            return Blocks.SCULK.defaultBlockState();
        }
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    private static BlockState pickRetexture(RandomSource random) {
        float r = random.nextFloat();
        if (r < 0.35f) {
            return Blocks.SCULK.defaultBlockState();
        }
        if (r < 0.6f) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        if (r < 0.8f) {
            return Blocks.PURPUR_BLOCK.defaultBlockState();
        }
        return Blocks.BLACKSTONE.defaultBlockState();
    }

    private static void putFake(Session session, BlockPos pos, BlockState state) {
        session.fakes.put(pos.immutable(), state);
    }

    private static void pushAll(ServerPlayer victim, Session session) {
        for (Map.Entry<BlockPos, BlockState> e : session.fakes.entrySet()) {
            victim.connection.send(new ClientboundBlockUpdatePacket(e.getKey(), e.getValue()));
        }
    }

    private static void restoreBlocks(ServerPlayer victim, Session session) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : session.fakes.keySet()) {
            victim.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        }
    }

    private static void applyPhysics(ServerPlayer player, Session session) {
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.02, player.getZ());
        BlockState floor = session.fakes.get(below.immutable());
        if (floor != null && blocksMotion(floor)) {
            double top = below.getY() + 1.0;
            if (player.getY() >= top - 0.65 && player.getY() <= top + 0.08 && player.getDeltaMovement().y <= 0.08) {
                player.setPos(player.getX(), top, player.getZ());
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.x, Math.max(0, vel.y), vel.z);
                player.setOnGround(true);
                player.resetFallDistance();
            }
        }

        AABB box = player.getBoundingBox();
        BlockPos min = BlockPos.containing(box.minX - 0.05, box.minY - 0.05, box.minZ - 0.05);
        BlockPos max = BlockPos.containing(box.maxX + 0.05, box.maxY + 0.05, box.maxZ + 0.05);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockState fake = session.fakes.get(cursor.immutable());
            if (fake == null || !blocksMotion(fake)) {
                continue;
            }
            resolvePenetration(player, cursor);
        }
    }

    private static boolean blocksMotion(BlockState state) {
        return state.blocksMotion();
    }

    private static void resolvePenetration(ServerPlayer player, BlockPos pos) {
        AABB playerBox = player.getBoundingBox();
        AABB blockBox = new AABB(pos);
        if (!playerBox.intersects(blockBox)) {
            return;
        }
        double overlapX = Math.min(playerBox.maxX - blockBox.minX, blockBox.maxX - playerBox.minX);
        double overlapY = Math.min(playerBox.maxY - blockBox.minY, blockBox.maxY - playerBox.minY);
        double overlapZ = Math.min(playerBox.maxZ - blockBox.minZ, blockBox.maxZ - playerBox.minZ);
        if (overlapX <= 0 || overlapY <= 0 || overlapZ <= 0) {
            return;
        }

        double cx = playerBox.getCenter().x;
        double cy = playerBox.getCenter().y;
        double cz = playerBox.getCenter().z;
        double bx = blockBox.getCenter().x;
        double by = blockBox.getCenter().y;
        double bz = blockBox.getCenter().z;

        if (overlapY <= overlapX && overlapY <= overlapZ) {
            if (cy >= by) {
                player.setPos(player.getX(), blockBox.maxY, player.getZ());
                player.setOnGround(true);
                player.resetFallDistance();
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.x, Math.max(0, vel.y), vel.z);
            } else {
                player.setPos(player.getX(), blockBox.minY - player.getBbHeight(), player.getZ());
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.x, Math.min(0, vel.y), vel.z);
            }
        } else if (overlapX <= overlapZ) {
            double dir = cx >= bx ? 1.0 : -1.0;
            player.setPos(player.getX() + dir * overlapX, player.getY(), player.getZ());
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(0, vel.y, vel.z);
        } else {
            double dir = cz >= bz ? 1.0 : -1.0;
            player.setPos(player.getX(), player.getY(), player.getZ() + dir * overlapZ);
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(vel.x, vel.y, 0);
        }
        player.hasImpulse = true;
    }

    private static final class Session {
        final UUID victimId;
        final UUID casterId;
        final long endTick;
        float illusoryHp;
        final float illusoryMaxHp;
        final float pulseDamage;
        final BlockPos origin;
        final Vec3 bodyPos;
        final float bodyYRot;
        final float bodyXRot;
        final Map<BlockPos, BlockState> fakes = new HashMap<>();
        UUID bodyId;
        int ticksAlive;

        Session(
                UUID victimId,
                UUID casterId,
                long endTick,
                float illusoryHp,
                float illusoryMaxHp,
                float pulseDamage,
                BlockPos origin,
                Vec3 bodyPos,
                float bodyYRot,
                float bodyXRot) {
            this.victimId = victimId;
            this.casterId = casterId;
            this.endTick = endTick;
            this.illusoryHp = illusoryHp;
            this.illusoryMaxHp = illusoryMaxHp;
            this.pulseDamage = pulseDamage;
            this.origin = origin;
            this.bodyPos = bodyPos;
            this.bodyYRot = bodyYRot;
            this.bodyXRot = bodyXRot;
        }
    }
}
