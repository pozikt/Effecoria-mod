package com.effecoria.effect.necromancy;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Death Mark: tag a mob; on its death leave a clickable mark; necromancer raises it as a thrall.
 * Raised thralls reserve Ψ equal to the mob's max health.
 */
public final class DeathMarkService {
    public static final String LIVING_OWNER_TAG = "effecoria:death_mark_owner";
    public static final String LIVING_UNTIL_TAG = "effecoria:death_mark_until";

    public static final String MARK_TAG = "effecoria:death_mark";
    public static final String MARK_OWNER_TAG = "effecoria:death_mark_owner";
    public static final String MARK_TYPE_TAG = "effecoria:death_mark_type";
    public static final String MARK_HP_TAG = "effecoria:death_mark_hp";
    public static final String MARK_EXPIRE_TAG = "effecoria:death_mark_expire";

    /** How long the living mark lasts (ticks). */
    public static final int LIVING_MARK_TICKS = 20 * 90;
    /** How long the world mark persists after death (ticks). */
    public static final int WORLD_MARK_TICKS = 20 * 60 * 5;

    private DeathMarkService() {}

    public static void applyMark(ServerPlayer caster, LivingEntity target, int durationTicks) {
        if (target == null) {
            return;
        }
        if (!(target instanceof Mob) || target instanceof Player) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.death_mark_invalid"), true);
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = durationTicks > 0 ? durationTicks : LIVING_MARK_TICKS;
        CompoundTag data = target.getPersistentData();
        data.putUUID(LIVING_OWNER_TAG, caster.getUUID());
        data.putLong(LIVING_UNTIL_TAG, level.getGameTime() + duration);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, true, true));
        NecromancyEffects.spawnNecroParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.85f, 0.55f);
    }

    public static boolean hasActiveLivingMark(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.hasUUID(LIVING_OWNER_TAG) || !data.contains(LIVING_UNTIL_TAG)) {
            return false;
        }
        return entity.level().getGameTime() <= data.getLong(LIVING_UNTIL_TAG);
    }

    public static void onMarkedDeath(LivingEntity dead) {
        if (!(dead.level() instanceof ServerLevel level) || !hasActiveLivingMark(dead)) {
            return;
        }
        if (!(dead instanceof Mob) || dead instanceof Player) {
            return;
        }
        CompoundTag data = dead.getPersistentData();
        java.util.UUID ownerId = data.getUUID(LIVING_OWNER_TAG);
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (typeId == null) {
            return;
        }
        spawnWorldMark(level, dead.position(), ownerId, typeId, dead.getMaxHealth());
    }

    public static void spawnWorldMark(
            ServerLevel level, Vec3 pos, java.util.UUID ownerId, ResourceLocation typeId, float maxHealth) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        if (stand == null) {
            return;
        }
        float hp = Math.max(1f, maxHealth);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(typeId);
        Component typeName = type != null ? type.getDescription() : Component.literal(typeId.getPath());

        stand.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setGlowingTag(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(Component.translatable("entity.effecoria.death_mark", typeName, (int) Math.ceil(hp)));
        stand.setShowArms(false);
        stand.setNoBasePlate(true);

        CompoundTag tag = stand.getPersistentData();
        tag.putBoolean(MARK_TAG, true);
        tag.putUUID(MARK_OWNER_TAG, ownerId);
        tag.putString(MARK_TYPE_TAG, typeId.toString());
        tag.putFloat(MARK_HP_TAG, hp);
        tag.putLong(MARK_EXPIRE_TAG, level.getGameTime() + WORLD_MARK_TICKS);

        level.addFreshEntity(stand);
        NecromancyEffects.spawnNecroParticles(level, pos.add(0, 0.5, 0));
        level.playSound(null, stand.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    public static boolean isWorldMark(Entity entity) {
        return entity instanceof ArmorStand && entity.getPersistentData().getBoolean(MARK_TAG);
    }

    public static boolean tryRaise(ServerPlayer player, Entity clicked) {
        if (!isWorldMark(clicked) || !(clicked.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag tag = clicked.getPersistentData();
        if (!tag.hasUUID(MARK_OWNER_TAG) || !player.getUUID().equals(tag.getUUID(MARK_OWNER_TAG))) {
            player.displayClientMessage(Component.translatable("message.effecoria.necro.death_mark_not_yours"), true);
            return true;
        }
        if (level.getGameTime() > tag.getLong(MARK_EXPIRE_TAG)) {
            clicked.discard();
            return true;
        }

        float reserve = tag.getFloat(MARK_HP_TAG);
        ResourceLocation typeId = ResourceLocation.parse(tag.getString(MARK_TYPE_TAG));
        if (!NecroSummonService.canAfford(player, reserve)) {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.necro.summon_psi_reserve", (int) Math.ceil(reserve)),
                    true);
            return true;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(typeId);
        if (type == null) {
            clicked.discard();
            return true;
        }
        Entity created = type.create(level);
        if (!(created instanceof Mob mob) || created instanceof Player) {
            player.displayClientMessage(Component.translatable("message.effecoria.necro.death_mark_invalid"), true);
            clicked.discard();
            return true;
        }

        mob.moveTo(clicked.getX(), clicked.getY(), clicked.getZ(), player.getYRot(), 0f);
        mob.setPersistenceRequired();
        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(reserve);
            mob.setHealth(reserve);
        }
        level.addFreshEntity(mob);
        if (!NecroSummonService.register(mob, player, null, reserve)) {
            mob.discard();
            return true;
        }

        clicked.discard();
        NecromancyEffects.spawnNecroParticles(level, mob.position().add(0, 1, 0));
        level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1f, 0.75f);
        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.necro.death_mark_raised",
                        mob.getDisplayName(),
                        (int) Math.ceil(reserve)),
                true);
        return true;
    }

    public static void tickMarks(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            AABB box = player.getBoundingBox().inflate(48);
            for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box, DeathMarkService::isWorldMark)) {
                CompoundTag tag = stand.getPersistentData();
                if (level.getGameTime() > tag.getLong(MARK_EXPIRE_TAG)) {
                    stand.discard();
                    continue;
                }
                Vec3 p = stand.position().add(0, 0.6, 0);
                level.sendParticles(ModParticleTypes.NECRO_SHADOW.get(), p.x, p.y, p.z, 2, 0.25, 0.35, 0.25, 0.01);
                level.sendParticles(ParticleTypes.SOUL, p.x, p.y + 0.2, p.z, 1, 0.15, 0.2, 0.15, 0.01);
            }
        }
    }

    /** Sync reserved Ψ onto networked player data for HUD. */
    public static void syncReservedPsi(ServerPlayer owner) {
        PlayerPsiData data = PsiHelper.get(owner);
        float reserved = NecroSummonService.reservedPsi(owner);
        if (Math.abs(data.necroReservedPsi() - reserved) > 0.05f) {
            data.setNecroReservedPsi(reserved);
            PsiHelper.set(owner, data);
        }
    }
}
