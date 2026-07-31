package com.effecoria.effect.corruption;

import com.effecoria.EffecoriaMod;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.formula.SpellCombat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies contagious corruption curses, soft DoT, and individual cure via blocks/items.
 */
public final class CorruptionCurseService {
    public static final String CURSE_TAG = "effecoria:corruption_curse";
    public static final String GOAL_TAG = "effecoria:corruption_cure_goal";

    public static final TagKey<Block> CURE_BLOCKS_COMMON =
            TagKey.create(Registries.BLOCK, EffecoriaMod.id("corruption_cure_common"));
    public static final TagKey<Block> CURE_BLOCKS_RARE =
            TagKey.create(Registries.BLOCK, EffecoriaMod.id("corruption_cure_rare"));
    public static final TagKey<Item> CURE_ITEMS_COMMON =
            TagKey.create(Registries.ITEM, EffecoriaMod.id("corruption_cure_common"));
    public static final TagKey<Item> CURE_ITEMS_RARE =
            TagKey.create(Registries.ITEM, EffecoriaMod.id("corruption_cure_rare"));

    private CorruptionCurseService() {}

    public static boolean hasCurse(LivingEntity entity) {
        return entity.getPersistentData().contains(CURSE_TAG, Tag.TAG_COMPOUND);
    }

    public static CorruptionCurse.CureTier cureTier(LivingEntity entity) {
        if (!hasCurse(entity)) {
            return CorruptionCurse.CureTier.COMMON;
        }
        return CorruptionCurse.CureTier.fromString(entity.getPersistentData().getCompound(CURSE_TAG).getString("CureTier"));
    }

    public static float softDot(LivingEntity entity) {
        if (!hasCurse(entity)) {
            return 0f;
        }
        return entity.getPersistentData().getCompound(CURSE_TAG).getFloat("SoftDot");
    }

    /**
     * @param allowSpread if true and curse has contagion, copy to same EntityType peers (no cascade)
     * @return peers cursed by contagion (not including primary)
     */
    public static int apply(ServerPlayer caster, LivingEntity target, CorruptionCurse curse, boolean allowSpread) {
        if (target == null || !target.isAlive() || curse == null) {
            return 0;
        }
        // Players get effects only — no contagion metadata / seek AI.
        if (target instanceof Player) {
            applyEffects(caster, target, curse);
            return 0;
        }

        writeCurse(target, curse, caster);
        applyEffects(caster, target, curse);
        enablePickupIfNeeded(target);
        ensureSeekGoal(target);

        int spread = 0;
        if (allowSpread && curse.contagionChunks() > 0 && caster.level() instanceof ServerLevel level) {
            spread = spreadToKin(caster, level, target, curse);
        }
        return spread;
    }

    private static int spreadToKin(
            ServerPlayer caster, ServerLevel level, LivingEntity primary, CorruptionCurse curse) {
        EntityType<?> type = primary.getType();
        double range = curse.contagionRangeBlocks();
        AABB box = primary.getBoundingBox().inflate(range);
        int count = 0;
        for (LivingEntity peer : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != primary)) {
            if (peer instanceof Player) {
                continue;
            }
            if (peer.getType() != type) {
                continue;
            }
            if (peer.distanceToSqr(primary) > range * range) {
                continue;
            }
            apply(caster, peer, curse, false);
            CorruptionEffects.spawnCorruptionParticles(level, peer.position().add(0, 1, 0));
            count++;
        }
        if (count > 0) {
            caster.displayClientMessage(Component.translatable("message.effecoria.corruption.spread", count), true);
        }
        return count;
    }

    private static void applyEffects(ServerPlayer caster, LivingEntity target, CorruptionCurse curse) {
        for (CorruptionCurse.EffectSpec spec : curse.effects()) {
            BreathDebuffs.apply(caster, target, spec.toInstance());
        }
    }

    private static void writeCurse(LivingEntity target, CorruptionCurse curse, ServerPlayer caster) {
        CompoundTag root = target.getPersistentData();
        CompoundTag tag = hasCurse(target) ? root.getCompound(CURSE_TAG).copy() : new CompoundTag();
        tag.putString("Id", curse.id());
        tag.putUUID("Caster", curse.casterId() != null ? curse.casterId() : caster.getUUID());

        CorruptionCurse.CureTier priorTier = CorruptionCurse.CureTier.fromString(tag.getString("CureTier"));
        CorruptionCurse.CureTier nextTier = curse.cureTier();
        if (priorTier == CorruptionCurse.CureTier.RARE || nextTier == CorruptionCurse.CureTier.RARE) {
            tag.putString("CureTier", CorruptionCurse.CureTier.RARE.id());
        } else {
            tag.putString("CureTier", nextTier.id());
        }
        tag.putFloat("SoftDot", Math.max(tag.getFloat("SoftDot"), curse.softDotPerSecond()));
        tag.putInt("ContagionChunks", Math.max(tag.getInt("ContagionChunks"), curse.contagionChunks()));

        ListTag effectIds = tag.contains("Effects", Tag.TAG_LIST)
                ? tag.getList("Effects", Tag.TAG_STRING)
                : new ListTag();
        java.util.HashSet<String> known = new java.util.HashSet<>();
        for (int i = 0; i < effectIds.size(); i++) {
            known.add(effectIds.getString(i));
        }
        boolean anyPermanent = false;
        int maxDuration = 0;
        for (CorruptionCurse.EffectSpec spec : curse.effects()) {
            if (spec.permanent()) {
                anyPermanent = true;
            } else {
                maxDuration = Math.max(maxDuration, spec.durationTicks());
            }
            ResourceLocation key = spec.effect().unwrapKey().map(k -> k.location()).orElse(null);
            if (key != null && known.add(key.toString())) {
                effectIds.add(StringTag.valueOf(key.toString()));
            }
        }
        tag.put("Effects", effectIds);

        if (anyPermanent || curse.softDotPerSecond() > 0f || tag.getFloat("SoftDot") > 0f) {
            tag.remove("ExpireAt");
        } else if (maxDuration > 0 && target.level() instanceof ServerLevel level) {
            long expireAt = level.getGameTime() + maxDuration;
            if (!tag.contains("ExpireAt") || tag.getLong("ExpireAt") < expireAt) {
                tag.putLong("ExpireAt", expireAt);
            }
        }
        root.put(CURSE_TAG, tag);
    }

    private static void enablePickupIfNeeded(LivingEntity target) {
        if (!(target instanceof Mob mob)) {
            return;
        }
        CompoundTag tag = mob.getPersistentData().getCompound(CURSE_TAG);
        if (!mob.canPickUpLoot()) {
            mob.setCanPickUpLoot(true);
            tag.putBoolean("EnabledPickup", true);
            mob.getPersistentData().put(CURSE_TAG, tag);
        }
    }

    public static void ensureSeekGoal(LivingEntity target) {
        if (!(target instanceof net.minecraft.world.entity.PathfinderMob pathMob)) {
            return;
        }
        CompoundTag root = pathMob.getPersistentData();
        if (root.getBoolean(GOAL_TAG)) {
            return;
        }
        pathMob.goalSelector.addGoal(5, new SeekCorruptionCureGoal(pathMob));
        root.putBoolean(GOAL_TAG, true);
    }

    public static void clear(LivingEntity entity) {
        if (!hasCurse(entity)) {
            return;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(CURSE_TAG);
        if (entity.level() instanceof ServerLevel level) {
            var effects = level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
            if (tag.contains("Effects", Tag.TAG_LIST)) {
                ListTag list = tag.getList("Effects", Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                    if (id == null) {
                        continue;
                    }
                    effects.getHolder(id).ifPresent(holder -> entity.removeEffect(holder));
                }
            }
        }
        if (entity instanceof Mob mob && tag.getBoolean("EnabledPickup")) {
            mob.setCanPickUpLoot(false);
        }
        entity.getPersistentData().remove(CURSE_TAG);
        if (entity.level() instanceof ServerLevel level) {
            CorruptionEffects.spawnCorruptionParticles(level, entity.position().add(0, 1, 0));
        }
    }

    public static void tickEntity(LivingEntity entity) {
        if (!hasCurse(entity) || entity.level().isClientSide()) {
            return;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(CURSE_TAG);
        if (tag.contains("ExpireAt")
                && entity.level() instanceof ServerLevel level
                && level.getGameTime() >= tag.getLong("ExpireAt")) {
            if (entity instanceof Mob mob && tag.getBoolean("EnabledPickup")) {
                mob.setCanPickUpLoot(false);
            }
            entity.getPersistentData().remove(CURSE_TAG);
            return;
        }
        if (tryCure(entity)) {
            return;
        }
        float dot = softDot(entity);
        if (dot > 0f && entity.tickCount % 20 == 0) {
            ServerPlayer caster = null;
            if (tag.hasUUID("Caster") && entity.level() instanceof ServerLevel level) {
                caster = level.getServer().getPlayerList().getPlayer(tag.getUUID("Caster"));
            }
            if (caster != null) {
                SpellCombat.hurtMagic(caster, entity, dot);
            } else {
                entity.hurt(entity.level().damageSources().magic(), dot);
            }
        }
    }

    public static boolean tryCure(LivingEntity entity) {
        if (!hasCurse(entity)) {
            return false;
        }
        CorruptionCurse.CureTier tier = cureTier(entity);
        if (isStandingOnCure(entity, tier) || isHoldingCureItem(entity, tier)) {
            clear(entity);
            if (entity.level() instanceof ServerLevel level) {
                level.playSound(
                        null,
                        entity.blockPosition(),
                        net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE,
                        net.minecraft.sounds.SoundSource.HOSTILE,
                        0.5f,
                        1.4f);
            }
            return true;
        }
        return false;
    }

    public static boolean isStandingOnCure(LivingEntity entity, CorruptionCurse.CureTier tier) {
        BlockPos feet = entity.blockPosition();
        BlockPos below = feet.below();
        BlockState belowState = entity.level().getBlockState(below);
        BlockState inState = entity.level().getBlockState(feet);
        if (matchesCureBlock(belowState, tier) || matchesCureBlock(inState, tier)) {
            return true;
        }
        // Common tier: standing in water also cleanses.
        if (tier == CorruptionCurse.CureTier.COMMON && (entity.isInWater() || inState.is(Blocks.WATER))) {
            return true;
        }
        return false;
    }

    public static boolean matchesCureBlock(BlockState state, CorruptionCurse.CureTier tier) {
        if (tier == CorruptionCurse.CureTier.RARE) {
            return state.is(CURE_BLOCKS_RARE);
        }
        return state.is(CURE_BLOCKS_COMMON) || state.is(CURE_BLOCKS_RARE);
    }

    public static boolean matchesCureItem(ItemStack stack, CorruptionCurse.CureTier tier) {
        if (stack.isEmpty()) {
            return false;
        }
        if (tier == CorruptionCurse.CureTier.RARE) {
            return stack.is(CURE_ITEMS_RARE);
        }
        return stack.is(CURE_ITEMS_COMMON) || stack.is(CURE_ITEMS_RARE);
    }

    private static boolean isHoldingCureItem(LivingEntity entity, CorruptionCurse.CureTier tier) {
        if (matchesCureItem(entity.getMainHandItem(), tier) || matchesCureItem(entity.getOffhandItem(), tier)) {
            return true;
        }
        if (entity instanceof Mob mob) {
            for (ItemStack stack : mob.getAllSlots()) {
                if (matchesCureItem(stack, tier)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Used by seek goal — blocks within sight cube. */
    public static List<BlockPos> findVisibleCureBlocks(Mob mob, int range) {
        List<BlockPos> found = new ArrayList<>();
        CorruptionCurse.CureTier tier = cureTier(mob);
        BlockPos origin = mob.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = mob.level().getBlockState(cursor);
                    boolean waterOk = tier == CorruptionCurse.CureTier.COMMON && state.is(Blocks.WATER);
                    if (!matchesCureBlock(state, tier) && !waterOk) {
                        continue;
                    }
                    if (canSeeBlock(mob, cursor)) {
                        found.add(cursor.immutable());
                    }
                }
            }
        }
        return found;
    }

    public static boolean canSeeBlock(Mob mob, BlockPos pos) {
        var from = mob.getEyePosition();
        var to = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        var hit = mob.level()
                .clip(new net.minecraft.world.level.ClipContext(
                        from,
                        to,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        mob));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                || hit.getBlockPos().equals(pos);
    }

    public static ItemEntity findVisibleCureItem(Mob mob, int range) {
        CorruptionCurse.CureTier tier = cureTier(mob);
        AABB box = mob.getBoundingBox().inflate(range);
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : mob.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (!matchesCureItem(item.getItem(), tier)) {
                continue;
            }
            if (!mob.hasLineOfSight(item)) {
                continue;
            }
            double d = mob.distanceToSqr(item);
            if (d < bestDist) {
                bestDist = d;
                best = item;
            }
        }
        return best;
    }
}
