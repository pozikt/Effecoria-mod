package com.effecoria.effect.mental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.necromancy.DeathMarkService;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Permanent mental servants: mindless meat that only runs mage-issued orders
 * (idle / follow / dig tunnel / haul). Digging requires a tool (hand or from the
 * bound chest) and continues until the chest has no free space. Each servant
 * reserves Ψ equal to its max health. Necromancer thralls are excluded.
 */
public final class MentalServitudeService {
    public static final String OWNER_TAG = "effecoria:mental_servant_owner";
    public static final String RESERVE_TAG = "effecoria:mental_servant_reserve";
    public static final String MASTERY_TAG = "effecoria:mental_servant_mastery";
    public static final String MODE_TAG = "effecoria:mental_servant_mode";
    public static final String CHEST_TAG = "effecoria:mental_servant_chest";
    public static final String CARGO_TAG = "effecoria:mental_servant_cargo";
    public static final String DIG_ORIGIN_TAG = "effecoria:mental_dig_origin";
    public static final String DIG_DIR_TAG = "effecoria:mental_dig_dir";
    public static final String DIG_INDEX_TAG = "effecoria:mental_dig_index";
    public static final String DIG_COOLDOWN_TAG = "effecoria:mental_dig_cd";
    public static final String WARN_CD_TAG = "effecoria:mental_servant_warn_cd";
    /** Backup of the equipped dig tool (mainhand is unreliable on some humanoids). */
    public static final String TOOL_TAG = "effecoria:mental_servant_tool";

    private static final int MAX_CARGO_STACKS = 12;
    private static final float MAX_DIG_HARDNESS = 50f;
    private static final double CONTROL_RANGE = 48.0;
    /** Reach for chest tool grab / deposit — generous so stuck pathing still works. */
    private static final double INTERACT_RANGE = 4.25;
    /** MoveControl speed — setNoAi blocks reliable PathNavigation follow. */
    private static final double WALK_SPEED = 0.85;

    public enum Mode {
        IDLE,
        FOLLOW,
        DIG,
        HAUL;

        static Mode fromId(String id) {
            return switch (id) {
                case "follow" -> FOLLOW;
                case "dig" -> DIG;
                case "haul" -> HAUL;
                default -> IDLE;
            };
        }

        String id() {
            return switch (this) {
                case FOLLOW -> "follow";
                case DIG -> "dig";
                case HAUL -> "haul";
                case IDLE -> "idle";
            };
        }
    }

    private MentalServitudeService() {}

    public static boolean isServant(LivingEntity entity) {
        return entity instanceof Mob && entity.getPersistentData().hasUUID(OWNER_TAG);
    }

    public static boolean isOwnedBy(LivingEntity entity, UUID ownerId) {
        return isServant(entity)
                && ownerId.equals(entity.getPersistentData().getUUID(OWNER_TAG));
    }

    public static boolean canSeize(LivingEntity target) {
        if (!(target instanceof Mob) || target instanceof Player) {
            return false;
        }
        if (NecroSummonService.isNecroThrall(target)) {
            return false;
        }
        if (MentalityService.of(target) != MentalityService.Kind.HUMANOID) {
            return false;
        }
        if (MentalityService.isImmune(target)) {
            return false;
        }
        return true;
    }

    public static float reserveCostOf(LivingEntity target) {
        return Math.max(1f, target.getMaxHealth());
    }

    public static float reserveOf(Mob mob) {
        if (mob.getPersistentData().contains(RESERVE_TAG)) {
            return Math.max(1f, mob.getPersistentData().getFloat(RESERVE_TAG));
        }
        return Math.max(1f, mob.getMaxHealth());
    }

    public static float reservedPsi(Player owner) {
        if (owner.level().isClientSide()) {
            return 0f;
        }
        // Ledger map on the player — never depends on entity lookup.
        return PsiHelper.get(owner).mentalReservedPsi();
    }

    public static boolean canAfford(ServerPlayer owner, float reserveCost) {
        PlayerPsiData data = PsiHelper.get(owner);
        float cost = Math.max(1f, reserveCost);
        float already = NecroSummonService.reservedPsi(owner) + reservedPsi(owner);
        float usable = Math.max(0f, data.currentPsi() - already);
        return cost <= usable + 0.05f && already + cost <= data.maxPsi() - 1f;
    }

    /**
     * Permanent seize. Reserves Ψ = max health. Returns false if resisted or unaffordable.
     */
    public static boolean seize(ServerPlayer caster, Mob mob) {
        if (!canSeize(mob)) {
            return false;
        }
        float reserve = reserveCostOf(mob);
        if (!canAfford(caster, reserve)) {
            caster.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.mental.servitude_psi_reserve", (int) Math.ceil(reserve)),
                    true);
            return false;
        }
        PlayerPsiData casterData = PsiHelper.get(caster);
        float resist = MentalityService.resistChance(mob, casterData.breathingMastery());
        if (mob.getRandom().nextFloat() < resist) {
            return false;
        }
        if (MentalCompulsionService.hasActive(mob)) {
            MentalCompulsionService.clear(mob);
        }
        CompoundTag data = mob.getPersistentData();
        data.putUUID(OWNER_TAG, caster.getUUID());
        data.putFloat(RESERVE_TAG, reserve);
        data.putFloat(MASTERY_TAG, casterData.breathingMastery());
        data.putString(MODE_TAG, Mode.IDLE.id());
        clearDig(data);
        mob.setPersistenceRequired();
        stripToPuppet(mob);

        casterData.trackMentalServant(mob.getUUID(), reserve);
        PsiHelper.set(caster, casterData);
        DeathMarkService.syncReservedPsi(caster);
        return true;
    }

    public static void release(Mob mob) {
        UUID ownerId = mob.getPersistentData().hasUUID(OWNER_TAG)
                ? mob.getPersistentData().getUUID(OWNER_TAG)
                : null;
        UUID servantId = mob.getUUID();
        dropCargo(mob);
        CompoundTag data = mob.getPersistentData();
        data.remove(OWNER_TAG);
        data.remove(RESERVE_TAG);
        data.remove(MASTERY_TAG);
        data.remove(MODE_TAG);
        data.remove(CHEST_TAG);
        data.remove(CARGO_TAG);
        data.remove(WARN_CD_TAG);
        data.remove(TOOL_TAG);
        clearDig(data);
        data.remove("effecoria:mental_servant_until");
        restoreAutonomy(mob);

        if (ownerId != null && mob.level().getServer() != null) {
            ServerPlayer owner = mob.level().getServer().getPlayerList().getPlayer(ownerId);
            if (owner != null) {
                PlayerPsiData pdata = PsiHelper.get(owner);
                pdata.untrackMentalServant(servantId);
                PsiHelper.set(owner, pdata);
                DeathMarkService.syncReservedPsi(owner);
            }
        }
    }

    public static void clearQuiet(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (!entity.getPersistentData().hasUUID(OWNER_TAG) && !entity.getPersistentData().contains(CARGO_TAG)) {
            return;
        }
        release(mob);
    }

    /** Disable all native brain/goals — servant only runs our command tick. */
    private static void stripToPuppet(Mob mob) {
        mob.setNoAi(true);
        mob.setTarget(null);
        mob.setAggressive(false);
        mob.getNavigation().stop();
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);
        // Drop lingering wrapped goals that may re-register before no-ai sticks.
        List<WrappedGoal> leftovers = new ArrayList<>(mob.goalSelector.getAvailableGoals());
        for (WrappedGoal wrapped : leftovers) {
            mob.goalSelector.removeGoal(wrapped.getGoal());
        }
    }

    private static void restoreAutonomy(Mob mob) {
        mob.setNoAi(false);
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    private static void clearDig(CompoundTag data) {
        data.remove(DIG_ORIGIN_TAG);
        data.remove(DIG_DIR_TAG);
        data.remove(DIG_INDEX_TAG);
        data.remove(DIG_COOLDOWN_TAG);
        // Legacy length-limited digs
        data.remove("effecoria:mental_dig_total");
    }

    public static List<Mob> listOwned(Player owner) {
        if (!(owner instanceof ServerPlayer serverOwner)) {
            return List.of();
        }
        MinecraftServer server = serverOwner.getServer();
        if (server == null) {
            return List.of();
        }
        // Keep ledger populated for nearby tagged servants.
        seedNearby(serverOwner);

        PlayerPsiData data = PsiHelper.get(serverOwner);
        List<UUID> ledger = data.mentalServantIds();
        List<Mob> owned = new ArrayList<>();
        List<UUID> stale = new ArrayList<>();
        for (UUID id : new ArrayList<>(ledger)) {
            Mob found = findServant(server, id);
            if (found == null) {
                // Unloaded / not in lookup yet — keep reserving, do not prune.
                continue;
            }
            if (!found.isAlive() || !isOwnedBy(found, serverOwner.getUUID())) {
                stale.add(id);
                continue;
            }
            owned.add(found);
        }
        if (!stale.isEmpty()) {
            for (UUID id : stale) {
                data.untrackMentalServant(id);
            }
            PsiHelper.set(serverOwner, data);
            DeathMarkService.syncReservedPsi(serverOwner);
        }
        return owned;
    }

    @Nullable
    private static Mob findServant(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    @Nullable
    public static Mob findOwned(ServerPlayer owner) {
        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob mob : listOwned(owner)) {
            if (mob.level() != owner.level()) {
                continue;
            }
            double d = mob.distanceToSqr(owner);
            if (d > CONTROL_RANGE * CONTROL_RANGE) {
                continue;
            }
            if (d < bestDist) {
                bestDist = d;
                best = mob;
            }
        }
        if (best != null) {
            return best;
        }
        // Fallback: OWNER_TAG nearby even if ledger lagged.
        AABB box = owner.getBoundingBox().inflate(CONTROL_RANGE);
        for (Mob mob : owner.level().getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
            if (!isOwnedBy(mob, owner.getUUID())) {
                continue;
            }
            double d = mob.distanceToSqr(owner);
            if (d < bestDist) {
                bestDist = d;
                best = mob;
            }
        }
        if (best != null) {
            PlayerPsiData data = PsiHelper.get(owner);
            data.trackMentalServant(best.getUUID(), reserveOf(best));
            PsiHelper.set(owner, data);
            DeathMarkService.syncReservedPsi(owner);
        }
        return best;
    }

    public static void bindChest(Mob servant, BlockPos chestPos) {
        servant.getPersistentData().putLong(CHEST_TAG, chestPos.asLong());
        // Prefetch a dig tool immediately on bind.
        if (servant.level() instanceof ServerLevel level) {
            Container container = resolveContainer(level, chestPos);
            if (container != null && !hasWorkTool(servant)) {
                tryFetchTool(level, servant, chestPos, container, null);
            }
        }
    }

    public static void commandIdle(Mob servant) {
        CompoundTag data = servant.getPersistentData();
        data.putString(MODE_TAG, Mode.IDLE.id());
        clearDig(data);
        servant.getNavigation().stop();
    }

    public static void commandFollow(Mob servant) {
        CompoundTag data = servant.getPersistentData();
        data.putString(MODE_TAG, Mode.FOLLOW.id());
        clearDig(data);
    }

    public static void commandHaul(Mob servant) {
        CompoundTag data = servant.getPersistentData();
        data.putString(MODE_TAG, Mode.HAUL.id());
        clearDig(data);
    }

    /** Start an open-ended tunnel toward {@code toward}; stops when the bound chest is full. */
    public static void commandDig(Mob servant, BlockPos toward) {
        CompoundTag data = servant.getPersistentData();
        BlockPos feet = servant.blockPosition();
        Direction dir = Direction.getNearest(toward.getX() - feet.getX(), 0, toward.getZ() - feet.getZ());
        if (dir.getAxis() == Direction.Axis.Y) {
            dir = servant.getDirection();
        }
        data.putString(MODE_TAG, Mode.DIG.id());
        data.putLong(DIG_ORIGIN_TAG, feet.asLong());
        data.putByte(DIG_DIR_TAG, (byte) dir.get3DDataValue());
        data.putInt(DIG_INDEX_TAG, 0);
        data.putInt(DIG_COOLDOWN_TAG, 0);

        // Immediately pull a tool from the bound chest (no walk required).
        if (!hasWorkTool(servant)
                && servant.level() instanceof ServerLevel level
                && data.contains(CHEST_TAG)) {
            BlockPos chest = BlockPos.of(data.getLong(CHEST_TAG));
            Container container = resolveContainer(level, chest);
            if (container != null) {
                tryFetchTool(level, servant, chest, container, null);
            }
        }
    }

    public static boolean isDepositContainer(ServerLevel level, BlockPos pos) {
        return resolveContainer(level, pos) != null;
    }

    /**
     * Resolves the full inventory for the bound block (double chests included).
     */
    @Nullable
    private static Container resolveContainer(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container combined = ChestBlock.getContainer(chestBlock, state, level, pos, true);
            if (combined != null) {
                return combined;
            }
        }
        // Barrel / hopper / single chest BE / shulker / etc.
        return be instanceof Container container ? container : null;
    }

    private static boolean inInteractRange(Mob mob, BlockPos pos) {
        return mob.distanceToSqr(Vec3.atCenterOf(pos)) <= INTERACT_RANGE * INTERACT_RANGE;
    }

    /** Same-dimension bound chest within control leash. */
    private static boolean canReachChestInv(Mob mob, BlockPos chest) {
        if (!(mob.level() instanceof ServerLevel)) {
            return false;
        }
        return mob.distanceToSqr(Vec3.atCenterOf(chest)) <= CONTROL_RANGE * CONTROL_RANGE;
    }

    public static int freeCargoSlots(Mob mob) {
        return Math.max(0, MAX_CARGO_STACKS - cargoStackCount(mob));
    }

    public static boolean servantHasTool(Mob mob) {
        return hasWorkTool(mob);
    }

    public static void tick(ServerLevel level) {
        boolean logicTick = level.getGameTime() % 2 == 0;
        for (ServerPlayer player : level.players()) {
            for (Mob mob : listOwned(player)) {
                if (mob.level() != level) {
                    continue;
                }
                // Keep them puppets every tick so villager brain never teleports/jobs.
                if (!mob.isNoAi()) {
                    stripToPuppet(mob);
                }
                mob.setTarget(null);
                mob.setAggressive(false);
                if (logicTick) {
                    tickServant(level, mob, player);
                }
                // setNoAi skips serverAiStep — pathfinding must be driven every tick.
                tickPuppetMovement(mob);
            }
            if (level.getGameTime() % 10 == 0) {
                DeathMarkService.syncReservedPsi(player);
            }
            if (level.getGameTime() % 40 == 0) {
                seedNearby(player);
            }
        }
    }

    private static void seedNearby(ServerPlayer owner) {
        PlayerPsiData data = PsiHelper.get(owner);
        boolean changed = false;
        AABB box = owner.getBoundingBox().inflate(CONTROL_RANGE);
        for (Mob mob : owner.level().getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
            if (isOwnedBy(mob, owner.getUUID()) && !data.mentalServantIds().contains(mob.getUUID())) {
                data.trackMentalServant(mob.getUUID(), reserveOf(mob));
                changed = true;
            }
        }
        if (changed) {
            PsiHelper.set(owner, data);
            DeathMarkService.syncReservedPsi(owner);
        }
    }

    private static void tickServant(ServerLevel level, Mob mob, ServerPlayer owner) {
        CompoundTag data = mob.getPersistentData();
        if (NecroSummonService.isNecroThrall(mob)) {
            release(mob);
            return;
        }
        if (!owner.isAlive()) {
            mob.getNavigation().stop();
            return;
        }

        level.sendParticles(
                ModParticleTypes.MENTAL_FORCE.get(),
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.85,
                mob.getZ(),
                1,
                0.12,
                0.15,
                0.12,
                0.001);

        Mode mode = Mode.fromId(data.getString(MODE_TAG));
        BlockPos chest = data.contains(CHEST_TAG) ? BlockPos.of(data.getLong(CHEST_TAG)) : null;

        switch (mode) {
            case IDLE -> mob.getNavigation().stop();
            case FOLLOW -> {
                if (mob.distanceToSqr(owner) > 3.5 * 3.5) {
                    walkTo(mob, owner.getX(), owner.getY(), owner.getZ());
                } else {
                    mob.getNavigation().stop();
                }
            }
            case DIG -> tickDigOrder(level, mob, owner, chest);
            case HAUL -> tickHaulOrder(level, mob, owner, chest);
        }
    }

    private static void tickPuppetMovement(Mob mob) {
        mob.getNavigation().tick();
        mob.getMoveControl().tick();
        mob.getLookControl().tick();
        mob.getJumpControl().tick();
    }

    private static void tickDigOrder(ServerLevel level, Mob mob, ServerPlayer owner, @Nullable BlockPos chest) {
        if (chest == null || !isDepositContainer(level, chest)) {
            warn(owner, mob, "message.effecoria.mental.servitude_need_chest");
            commandIdle(mob);
            return;
        }
        Container container = resolveContainer(level, chest);
        if (container == null) {
            commandIdle(mob);
            return;
        }

        // Need a tool before any mining — walk to chest and pull one out.
        if (!hasWorkTool(mob)) {
            if (!tryFetchTool(level, mob, chest, container, owner)) {
                return;
            }
        }

        boolean cargoFull = freeCargoSlots(mob) <= 0;
        boolean hasCargo = cargoStackCount(mob) > 0;

        // Inventory full → dump into chest, then continue if chest still has room.
        if (cargoFull || (hasCargo && !containerHasSpace(container))) {
            if (tryDeposit(level, mob, chest)) {
                if (cargoStackCount(mob) > 0 && !containerHasSpace(container)) {
                    finishDig(owner, mob, "message.effecoria.mental.servitude_chest_full");
                }
            }
            return;
        }

        if (!containerHasSpace(container) && !hasCargo) {
            finishDig(owner, mob, "message.effecoria.mental.servitude_chest_full");
            return;
        }

        // Dig only as many column-breaks as free cargo slots can absorb this trip.
        if (freeCargoSlots(mob) <= 0) {
            return;
        }
        tickDigMine(level, mob, owner);
    }

    private static void finishDig(ServerPlayer owner, Mob mob, String messageKey) {
        clearDig(mob.getPersistentData());
        mob.getPersistentData().putString(MODE_TAG, Mode.IDLE.id());
        mob.getNavigation().stop();
        owner.displayClientMessage(Component.translatable(messageKey), true);
    }

    private static void tickDigMine(ServerLevel level, Mob mob, ServerPlayer owner) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(DIG_ORIGIN_TAG) || !data.contains(DIG_DIR_TAG)) {
            commandIdle(mob);
            return;
        }

        ItemStack tool = workTool(mob);
        if (tool.isEmpty()) {
            return;
        }

        int cd = data.getInt(DIG_COOLDOWN_TAG);
        if (cd > 0) {
            data.putInt(DIG_COOLDOWN_TAG, cd - 1);
            // Keep walking toward the face while waiting between swings.
            return;
        }

        int index = data.getInt(DIG_INDEX_TAG);
        Direction dir = Direction.from3DDataValue(data.getByte(DIG_DIR_TAG));
        BlockPos origin = BlockPos.of(data.getLong(DIG_ORIGIN_TAG));
        BlockPos column = origin.relative(dir, index + 1);

        if (mob.distanceToSqr(Vec3.atCenterOf(column)) > 2.8 * 2.8) {
            walkTo(mob, column.getX() + 0.5, column.getY(), column.getZ() + 0.5);
            return;
        }

        boolean broke = false;
        int swingTicks = 12;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos dig = column.above(dy);
            int took = tryBreakForCargo(level, mob, dig, tool);
            if (took > 0) {
                broke = true;
                swingTicks = Math.max(swingTicks, took);
            }
            tool = workTool(mob);
            if (tool.isEmpty()) {
                // Tool broke mid-column — fetch another next tick.
                data.putInt(DIG_COOLDOWN_TAG, 4);
                return;
            }
        }
        data.putInt(DIG_INDEX_TAG, index + 1);
        data.putInt(DIG_COOLDOWN_TAG, broke ? swingTicks : 8);
        mob.getLookControl().setLookAt(column.getX() + 0.5, column.getY() + 0.5, column.getZ() + 0.5);
        if (broke) {
            level.playSound(null, column, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.45f, 1.0f);
        }
    }

    private static void tickHaulOrder(
            ServerLevel level, Mob mob, ServerPlayer owner, @Nullable BlockPos chest) {
        boolean cargoFull = freeCargoSlots(mob) <= 0;
        boolean hasCargo = cargoStackCount(mob) > 0;

        if (hasCargo && (cargoFull || chest == null)) {
            if (chest != null && isDepositContainer(level, chest)) {
                tryDeposit(level, mob, chest);
            } else if (mob.distanceToSqr(owner) > 2.5 * 2.5) {
                walkTo(mob, owner.getX(), owner.getY(), owner.getZ());
            } else {
                dropCargoAt(mob, owner.blockPosition());
            }
            return;
        }

        if (cargoFull && chest != null) {
            tryDeposit(level, mob, chest);
            return;
        }

        ItemEntity item = nearestItem(level, mob, 12.0);
        if (item == null) {
            if (hasCargo && chest != null) {
                tryDeposit(level, mob, chest);
            } else {
                mob.getNavigation().stop();
            }
            return;
        }
        if (mob.distanceToSqr(item) > 1.6 * 1.6) {
            walkTo(mob, item.getX(), item.getY(), item.getZ());
            return;
        }
        ItemStack stack = item.getItem();
        ItemStack leftover = addCargo(mob, stack);
        if (leftover.isEmpty()) {
            item.discard();
        } else {
            item.setItem(leftover);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.35f, 1.0f);
    }

    private static void walkTo(Mob mob, double x, double y, double z) {
        // Direct locomotion: LevelTick Post + setNoAi make MoveControl/Navigation flaky.
        Vec3 pos = mob.position();
        double dx = x - pos.x;
        double dz = z - pos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        mob.getLookControl().setLookAt(x, y + 0.5, z);
        if (horiz < 0.2) {
            return;
        }
        double step = Math.min(0.2, horiz);
        double nx = dx / horiz * step;
        double nz = dz / horiz * step;
        mob.setDeltaMovement(nx, mob.getDeltaMovement().y, nz);
        mob.hasImpulse = true;
        // Small hop if stuck against a step-up.
        if (mob.horizontalCollision && mob.onGround()) {
            mob.setDeltaMovement(mob.getDeltaMovement().x, 0.42, mob.getDeltaMovement().z);
        }
    }

    private static boolean hasWorkTool(Mob mob) {
        return !workTool(mob).isEmpty();
    }

    private static ItemStack workTool(Mob mob) {
        // NBT is source of truth — mainhand is display only and flaky on villagers.
        CompoundTag data = mob.getPersistentData();
        if (data.contains(TOOL_TAG, Tag.TAG_COMPOUND)) {
            ItemStack stored = ItemStack.parse(mob.level().registryAccess(), data.getCompound(TOOL_TAG))
                    .orElse(ItemStack.EMPTY);
            if (isWorkTool(stored)) {
                ItemStack hand = mob.getMainHandItem();
                if (!isWorkTool(hand) || !ItemStack.isSameItemSameComponents(hand, stored)) {
                    mob.setItemSlot(EquipmentSlot.MAINHAND, stored.copy());
                    mob.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
                    hand = mob.getMainHandItem();
                }
                // Prefer the live hand stack so hurtAndBreak mutates the real item.
                return isWorkTool(hand) ? hand : stored;
            }
            data.remove(TOOL_TAG);
        }
        ItemStack hand = mob.getMainHandItem();
        if (isWorkTool(hand)) {
            data.put(TOOL_TAG, hand.save(mob.level().registryAccess()));
            return hand;
        }
        return ItemStack.EMPTY;
    }

    private static void equipWorkTool(Mob mob, ItemStack tool) {
        ItemStack copy = tool.isEmpty() ? ItemStack.EMPTY : tool.copy();
        mob.setItemSlot(EquipmentSlot.MAINHAND, copy);
        mob.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
        CompoundTag data = mob.getPersistentData();
        if (copy.isEmpty()) {
            data.remove(TOOL_TAG);
        } else {
            data.put(TOOL_TAG, copy.save(mob.level().registryAccess()));
        }
    }

    private static boolean isWorkTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof DiggerItem) {
            return true;
        }
        if (stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.HOES)) {
            return true;
        }
        return stack.canPerformAction(ItemAbilities.PICKAXE_DIG)
                || stack.canPerformAction(ItemAbilities.SHOVEL_DIG)
                || stack.canPerformAction(ItemAbilities.AXE_DIG);
    }

    /**
     * Pull a digger from the bound chest into the servant's hand.
     * Does not require walking — mental reach to the bound inventory.
     * {@code owner} may be null (silent) when called from commandDig.
     */
    private static boolean tryFetchTool(
            ServerLevel level,
            Mob mob,
            BlockPos chest,
            Container container,
            @Nullable ServerPlayer owner) {
        if (!canReachChestInv(mob, chest)) {
            if (owner != null) {
                warn(owner, mob, "message.effecoria.mental.servitude_need_chest");
            }
            return false;
        }
        int slot = findToolSlot(container);
        if (slot < 0) {
            if (owner != null) {
                int filled = 0;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (!container.getItem(i).isEmpty()) {
                        filled++;
                    }
                }
                owner.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.mental.servitude_need_tool_detail",
                                filled,
                                container.getContainerSize()),
                        true);
                finishDig(owner, mob, "message.effecoria.mental.servitude_need_tool");
            }
            return false;
        }
        ItemStack inSlot = container.getItem(slot);
        if (!isWorkTool(inSlot)) {
            return false;
        }
        ItemStack tool = inSlot.split(1);
        container.setItem(slot, inSlot.isEmpty() ? ItemStack.EMPTY : inSlot);
        if (!isWorkTool(tool)) {
            insertStack(container, tool);
            container.setChanged();
            return false;
        }
        // Stash non-tool held item into cargo / chest.
        ItemStack previous = mob.getMainHandItem();
        if (!previous.isEmpty() && !isWorkTool(previous)) {
            ItemStack left = addCargo(mob, previous);
            if (!left.isEmpty()) {
                left = insertStack(container, left);
                if (!left.isEmpty()) {
                    Containers.dropItemStack(
                            level, chest.getX() + 0.5, chest.getY() + 0.5, chest.getZ() + 0.5, left);
                }
            }
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        equipWorkTool(mob, tool.copy());
        container.setChanged();
        level.playSound(null, mob.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.55f, 0.95f);
        if (owner != null) {
            owner.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.mental.servitude_tool_taken", tool.getHoverName()),
                    true);
        }
        return true;
    }

    private static int findToolSlot(Container container) {
        int shovel = -1;
        int axe = -1;
        int other = -1;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!isWorkTool(stack)) {
                continue;
            }
            // Prefer pickaxes for tunnel work.
            if (stack.getItem() instanceof PickaxeItem || stack.is(ItemTags.PICKAXES)) {
                return i;
            }
            if ((stack.getItem() instanceof ShovelItem || stack.is(ItemTags.SHOVELS)) && shovel < 0) {
                shovel = i;
            } else if ((stack.getItem() instanceof AxeItem || stack.is(ItemTags.AXES)) && axe < 0) {
                axe = i;
            } else if (other < 0) {
                other = i;
            }
        }
        if (shovel >= 0) {
            return shovel;
        }
        if (axe >= 0) {
            return axe;
        }
        return other;
    }

    private static boolean containerHasSpace(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return swing cooldown ticks if a block was broken, else 0
     */
    private static int tryBreakForCargo(ServerLevel level, Mob mob, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0f || hardness > MAX_DIG_HARDNESS) {
            return 0;
        }
        if (isDepositContainer(level, pos)) {
            return 0;
        }
        CompoundTag data = mob.getPersistentData();
        if (data.contains(CHEST_TAG) && pos.asLong() == data.getLong(CHEST_TAG)) {
            return 0;
        }
        if (freeCargoSlots(mob) <= 0) {
            return 0;
        }
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
            return 0;
        }
        float destroySpeed = tool.getDestroySpeed(state);
        if (destroySpeed <= 1.0f && hardness > 0.5f) {
            // Wrong / useless tool on this block — skip.
            return 0;
        }

        List<ItemStack> drops = state.getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool.copy())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, mob));
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            ItemStack left = addCargo(mob, drop);
            if (!left.isEmpty()) {
                Block.popResource(level, pos, left);
            }
        }
        tool.hurtAndBreak(1, mob, EquipmentSlot.MAINHAND);
        // Keep NBT tool mirror in sync (broken tools become empty).
        ItemStack after = mob.getMainHandItem();
        if (isWorkTool(after)) {
            mob.getPersistentData().put(TOOL_TAG, after.save(mob.level().registryAccess()));
        } else {
            mob.getPersistentData().remove(TOOL_TAG);
        }
        int swing = Mth.clamp(Math.round(hardness * 24f / Math.max(0.5f, destroySpeed)), 10, 50);
        return swing;
    }

    private static boolean tryDeposit(ServerLevel level, Mob mob, BlockPos chestPos) {
        if (!inInteractRange(mob, chestPos)) {
            walkTo(mob, chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5);
            return true;
        }
        Container container = resolveContainer(level, chestPos);
        if (container == null) {
            mob.getPersistentData().remove(CHEST_TAG);
            return false;
        }
        List<ItemStack> cargo = readCargo(mob);
        if (cargo.isEmpty()) {
            return false;
        }
        List<ItemStack> remain = new ArrayList<>();
        for (ItemStack stack : cargo) {
            ItemStack leftover = insertStack(container, stack);
            if (!leftover.isEmpty()) {
                remain.add(leftover);
            }
        }
        writeCargo(mob, remain);
        container.setChanged();
        level.playSound(null, chestPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1.0f);
        return true;
    }

    private static ItemStack insertStack(Container container, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                int put = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                container.setItem(i, remaining.split(put));
                continue;
            }
            if (ItemStack.isSameItemSameComponents(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int put = Math.min(space, remaining.getCount());
                slot.grow(put);
                remaining.shrink(put);
                container.setItem(i, slot);
            }
        }
        return remaining;
    }

    @Nullable
    private static ItemEntity nearestItem(ServerLevel level, Mob mob, double range) {
        AABB box = mob.getBoundingBox().inflate(range);
        ItemEntity best = null;
        double bestDist = range * range;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> !e.getItem().isEmpty())) {
            double d = mob.distanceToSqr(item);
            if (d < bestDist) {
                bestDist = d;
                best = item;
            }
        }
        return best;
    }

    private static int cargoStackCount(Mob mob) {
        return readCargo(mob).size();
    }

    private static List<ItemStack> readCargo(Mob mob) {
        ListTag list = mob.getPersistentData().getList(CARGO_TAG, Tag.TAG_COMPOUND);
        List<ItemStack> stacks = new ArrayList<>();
        var registries = mob.level().registryAccess();
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(registries, list.getCompound(i)).ifPresent(stacks::add);
        }
        return stacks;
    }

    private static void writeCargo(Mob mob, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.save(mob.level().registryAccess()));
            }
        }
        if (list.isEmpty()) {
            mob.getPersistentData().remove(CARGO_TAG);
        } else {
            mob.getPersistentData().put(CARGO_TAG, list);
        }
    }

    private static ItemStack addCargo(Mob mob, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> cargo = readCargo(mob);
        ItemStack remaining = stack.copy();
        for (ItemStack slot : cargo) {
            if (remaining.isEmpty()) {
                break;
            }
            if (ItemStack.isSameItemSameComponents(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int put = Math.min(space, remaining.getCount());
                slot.grow(put);
                remaining.shrink(put);
            }
        }
        while (!remaining.isEmpty() && cargo.size() < MAX_CARGO_STACKS) {
            int put = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            cargo.add(remaining.split(put));
        }
        writeCargo(mob, cargo);
        return remaining;
    }

    private static void dropCargo(Mob mob) {
        dropCargoAt(mob, mob.blockPosition());
    }

    private static void dropCargoAt(Mob mob, BlockPos pos) {
        List<ItemStack> cargo = readCargo(mob);
        if (cargo.isEmpty()) {
            return;
        }
        if (mob.level() instanceof ServerLevel level) {
            for (ItemStack stack : cargo) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack);
            }
        }
        mob.getPersistentData().remove(CARGO_TAG);
    }

    private static void warn(ServerPlayer owner, Mob mob, String key) {
        CompoundTag data = mob.getPersistentData();
        long now = mob.level().getGameTime();
        if (data.getLong(WARN_CD_TAG) > now) {
            return;
        }
        data.putLong(WARN_CD_TAG, now + 40);
        owner.displayClientMessage(Component.translatable(key), true);
    }
}
