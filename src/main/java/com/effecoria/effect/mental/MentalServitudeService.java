package com.effecoria.effect.mental;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.effect.necromancy.NecroSummonService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Temporary mental servant: humanoid mobs dig tunnels / haul items into a bound chest.
 * Necromancer thralls are explicitly excluded.
 */
public final class MentalServitudeService {
    public static final String OWNER_TAG = "effecoria:mental_servant_owner";
    public static final String UNTIL_TAG = "effecoria:mental_servant_until";
    public static final String MODE_TAG = "effecoria:mental_servant_mode";
    public static final String CHEST_TAG = "effecoria:mental_servant_chest";
    public static final String CARGO_TAG = "effecoria:mental_servant_cargo";
    public static final String DIG_ORIGIN_TAG = "effecoria:mental_dig_origin";
    public static final String DIG_DIR_TAG = "effecoria:mental_dig_dir";
    public static final String DIG_INDEX_TAG = "effecoria:mental_dig_index";
    public static final String DIG_TOTAL_TAG = "effecoria:mental_dig_total";
    public static final String DIG_COOLDOWN_TAG = "effecoria:mental_dig_cd";

    private static final int MAX_CARGO_STACKS = 12;
    private static final float MAX_DIG_HARDNESS = 25f;
    private static final double CONTROL_RANGE = 48.0;

    public enum Mode {
        FOLLOW,
        DIG,
        HAUL;

        static Mode fromId(String id) {
            return switch (id) {
                case "dig" -> DIG;
                case "haul" -> HAUL;
                default -> FOLLOW;
            };
        }

        String id() {
            return switch (this) {
                case DIG -> "dig";
                case HAUL -> "haul";
                case FOLLOW -> "follow";
            };
        }
    }

    private MentalServitudeService() {}

    public static boolean isServant(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(OWNER_TAG)
                && data.contains(UNTIL_TAG)
                && entity.level().getGameTime() <= data.getLong(UNTIL_TAG);
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

    public static boolean seize(ServerPlayer caster, Mob mob, int durationTicks) {
        if (!canSeize(mob) || durationTicks <= 0) {
            return false;
        }
        durationTicks = BreathDebuffs.scaleDuration(caster, durationTicks);
        if (!MentalityService.tryAfflict(caster, mob, durationTicks)) {
            return false;
        }
        // Drop competing short-term puppet control.
        if (MentalCompulsionService.hasActive(mob)) {
            MentalCompulsionService.clear(mob);
        }
        CompoundTag data = mob.getPersistentData();
        data.putUUID(OWNER_TAG, caster.getUUID());
        data.putLong(UNTIL_TAG, caster.level().getGameTime() + durationTicks);
        data.putString(MODE_TAG, Mode.FOLLOW.id());
        clearDig(data);
        mob.setTarget(null);
        mob.getNavigation().stop();
        return true;
    }

    public static void release(Mob mob) {
        dropCargo(mob);
        CompoundTag data = mob.getPersistentData();
        data.remove(OWNER_TAG);
        data.remove(UNTIL_TAG);
        data.remove(MODE_TAG);
        data.remove(CHEST_TAG);
        data.remove(CARGO_TAG);
        clearDig(data);
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    public static void clearQuiet(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (!entity.getPersistentData().hasUUID(OWNER_TAG) && !entity.getPersistentData().contains(CARGO_TAG)) {
            return;
        }
        dropCargo(mob);
        CompoundTag data = mob.getPersistentData();
        data.remove(OWNER_TAG);
        data.remove(UNTIL_TAG);
        data.remove(MODE_TAG);
        data.remove(CHEST_TAG);
        data.remove(CARGO_TAG);
        clearDig(data);
    }

    private static void clearDig(CompoundTag data) {
        data.remove(DIG_ORIGIN_TAG);
        data.remove(DIG_DIR_TAG);
        data.remove(DIG_INDEX_TAG);
        data.remove(DIG_TOTAL_TAG);
        data.remove(DIG_COOLDOWN_TAG);
    }

    @Nullable
    public static Mob findOwned(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        AABB box = owner.getBoundingBox().inflate(CONTROL_RANGE);
        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> isOwnedBy(m, owner.getUUID()))) {
            double d = mob.distanceToSqr(owner);
            if (d < bestDist) {
                bestDist = d;
                best = mob;
            }
        }
        return best;
    }

    public static void bindChest(Mob servant, BlockPos chestPos) {
        servant.getPersistentData().putLong(CHEST_TAG, chestPos.asLong());
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

    public static void commandDig(Mob servant, BlockPos toward, int length) {
        CompoundTag data = servant.getPersistentData();
        BlockPos feet = servant.blockPosition();
        Direction dir = Direction.getNearest(toward.getX() - feet.getX(), 0, toward.getZ() - feet.getZ());
        if (dir.getAxis() == Direction.Axis.Y) {
            dir = servant.getDirection();
        }
        int total = Math.max(1, length);
        data.putString(MODE_TAG, Mode.DIG.id());
        data.putLong(DIG_ORIGIN_TAG, feet.asLong());
        data.putByte(DIG_DIR_TAG, (byte) dir.get3DDataValue());
        data.putInt(DIG_INDEX_TAG, 0);
        data.putInt(DIG_TOTAL_TAG, total);
        data.putInt(DIG_COOLDOWN_TAG, 0);
    }

    public static boolean isDepositContainer(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container;
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 2 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            AABB box = player.getBoundingBox().inflate(CONTROL_RANGE);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, box, MentalServitudeService::isServant)) {
                tickServant(level, mob);
            }
        }
    }

    private static void tickServant(ServerLevel level, Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (level.getGameTime() > data.getLong(UNTIL_TAG)) {
            UUID ownerId = data.hasUUID(OWNER_TAG) ? data.getUUID(OWNER_TAG) : null;
            release(mob);
            if (ownerId != null && level.getPlayerByUUID(ownerId) instanceof ServerPlayer owner) {
                owner.displayClientMessage(Component.translatable("message.effecoria.mental.servitude_end"), true);
            }
            return;
        }
        if (NecroSummonService.isNecroThrall(mob)) {
            // Safety: never keep a necro thrall as a mental servant.
            release(mob);
            return;
        }

        UUID ownerId = data.getUUID(OWNER_TAG);
        ServerPlayer owner = level.getPlayerByUUID(ownerId) instanceof ServerPlayer p ? p : null;
        if (owner == null || !owner.isAlive()) {
            release(mob);
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

        mob.setTarget(null);
        Mode mode = Mode.fromId(data.getString(MODE_TAG));
        boolean cargoFull = cargoStackCount(mob) >= MAX_CARGO_STACKS;
        boolean hasCargo = cargoStackCount(mob) > 0;
        BlockPos chest = data.contains(CHEST_TAG) ? BlockPos.of(data.getLong(CHEST_TAG)) : null;

        boolean digDone = mode == Mode.DIG && data.getInt(DIG_INDEX_TAG) >= data.getInt(DIG_TOTAL_TAG);
        if (hasCargo && (cargoFull || digDone || mode == Mode.HAUL)) {
            if (chest != null && isDepositContainer(level, chest)) {
                if (tryDeposit(level, mob, chest)) {
                    if (digDone && cargoStackCount(mob) == 0) {
                        data.putString(MODE_TAG, Mode.FOLLOW.id());
                        clearDig(data);
                        owner.displayClientMessage(
                                Component.translatable("message.effecoria.mental.servitude_dig_done"), true);
                    }
                    return;
                }
            } else if (mode == Mode.HAUL) {
                if (mob.distanceToSqr(owner) > 2.5 * 2.5) {
                    mob.getNavigation().moveTo(owner, 1.15);
                } else {
                    dropCargoAt(mob, owner.blockPosition());
                }
                return;
            }
        }

        switch (mode) {
            case FOLLOW -> {
                if (mob.distanceToSqr(owner) > 3.5 * 3.5) {
                    mob.getNavigation().moveTo(owner, 1.15);
                }
            }
            case DIG -> tickDig(level, mob, owner);
            case HAUL -> tickHaul(level, mob, chest);
        }
    }

    private static void tickDig(ServerLevel level, Mob mob, ServerPlayer owner) {
        CompoundTag data = mob.getPersistentData();
        int index = data.getInt(DIG_INDEX_TAG);
        int total = data.getInt(DIG_TOTAL_TAG);
        if (index >= total) {
            if (cargoStackCount(mob) == 0 || !data.contains(CHEST_TAG)) {
                data.putString(MODE_TAG, Mode.FOLLOW.id());
                clearDig(data);
                owner.displayClientMessage(Component.translatable("message.effecoria.mental.servitude_dig_done"), true);
            }
            return;
        }
        if (!data.contains(DIG_ORIGIN_TAG) || !data.contains(DIG_DIR_TAG)) {
            data.putString(MODE_TAG, Mode.FOLLOW.id());
            return;
        }
        if (cargoStackCount(mob) >= MAX_CARGO_STACKS) {
            return;
        }

        int cd = data.getInt(DIG_COOLDOWN_TAG);
        if (cd > 0) {
            data.putInt(DIG_COOLDOWN_TAG, cd - 1);
            return;
        }

        Direction dir = Direction.from3DDataValue(data.getByte(DIG_DIR_TAG));
        BlockPos origin = BlockPos.of(data.getLong(DIG_ORIGIN_TAG));
        BlockPos column = origin.relative(dir, index + 1);

        if (mob.distanceToSqr(Vec3.atCenterOf(column)) > 2.8 * 2.8) {
            mob.getNavigation().moveTo(column.getX() + 0.5, column.getY(), column.getZ() + 0.5, 1.1);
            return;
        }

        boolean broke = false;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos dig = column.above(dy);
            if (tryBreakForCargo(level, mob, dig)) {
                broke = true;
            }
        }
        data.putInt(DIG_INDEX_TAG, index + 1);
        data.putInt(DIG_COOLDOWN_TAG, broke ? 6 : 2);
        mob.getLookControl().setLookAt(column.getX() + 0.5, column.getY() + 0.5, column.getZ() + 0.5);
        level.playSound(null, column, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.45f, 1.1f);
    }

    private static void tickHaul(ServerLevel level, Mob mob, @Nullable BlockPos chest) {
        if (cargoStackCount(mob) >= MAX_CARGO_STACKS) {
            if (chest != null) {
                tryDeposit(level, mob, chest);
            }
            return;
        }
        ItemEntity item = nearestItem(level, mob, 12.0);
        if (item == null) {
            if (cargoStackCount(mob) > 0 && chest != null) {
                tryDeposit(level, mob, chest);
            }
            return;
        }
        if (mob.distanceToSqr(item) > 1.6 * 1.6) {
            mob.getNavigation().moveTo(item, 1.2);
            return;
        }
        ItemStack stack = item.getItem();
        ItemStack leftover = addCargo(mob, stack);
        if (leftover.isEmpty()) {
            item.discard();
        } else {
            item.setItem(leftover);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.35f, 1.2f);
    }

    private static boolean tryBreakForCargo(ServerLevel level, Mob mob, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0f || hardness > MAX_DIG_HARDNESS) {
            return false;
        }
        if (isDepositContainer(level, pos)) {
            return false;
        }
        CompoundTag data = mob.getPersistentData();
        if (data.contains(CHEST_TAG) && pos.asLong() == data.getLong(CHEST_TAG)) {
            return false;
        }

        List<ItemStack> drops = state.getDrops(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, mob));
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            ItemStack left = addCargo(mob, drop);
            if (!left.isEmpty()) {
                Block.popResource(level, pos, left);
            }
        }
        return true;
    }

    private static boolean tryDeposit(ServerLevel level, Mob mob, BlockPos chestPos) {
        if (mob.distanceToSqr(Vec3.atCenterOf(chestPos)) > 2.4 * 2.4) {
            mob.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.15);
            return true;
        }
        BlockEntity be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) {
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

}
