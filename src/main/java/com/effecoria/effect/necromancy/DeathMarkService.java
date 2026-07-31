package com.effecoria.effect.necromancy;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Death Mark: tag a mob; on death leave a clickable mark; necromancer raises a thrall.
 * Gear is snapshotted while alive, parked on the mark's armor-stand slots, then restored on raise.
 */
public final class DeathMarkService {
    public static final String LIVING_OWNER_TAG = "effecoria:death_mark_owner";
    public static final String LIVING_UNTIL_TAG = "effecoria:death_mark_until";
    public static final String LIVING_EQUIP_TAG = "effecoria:death_mark_equip_live";

    public static final String MARK_TAG = "effecoria:death_mark";
    public static final String MARK_OWNER_TAG = "effecoria:death_mark_owner";
    public static final String MARK_TYPE_TAG = "effecoria:death_mark_type";
    public static final String MARK_HP_TAG = "effecoria:death_mark_hp";
    public static final String MARK_EXPIRE_TAG = "effecoria:death_mark_expire";
    public static final String MARK_EQUIP_TAG = "effecoria:death_mark_equip";

    public static final int LIVING_MARK_TICKS = 20 * 90;
    public static final int WORLD_MARK_TICKS = 20 * 60 * 5;

    private static final EquipmentSlot[] GEAR_SLOTS = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private DeathMarkService() {}

    public static void applyMark(ServerPlayer caster, LivingEntity target, int durationTicks) {
        if (target == null) {
            return;
        }
        if (!(target instanceof Mob mob) || target instanceof Player) {
            caster.displayClientMessage(Component.translatable("message.effecoria.necro.death_mark_invalid"), true);
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = durationTicks > 0 ? durationTicks : LIVING_MARK_TICKS;
        CompoundTag data = target.getPersistentData();
        data.putUUID(LIVING_OWNER_TAG, caster.getUUID());
        data.putLong(LIVING_UNTIL_TAG, level.getGameTime() + duration);
        // Snapshot kit while the mob is still alive (bows included).
        data.put(LIVING_EQUIP_TAG, captureEquipment(mob, level.registryAccess()));
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
        if (!(dead instanceof Mob mob) || dead instanceof Player) {
            return;
        }
        CompoundTag data = dead.getPersistentData();
        java.util.UUID ownerId = data.getUUID(LIVING_OWNER_TAG);
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (typeId == null) {
            return;
        }

        CompoundTag fromSlots = captureEquipment(mob, level.registryAccess());
        CompoundTag fromMark = data.contains(LIVING_EQUIP_TAG, Tag.TAG_COMPOUND)
                ? data.getCompound(LIVING_EQUIP_TAG)
                : new CompoundTag();
        CompoundTag equipment = mergeEquipment(fromSlots, fromMark);

        clearEquipment(mob);
        spawnWorldMark(level, dead.position(), ownerId, typeId, dead.getMaxHealth(), equipment);
    }

    public static void spawnWorldMark(
            ServerLevel level,
            Vec3 pos,
            java.util.UUID ownerId,
            ResourceLocation typeId,
            float maxHealth,
            CompoundTag equipment) {
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
        stand.setShowArms(true);
        stand.setNoBasePlate(true);

        // Park gear on the stand itself — survives better than custom NBT alone.
        applyEquipment(stand, equipment, level.registryAccess());

        CompoundTag tag = stand.getPersistentData();
        tag.putBoolean(MARK_TAG, true);
        tag.putUUID(MARK_OWNER_TAG, ownerId);
        tag.putString(MARK_TYPE_TAG, typeId.toString());
        tag.putFloat(MARK_HP_TAG, hp);
        tag.putLong(MARK_EXPIRE_TAG, level.getGameTime() + WORLD_MARK_TICKS);
        if (equipment != null && !equipment.isEmpty()) {
            tag.put(MARK_EQUIP_TAG, equipment.copy());
        }

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
        ArmorStand stand = (ArmorStand) clicked;
        CompoundTag tag = stand.getPersistentData();
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
            NecroSummonService.diagnoseControl(player, reserve).ifPresent(block ->
                    player.displayClientMessage(
                            NecroSummonService.controlMessage(block, player, reserve), true));
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

        mob.moveTo(stand.getX(), stand.getY(), stand.getZ(), player.getYRot(), 0f);
        mob.setPersistenceRequired();
        if (mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(reserve);
            mob.setHealth(reserve);
        }

        CompoundTag fromStandSlots = captureEquipment(stand, level.registryAccess());
        CompoundTag fromNbt = tag.contains(MARK_EQUIP_TAG, Tag.TAG_COMPOUND)
                ? tag.getCompound(MARK_EQUIP_TAG)
                : new CompoundTag();
        CompoundTag equipment = mergeEquipment(fromStandSlots, fromNbt);

        level.addFreshEntity(mob);
        if (!NecroSummonService.register(mob, player, null, reserve)) {
            mob.discard();
            return true;
        }

        // Apply after spawn/register so join-world defaults cannot wipe the kit.
        applyEquipment(mob, equipment, level.registryAccess());
        ensureDefaultWeapon(mob);
        lockEquipment(mob);

        // Empty the mark so gear is not left floating.
        clearEquipment(stand);
        stand.discard();

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
            // Refresh live equipment snapshot on marked mobs still alive near the player.
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box, DeathMarkService::hasActiveLivingMark)) {
                mob.getPersistentData().put(LIVING_EQUIP_TAG, captureEquipment(mob, level.registryAccess()));
            }
        }
    }

    public static void syncReservedPsi(ServerPlayer owner) {
        PlayerPsiData data = PsiHelper.get(owner);
        float reserved = NecroSummonService.reservedPsi(owner);
        if (Math.abs(data.necroReservedPsi() - reserved) > 0.05f) {
            data.setNecroReservedPsi(reserved);
            PsiHelper.set(owner, data);
        }
    }

    /** Vanilla HandItems / ArmorItems style — reliable across 1.21 item components. */
    private static CompoundTag captureEquipment(LivingEntity entity, HolderLookup.Provider registries) {
        CompoundTag equipment = new CompoundTag();
        ListTag hands = new ListTag();
        hands.add(saveStack(entity.getItemBySlot(EquipmentSlot.MAINHAND), registries));
        hands.add(saveStack(entity.getItemBySlot(EquipmentSlot.OFFHAND), registries));
        equipment.put("HandItems", hands);

        ListTag armor = new ListTag();
        armor.add(saveStack(entity.getItemBySlot(EquipmentSlot.FEET), registries));
        armor.add(saveStack(entity.getItemBySlot(EquipmentSlot.LEGS), registries));
        armor.add(saveStack(entity.getItemBySlot(EquipmentSlot.CHEST), registries));
        armor.add(saveStack(entity.getItemBySlot(EquipmentSlot.HEAD), registries));
        equipment.put("ArmorItems", armor);
        return equipment;
    }

    private static Tag saveStack(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.save(registries);
    }

    private static void applyEquipment(LivingEntity entity, CompoundTag equipment, HolderLookup.Provider registries) {
        if (equipment == null || equipment.isEmpty()) {
            return;
        }
        if (equipment.contains("HandItems", Tag.TAG_LIST)) {
            ListTag hands = equipment.getList("HandItems", Tag.TAG_COMPOUND);
            if (hands.size() > 0) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, readStack(hands.get(0), registries));
            }
            if (hands.size() > 1) {
                entity.setItemSlot(EquipmentSlot.OFFHAND, readStack(hands.get(1), registries));
            }
        }
        if (equipment.contains("ArmorItems", Tag.TAG_LIST)) {
            ListTag armor = equipment.getList("ArmorItems", Tag.TAG_COMPOUND);
            EquipmentSlot[] slots = {
                EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
            };
            for (int i = 0; i < slots.length && i < armor.size(); i++) {
                entity.setItemSlot(slots[i], readStack(armor.get(i), registries));
            }
        }
        // Legacy slot-name map (older marks).
        for (EquipmentSlot slot : GEAR_SLOTS) {
            String key = slot.getSerializedName();
            if (equipment.contains(key)) {
                ItemStack stack = readStack(equipment.get(key), registries);
                if (!stack.isEmpty()) {
                    entity.setItemSlot(slot, stack);
                }
            }
        }
    }

    private static ItemStack readStack(Tag tag, HolderLookup.Provider registries) {
        if (tag == null || (tag instanceof CompoundTag compound && compound.isEmpty())) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);
    }

    /** Prefer non-empty stacks from primary over fallback. */
    private static CompoundTag mergeEquipment(CompoundTag primary, CompoundTag fallback) {
        if (primary == null || !hasAnyItem(primary)) {
            return fallback != null ? fallback.copy() : new CompoundTag();
        }
        if (fallback == null || !hasAnyItem(fallback)) {
            return primary.copy();
        }
        CompoundTag merged = fallback.copy();
        applyListPreferNonEmpty(merged, primary, "HandItems");
        applyListPreferNonEmpty(merged, primary, "ArmorItems");
        return merged;
    }

    private static void applyListPreferNonEmpty(CompoundTag dest, CompoundTag src, String key) {
        if (!src.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag srcList = src.getList(key, Tag.TAG_COMPOUND);
        ListTag destList = dest.contains(key, Tag.TAG_LIST)
                ? dest.getList(key, Tag.TAG_COMPOUND)
                : new ListTag();
        ListTag out = new ListTag();
        int size = Math.max(srcList.size(), destList.size());
        for (int i = 0; i < size; i++) {
            CompoundTag fromSrc = i < srcList.size() ? srcList.getCompound(i) : new CompoundTag();
            CompoundTag fromDest = i < destList.size() ? destList.getCompound(i) : new CompoundTag();
            out.add(!fromSrc.isEmpty() ? fromSrc : fromDest);
        }
        dest.put(key, out);
    }

    private static boolean hasAnyItem(CompoundTag equipment) {
        if (equipment.contains("HandItems", Tag.TAG_LIST)) {
            ListTag hands = equipment.getList("HandItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < hands.size(); i++) {
                if (!hands.getCompound(i).isEmpty()) {
                    return true;
                }
            }
        }
        if (equipment.contains("ArmorItems", Tag.TAG_LIST)) {
            ListTag armor = equipment.getList("ArmorItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < armor.size(); i++) {
                if (!armor.getCompound(i).isEmpty()) {
                    return true;
                }
            }
        }
        for (EquipmentSlot slot : GEAR_SLOTS) {
            if (equipment.contains(slot.getSerializedName())) {
                return true;
            }
        }
        return false;
    }

    private static void clearEquipment(LivingEntity entity) {
        for (EquipmentSlot slot : GEAR_SLOTS) {
            entity.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void lockEquipment(Mob mob) {
        for (EquipmentSlot slot : GEAR_SLOTS) {
            mob.setDropChance(slot, 0f);
        }
    }

    /** If kit somehow missing, give the vanilla default so ranged skeletons stay ranged. */
    private static void ensureDefaultWeapon(Mob mob) {
        if (!mob.getMainHandItem().isEmpty()) {
            return;
        }
        if (mob instanceof WitherSkeleton) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
        } else if (mob instanceof AbstractSkeleton) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }
}
