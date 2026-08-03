package com.effecoria.effect.mental;

import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.entity.MirageHorrorEntity;
import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streaming bone-and-blood mirage plains (client-only blocks).
 * Chunks generate ahead of the soul as they explore; distant chunks unload back to reality.
 */
public final class MirageWorldService {
    public static final String BODY_TAG = "effecoria:mirage_body";

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final int CHUNK = 8;
    /** Chunks kept generated around the soul (Chebyshev). */
    private static final int VIEW_RANGE = 3;
    /** Chunks beyond this are restored to the real world. */
    private static final int UNLOAD_RANGE = 5;
    private static final int CHUNKS_PER_TICK = 2;
    private static final int AIR_CLEAR = 14;
    private static final int PUSH_PER_TICK = 260;
    private static final int RESYNC_INTERVAL = 80;
    private static final int SPEAR_MIN_INTERVAL = 45;
    private static final int SPEAR_MAX_INTERVAL = 95;
    private static final int SPEAR_LENGTH_MIN = 22;
    private static final int SPEAR_LENGTH_MAX = 36;

    private MirageWorldService() {}

    public static void start(ServerPlayer victim, ServerPlayer caster, int durationTicks, float pulseDamage) {
        endQuiet(victim);
        float maxHp = Math.max(12f, victim.getMaxHealth() * 1.25f);
        BlockPos origin = victim.blockPosition().immutable();
        int seed = Long.hashCode(victim.getUUID().getMostSignificantBits()
                ^ (victim.level().getGameTime() * 31L)
                ^ caster.getUUID().getLeastSignificantBits());

        Session session = new Session(
                victim.getUUID(),
                caster.getUUID(),
                victim.level().getGameTime() + Math.max(120, durationTicks),
                maxHp,
                maxHp,
                Mth.clamp(pulseDamage, 0.5f, 6f),
                origin,
                victim.position(),
                victim.getYRot(),
                victim.getXRot(),
                seed);
        // Seed a small landing ring; the rest streams as the soul walks.
        queueAround(session, origin.getX(), origin.getZ(), 1);
        flushPendingChunks(session, 9);
        queueAround(session, origin.getX(), origin.getZ(), VIEW_RANGE);
        session.bodyId = spawnBodyShell(victim, session);
        enterSoul(victim);
        BreathDebuffs.apply(victim, new MobEffectInstance(MobEffects.CONFUSION, 80, 1, false, false, true));
        SESSIONS.put(victim.getUUID(), session);
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
        return s != null && visibleState(s, pos.immutable()) != null;
    }

    public static void resend(ServerPlayer player, BlockPos pos) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) {
            return;
        }
        BlockState fake = visibleState(s, pos.immutable());
        if (fake != null) {
            player.connection.send(new ClientboundBlockUpdatePacket(pos, fake));
        }
    }

    public static void playerTick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || player.level().getGameTime() >= session.endTick) {
            return;
        }
        player.noPhysics = true;
        player.setInvisible(true);

        streamChunks(player, session);
        drainPushQueue(player, session);
        applyPhysics(player, session);
        keepBodyShell(level, session);
        if (session.ticksAlive % 35 == 0) {
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.CONFUSION, 70, 1, false, false, true));
        }
        tickSpears(player, session);
        tickHorror(player, session);
        session.ticksAlive++;
        if (session.ticksAlive % RESYNC_INTERVAL == 0 && session.pushQueue.isEmpty()) {
            enqueueNearby(session, player.blockPosition().getX(), player.blockPosition().getZ());
        }
    }

    /** Illusory moral damage from spears / horror — never real HP. */
    public static void applyMoralDamage(ServerPlayer victim, float amount) {
        Session session = SESSIONS.get(victim.getUUID());
        if (session == null) {
            return;
        }
        session.illusoryHp = Math.max(0f, session.illusoryHp - amount);
        PacketDistributor.sendToPlayer(
                victim,
                new ModNetworking.MirageHurtPayload(amount, session.illusoryHp, session.illusoryMaxHp));
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
        discardHorror(victim.serverLevel(), session);
        if (victim.level() instanceof ServerLevel level) {
            for (BlockPos pos : session.overlays.keySet()) {
                victim.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
            }
            for (BlockPos pos : session.terrain.keySet()) {
                victim.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
            }
        }
        discardBody(victim.serverLevel(), session);
        exitSoul(victim);
        victim.removeEffect(MobEffects.CONFUSION);
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

    private static void discardHorror(ServerLevel level, Session session) {
        if (session.horrorId == null) {
            return;
        }
        var entity = level.getEntity(session.horrorId);
        if (entity != null) {
            entity.discard();
        }
        session.horrorId = null;
    }

    private static void tickHorror(ServerPlayer victim, Session session) {
        if (session.horrorId != null) {
            var existing = victim.serverLevel().getEntity(session.horrorId);
            if (existing == null || !existing.isAlive()) {
                session.horrorId = null;
            } else {
                return;
            }
        }
        if (session.nextHorrorAt < 0) {
            session.nextHorrorAt = session.ticksAlive + 70 + victim.getRandom().nextInt(50);
        }
        if (session.ticksAlive < session.nextHorrorAt) {
            return;
        }
        // Appear ahead of the soul's gaze — always in view for fear impact.
        Vec3 look = victim.getLookAngle().normalize();
        double dist = 9.0 + victim.getRandom().nextDouble() * 5.0;
        double x = victim.getX() + look.x * dist + (victim.getRandom().nextDouble() - 0.5) * 3.0;
        double z = victim.getZ() + look.z * dist + (victim.getRandom().nextDouble() - 0.5) * 3.0;
        double y = session.origin.getY();
        MirageHorrorEntity horror = MirageHorrorEntity.spawnFor(victim, x, y, z);
        if (horror != null) {
            session.horrorId = horror.getUUID();
            session.nextHorrorAt = session.ticksAlive + 160 + victim.getRandom().nextInt(80);
        }
    }

    // --- Streaming chunks --------------------------------------------------

    private static void streamChunks(ServerPlayer player, Session session) {
        int px = player.blockPosition().getX();
        int pz = player.blockPosition().getZ();
        queueAround(session, px, pz, VIEW_RANGE);
        flushPendingChunks(session, CHUNKS_PER_TICK);
        unloadFarChunks(player, session, px, pz);
    }

    private static void queueAround(Session session, int worldX, int worldZ, int range) {
        int pcx = Math.floorDiv(worldX, CHUNK);
        int pcz = Math.floorDiv(worldZ, CHUNK);
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                long key = chunkKey(pcx + dx, pcz + dz);
                if (!session.generatedChunks.contains(key)) {
                    session.pendingChunks.add(key);
                }
            }
        }
    }

    private static void flushPendingChunks(Session session, int budget) {
        int done = 0;
        while (done < budget && !session.pendingChunks.isEmpty()) {
            long best = Long.MIN_VALUE;
            int bestDist = Integer.MAX_VALUE;
            for (long key : session.pendingChunks) {
                int cx = unpackCx(key);
                int cz = unpackCz(key);
                int dist = Math.max(Math.abs(cx - session.lastFocusCx), Math.abs(cz - session.lastFocusCz));
                if (dist < bestDist) {
                    bestDist = dist;
                    best = key;
                }
            }
            if (best == Long.MIN_VALUE) {
                break;
            }
            session.pendingChunks.remove(best);
            if (session.generatedChunks.contains(best)) {
                continue;
            }
            generateChunk(session, unpackCx(best), unpackCz(best));
            done++;
        }
    }

    private static void unloadFarChunks(ServerPlayer player, Session session, int worldX, int worldZ) {
        int pcx = Math.floorDiv(worldX, CHUNK);
        int pcz = Math.floorDiv(worldZ, CHUNK);
        session.lastFocusCx = pcx;
        session.lastFocusCz = pcz;
        List<Long> toUnload = new ArrayList<>();
        for (long key : session.generatedChunks) {
            int cx = unpackCx(key);
            int cz = unpackCz(key);
            if (Math.max(Math.abs(cx - pcx), Math.abs(cz - pcz)) > UNLOAD_RANGE) {
                toUnload.add(key);
            }
        }
        for (long key : toUnload) {
            unloadChunk(player, session, key);
        }
    }

    private static void unloadChunk(ServerPlayer player, Session session, long key) {
        List<BlockPos> positions = session.chunkBlocks.remove(key);
        session.generatedChunks.remove(key);
        session.pendingChunks.remove(key);
        if (positions == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : positions) {
            session.terrain.remove(pos);
            session.overlays.remove(pos);
            player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        }
    }

    private static void generateChunk(Session session, int cx, int cz) {
        long key = chunkKey(cx, cz);
        if (!session.generatedChunks.add(key)) {
            return;
        }
        session.buildingChunk = key;
        session.chunkBlocks.put(key, new ArrayList<>());

        int baseY = session.origin.getY() - 1;
        int minX = cx * CHUNK;
        int minZ = cz * CHUNK;
        RandomSource random = RandomSource.create(session.seed * 734287L ^ key);

        for (int lx = 0; lx < CHUNK; lx++) {
            for (int lz = 0; lz < CHUNK; lz++) {
                int wx = minX + lx;
                int wz = minZ + lz;
                generateColumn(session, wx, wz, baseY, random);
            }
        }

        decorateChunk(session, cx, cz, baseY, random);
        session.buildingChunk = Long.MIN_VALUE;

        // Stream the new chunk to the client.
        List<BlockPos> positions = session.chunkBlocks.get(key);
        if (positions != null) {
            for (BlockPos pos : positions) {
                session.pushQueue.addLast(pos);
            }
        }
    }

    private static void generateColumn(Session session, int wx, int wz, int baseY, RandomSource random) {
        int seed = session.seed;
        float blood = fbm(wx / 11f, wz / 11f, seed);
        float river = Math.abs(fbm(wx / 17f, wz / 17f, seed + 19) - 0.5f);
        float bump = fbm(wx / 9f, wz / 9f, seed + 3);
        int groundY = baseY + (bump > 0.72f ? 1 : 0);

        boolean isRiver = river < 0.036f;
        boolean isBloodLake = !isRiver && blood > 0.58f;
        boolean isBloodPuddle = !isRiver && !isBloodLake && blood > 0.50f;

        if (isRiver || isBloodLake || isBloodPuddle) {
            int depth = isBloodLake && blood > 0.78f ? 2 : 1;
            placeBloodWater(session, wx, groundY, wz, depth);
        } else {
            placeBoneLand(session, wx, groundY, wz, random, bump);
        }

        for (int ay = 1; ay <= AIR_CLEAR; ay++) {
            BlockPos air = new BlockPos(wx, groundY + ay, wz);
            BlockState existing = session.terrain.get(air.immutable());
            if (existing == null || existing.getFluidState().isEmpty()) {
                putTerrain(session, air, Blocks.AIR.defaultBlockState());
            }
        }

        // Safe pad at cast origin.
        if (wx == session.origin.getX() && wz == session.origin.getZ()) {
            putTerrain(
                    session,
                    new BlockPos(wx, baseY, wz),
                    Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
            putTerrain(session, new BlockPos(wx, baseY + 1, wz), Blocks.AIR.defaultBlockState());
            putTerrain(session, new BlockPos(wx, baseY + 2, wz), Blocks.AIR.defaultBlockState());
        }
    }

    private static void placeBoneLand(
            Session session, int wx, int groundY, int wz, RandomSource random, float bump) {
        putTerrain(session, new BlockPos(wx, groundY - 1, wz), Blocks.NETHERRACK.defaultBlockState());
        putTerrain(session, new BlockPos(wx, groundY, wz), boneSurface(random, bump));
    }

    private static void decorateChunk(Session session, int cx, int cz, int baseY, RandomSource random) {
        int minX = cx * CHUNK;
        int minZ = cz * CHUNK;

        if (random.nextFloat() < 0.35f) {
            int ox = minX + 1 + random.nextInt(Math.max(1, CHUNK - 5));
            int oz = minZ + 1 + random.nextInt(Math.max(1, CHUNK - 5));
            boolean alongX = random.nextBoolean();
            int span = 3 + random.nextInt(3);
            int height = 4 + random.nextInt(3);
            BlockState boneY = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            BlockState boneBeam = Blocks.BONE_BLOCK.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
            for (int h = 1; h <= height; h++) {
                putTerrain(session, new BlockPos(ox, baseY + h, oz), boneY);
                putTerrain(
                        session,
                        new BlockPos(alongX ? ox + span : ox, baseY + h, alongX ? oz : oz + span),
                        boneY);
            }
            for (int s = 0; s <= span; s++) {
                putTerrain(
                        session,
                        new BlockPos(alongX ? ox + s : ox, baseY + height, alongX ? oz : oz + s),
                        boneBeam);
            }
        }

        int skulls = random.nextInt(3);
        for (int i = 0; i < skulls; i++) {
            int x = minX + random.nextInt(CHUNK);
            int z = minZ + random.nextInt(CHUNK);
            BlockState skull = (random.nextFloat() < 0.35f ? Blocks.WITHER_SKELETON_SKULL : Blocks.SKELETON_SKULL)
                    .defaultBlockState()
                    .setValue(SkullBlock.ROTATION, random.nextInt(16));
            putTerrain(session, new BlockPos(x, baseY + 1, z), skull);
        }

        if (random.nextFloat() < 0.22f) {
            int ox = minX + random.nextInt(CHUNK);
            int oz = minZ + random.nextInt(CHUNK);
            boolean alongX = random.nextBoolean();
            BlockState bone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (int r = 0; r < 5; r++) {
                int h = 2 + (r < 3 ? r : 5 - r);
                int px = alongX ? ox + r : ox;
                int pz = alongX ? oz : oz + r;
                for (int y = 1; y <= h; y++) {
                    putTerrain(session, new BlockPos(px, baseY + y, pz), bone);
                }
            }
        }

        int spurs = 1 + random.nextInt(3);
        for (int i = 0; i < spurs; i++) {
            int x = minX + random.nextInt(CHUNK);
            int z = minZ + random.nextInt(CHUNK);
            int h = 2 + random.nextInt(4);
            BlockState bone = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            for (int y = 1; y <= h; y++) {
                putTerrain(session, new BlockPos(x, baseY + y, z), bone);
            }
        }
    }

    /**
     * Blood water: custom crimson fluid (vanilla water cannot be retinted — NeoForge owns its tint).
     */
    private static void placeBloodWater(Session session, int x, int surfaceY, int z, int depth) {
        int d = Math.max(1, depth);
        putTerrain(session, new BlockPos(x, surfaceY - d, z), bloodBed(session.seed ^ (x * 31) ^ (z * 17)));
        BlockState blood = com.effecoria.content.ModBlocks.BLOOD_FLUID.get().defaultBlockState();
        for (int i = d - 1; i >= 0; i--) {
            putTerrain(session, new BlockPos(x, surfaceY - i, z), blood);
        }
    }

    private static BlockState bloodBed(int salt) {
        float r = hash2(salt, salt * 7, 99);
        if (r < 0.45f) {
            return Blocks.RED_CONCRETE.defaultBlockState();
        }
        if (r < 0.7f) {
            return Blocks.NETHER_WART_BLOCK.defaultBlockState();
        }
        if (r < 0.88f) {
            return Blocks.CRIMSON_NYLIUM.defaultBlockState();
        }
        return Blocks.NETHERRACK.defaultBlockState();
    }

    private static BlockState boneSurface(RandomSource random, float bump) {
        float r = random.nextFloat();
        if (bump > 0.66f && r < 0.55f) {
            return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        if (r < 0.4f) {
            return Blocks.BONE_BLOCK.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);
        }
        if (r < 0.7f) {
            return Blocks.CALCITE.defaultBlockState();
        }
        if (r < 0.88f) {
            return Blocks.WHITE_TERRACOTTA.defaultBlockState();
        }
        return Blocks.SMOOTH_QUARTZ.defaultBlockState();
    }

    private static void putTerrain(Session session, BlockPos pos, BlockState state) {
        BlockPos key = pos.immutable();
        session.terrain.put(key, state);
        if (session.buildingChunk != Long.MIN_VALUE) {
            session.chunkBlocks.computeIfAbsent(session.buildingChunk, k -> new ArrayList<>()).add(key);
        } else {
            // Features may spill one block past chunk edge — attach to column's chunk.
            long ck = chunkKey(Math.floorDiv(key.getX(), CHUNK), Math.floorDiv(key.getZ(), CHUNK));
            session.chunkBlocks.computeIfAbsent(ck, k -> new ArrayList<>()).add(key);
            session.generatedChunks.add(ck);
        }
    }

    private static BlockState visibleState(Session session, BlockPos pos) {
        BlockState overlay = session.overlays.get(pos);
        if (overlay != null) {
            return overlay;
        }
        return session.terrain.get(pos);
    }

    private static void enqueueNearby(Session session, int worldX, int worldZ) {
        int pcx = Math.floorDiv(worldX, CHUNK);
        int pcz = Math.floorDiv(worldZ, CHUNK);
        for (long key : session.generatedChunks) {
            int cx = unpackCx(key);
            int cz = unpackCz(key);
            if (Math.max(Math.abs(cx - pcx), Math.abs(cz - pcz)) > VIEW_RANGE) {
                continue;
            }
            List<BlockPos> positions = session.chunkBlocks.get(key);
            if (positions == null) {
                continue;
            }
            for (BlockPos pos : positions) {
                session.pushQueue.addLast(pos);
            }
        }
        for (BlockPos pos : session.overlays.keySet()) {
            session.pushQueue.addLast(pos);
        }
    }

    private static void drainPushQueue(ServerPlayer victim, Session session) {
        int n = 0;
        while (n < PUSH_PER_TICK && !session.pushQueue.isEmpty()) {
            BlockPos pos = session.pushQueue.removeFirst();
            BlockState state = visibleState(session, pos);
            if (state != null) {
                victim.connection.send(new ClientboundBlockUpdatePacket(pos, state));
            }
            n++;
        }
    }

    private static void sendOne(ServerPlayer victim, Session session, BlockPos pos) {
        BlockState state = visibleState(session, pos);
        if (state != null) {
            victim.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        } else if (victim.level() instanceof ServerLevel level) {
            victim.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        }
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    private static int unpackCx(long key) {
        return (int) (key >> 32);
    }

    private static int unpackCz(long key) {
        return (int) key;
    }

    // --- Light spears ------------------------------------------------------

    private static void tickSpears(ServerPlayer victim, Session session) {
        Iterator<LightSpear> it = session.spears.iterator();
        while (it.hasNext()) {
            LightSpear spear = it.next();
            if (!spear.impacted) {
                for (int i = 0; i < 3 && spear.filled < spear.length; i++) {
                    int y = spear.topY - spear.filled;
                    BlockPos pos = new BlockPos(spear.x, y, spear.z);
                    session.overlays.put(pos.immutable(), spearBlock(spear.filled));
                    sendOne(victim, session, pos);
                    spear.filled++;
                }
                if (spear.filled >= spear.length) {
                    spear.impacted = true;
                    spear.linger = 28;
                    strikeMoral(victim, session, spear);
                }
            } else {
                spear.linger--;
                if (spear.linger <= 0) {
                    for (int i = 0; i < spear.length; i++) {
                        BlockPos pos = new BlockPos(spear.x, spear.topY - i, spear.z).immutable();
                        session.overlays.remove(pos);
                        sendOne(victim, session, pos);
                    }
                    it.remove();
                }
            }
        }

        if (session.nextSpearAt < 0) {
            session.nextSpearAt = session.ticksAlive
                    + SPEAR_MIN_INTERVAL
                    + victim.getRandom().nextInt(SPEAR_MAX_INTERVAL - SPEAR_MIN_INTERVAL + 1);
        }
        if (session.ticksAlive < session.nextSpearAt || !session.spears.isEmpty()) {
            return;
        }
        spawnSpear(victim, session);
        session.nextSpearAt = session.ticksAlive
                + SPEAR_MIN_INTERVAL
                + victim.getRandom().nextInt(SPEAR_MAX_INTERVAL - SPEAR_MIN_INTERVAL + 1);
    }

    private static void spawnSpear(ServerPlayer victim, Session session) {
        RandomSource random = victim.getRandom();
        int tx = victim.blockPosition().getX() + random.nextInt(5) - 2;
        int tz = victim.blockPosition().getZ() + random.nextInt(5) - 2;
        if (random.nextFloat() < 0.55f) {
            tx = victim.blockPosition().getX();
            tz = victim.blockPosition().getZ();
        }
        int length = SPEAR_LENGTH_MIN + random.nextInt(SPEAR_LENGTH_MAX - SPEAR_LENGTH_MIN + 1);
        int groundY = session.origin.getY() - 1;
        int topY = groundY + length;
        session.spears.add(new LightSpear(tx, tz, topY, length));
        victim.displayClientMessage(Component.translatable("message.effecoria.mental.mirage_spear_fall"), true);
        victim.serverLevel().playSound(
                null,
                victim.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS,
                0.45f,
                1.6f);
    }

    private static BlockState spearBlock(int indexFromTop) {
        if (indexFromTop % 5 == 0) {
            return Blocks.OCHRE_FROGLIGHT.defaultBlockState();
        }
        return Blocks.END_ROD.defaultBlockState();
    }

    private static void strikeMoral(ServerPlayer victim, Session session, LightSpear spear) {
        float amount = 7f + session.pulseDamage * 1.8f;
        if (victim.blockPosition().getX() == spear.x && victim.blockPosition().getZ() == spear.z) {
            amount *= 1.65f;
        }
        session.illusoryHp = Math.max(0f, session.illusoryHp - amount);
        BreathDebuffs.apply(victim, new MobEffectInstance(MobEffects.CONFUSION, 100, 2, false, false, true));
        PacketDistributor.sendToPlayer(
                victim,
                new ModNetworking.MirageHurtPayload(amount, session.illusoryHp, session.illusoryMaxHp));
        victim.displayClientMessage(Component.translatable("message.effecoria.mental.mirage_spear_hit"), true);
        victim.serverLevel().playSound(
                null,
                victim.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                0.55f,
                1.35f);
    }

    // --- Physics -----------------------------------------------------------

    private static void applyPhysics(ServerPlayer player, Session session) {
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.02, player.getZ());
        BlockState floor = visibleState(session, below.immutable());
        if (floor != null && supportsStanding(floor)) {
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
            BlockState fake = visibleState(session, cursor.immutable());
            if (fake == null || !fake.blocksMotion()) {
                continue;
            }
            resolvePenetration(player, cursor);
        }
    }

    private static boolean supportsStanding(BlockState state) {
        if (state.blocksMotion()) {
            return true;
        }
        // Walk on mirage water surfaces instead of falling through.
        return !state.getFluidState().isEmpty();
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

    // --- Noise -------------------------------------------------------------

    private static float hash2(int x, int z, int seed) {
        int n = x * 374761393 + z * 668265263 + seed * 1274126177;
        n = (n ^ (n >> 13)) * 1274126177;
        return ((n ^ (n >> 16)) & 0xffff) / 65535f;
    }

    private static float smoothNoise(float x, float z, int seed) {
        int x0 = Mth.floor(x);
        int z0 = Mth.floor(z);
        float fx = x - x0;
        float fz = z - z0;
        fx = fx * fx * (3f - 2f * fx);
        fz = fz * fz * (3f - 2f * fz);
        float a = hash2(x0, z0, seed);
        float b = hash2(x0 + 1, z0, seed);
        float c = hash2(x0, z0 + 1, seed);
        float d = hash2(x0 + 1, z0 + 1, seed);
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    private static float fbm(float x, float z, int seed) {
        return smoothNoise(x, z, seed) * 0.5f
                + smoothNoise(x * 2f, z * 2f, seed + 1) * 0.25f
                + smoothNoise(x * 4f, z * 4f, seed + 2) * 0.125f;
    }

    private static final class LightSpear {
        final int x;
        final int z;
        final int topY;
        final int length;
        int filled;
        boolean impacted;
        int linger;

        LightSpear(int x, int z, int topY, int length) {
            this.x = x;
            this.z = z;
            this.topY = topY;
            this.length = length;
        }
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
        final int seed;
        final Map<BlockPos, BlockState> terrain = new HashMap<>();
        final Map<BlockPos, BlockState> overlays = new HashMap<>();
        final ArrayDeque<BlockPos> pushQueue = new ArrayDeque<>();
        final Set<Long> generatedChunks = new HashSet<>();
        final Set<Long> pendingChunks = new HashSet<>();
        final Map<Long, List<BlockPos>> chunkBlocks = new HashMap<>();
        final List<LightSpear> spears = new ArrayList<>();
        long buildingChunk = Long.MIN_VALUE;
        int lastFocusCx = Integer.MIN_VALUE;
        int lastFocusCz = Integer.MIN_VALUE;
        UUID bodyId;
        UUID horrorId;
        int ticksAlive;
        int nextSpearAt = -1;
        int nextHorrorAt = -1;

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
                float bodyXRot,
                int seed) {
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
            this.seed = seed;
            this.lastFocusCx = Math.floorDiv(origin.getX(), CHUNK);
            this.lastFocusCz = Math.floorDiv(origin.getZ(), CHUNK);
        }
    }
}
