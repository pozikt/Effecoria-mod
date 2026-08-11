package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiTurretMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModSounds;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.alchemy.TurretAim;
import com.effecoria.core.alchemy.TurretAssembly;
import com.effecoria.core.alchemy.TurretKind;
import com.effecoria.entity.TurretBoltEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Logic lives on the mount; fires only when a barrel is assembled. */
public final class PhiTurretBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_AMMO = 0;
    public static final int SLOT_COUNT = 1;

    public static final int DATA_ARMED = 0;
    public static final int DATA_HEAT = 1;
    public static final int DATA_COOLDOWN = 2;
    public static final int DATA_POWER = 3;
    public static final int DATA_HAS_TARGET = 4;
    public static final int DATA_KIND = 5;
    public static final int DATA_FORMED = 6;
    public static final int DATA_COUNT = 7;

    public static final int MAX_HEAT = 100;
    public static final int OVERHEAT_COOLDOWN = 100;
    public static final int BREECH_ANIM_TICKS = 8;
    private static final float TURN_SPEED = 8f;
    private static final float AIM_TOLERANCE_DEG = 8f;

    private static final int[] AMMO_SLOTS = {SLOT_AMMO};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private TurretKind kind = TurretKind.NONE;
    private boolean armed;
    private int heat;
    private int cooldown;
    private int overheatCooldown;
    private UUID lastTargetId;
    private float aimYaw;
    private float aimPitch;
    private float clientAimYaw;
    private float clientAimPitch;
    private boolean aimInitialized;
    /** Game time when breech kick started; -1 = idle. */
    private long breechStartTick = -1L;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ARMED -> armed ? 1 : 0;
                case DATA_HEAT -> heat;
                case DATA_COOLDOWN -> Math.max(cooldown, overheatCooldown);
                case DATA_POWER -> level == null ? 0 : Math.round(PhiPower.powerFactor(level, worldPosition) * 100f);
                case DATA_HAS_TARGET -> lastTargetId != null ? 1 : 0;
                case DATA_KIND -> kind.ordinal();
                case DATA_FORMED -> kind.isEmitter() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_ARMED -> armed = value != 0;
                case DATA_HEAT -> heat = value;
                case DATA_COOLDOWN -> cooldown = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_TURRET.get(), pos, state);
        if (state.hasProperty(TurretMountBlock.KIND)) {
            kind = state.getValue(TurretMountBlock.KIND);
        }
        TurretAim.Angles rest = TurretAim.restPose(state);
        aimYaw = rest.yawDeg();
        aimPitch = rest.pitchDeg();
        clientAimYaw = aimYaw;
        clientAimPitch = aimPitch;
        aimInitialized = true;
    }

    public void setAssembledKind(TurretKind next) {
        if (kind != next) {
            kind = next == null ? TurretKind.NONE : next;
            if (!kind.isEmitter()) {
                armed = false;
            }
            setChanged();
            syncAim();
        }
    }

    public TurretKind kind() {
        return kind;
    }

    public boolean formed() {
        return kind.isEmitter();
    }

    public float aimYaw() {
        return aimYaw;
    }

    public float aimPitch() {
        return aimPitch;
    }

    public float getClientAimYaw(float partialTick) {
        return Mth.rotLerp(partialTick, clientAimYaw, aimYaw);
    }

    public float getClientAimPitch(float partialTick) {
        return Mth.rotLerp(partialTick, clientAimPitch, aimPitch);
    }

    /** 0 = closed, 1 = fully retracted side rails (kick then return). */
    public float getBreechProgress(float partialTick) {
        if (breechStartTick < 0L || level == null) {
            return 0f;
        }
        float t = (level.getGameTime() - breechStartTick + partialTick) / (float) BREECH_ANIM_TICKS;
        if (t <= 0f || t >= 1f) {
            return 0f;
        }
        // Fast kick back, slower return.
        if (t < 0.35f) {
            return t / 0.35f;
        }
        return 1f - (t - 0.35f) / 0.65f;
    }

    private void triggerBreech() {
        if (level != null) {
            breechStartTick = level.getGameTime();
            syncAim();
        }
    }

    public ContainerData getData() {
        return data;
    }

    public boolean armed() {
        return armed;
    }

    public void toggleArmed() {
        if (!formed()) {
            return;
        }
        armed = !armed;
        setChanged();
        syncAim();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiTurretBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!(state.getBlock() instanceof TurretMountBlock)) {
            return;
        }
        if (!be.aimInitialized) {
            TurretAim.Angles rest = TurretAim.restPose(state);
            be.aimYaw = rest.yawDeg();
            be.aimPitch = rest.pitchDeg();
            be.aimInitialized = true;
        }

        boolean changed = false;

        if (be.overheatCooldown > 0) {
            be.overheatCooldown--;
            changed = true;
        }
        if (be.cooldown > 0) {
            be.cooldown--;
            changed = true;
        }
        if (be.heat > 0 && server.getGameTime() % 4L == 0L) {
            be.heat = Math.max(0, be.heat - 1);
            changed = true;
        }

        boolean formed = state.getValue(TurretMountBlock.FORMED) && be.kind.isEmitter();
        boolean litWanted = formed && be.armed && PhiPower.hasPower(level, pos) && be.overheatCooldown <= 0;
        if (state.getValue(TurretMountBlock.LIT) != litWanted) {
            level.setBlock(pos, state.setValue(TurretMountBlock.LIT, litWanted), Block.UPDATE_CLIENTS);
            state = level.getBlockState(pos);
        }

        if (!formed) {
            be.lastTargetId = null;
            if (changed) {
                be.setChanged();
            }
            return;
        }

        Vec3 pivot = TurretAim.pivot(pos, state);
        LivingEntity target = null;
        if (be.armed && be.overheatCooldown <= 0 && PhiPower.powerFactor(level, pos) >= be.kind.minPowerFactor()) {
            target = be.findTarget(server);
        }

        TurretAim.Angles desired;
        if (target != null) {
            be.lastTargetId = target.getUUID();
            desired = TurretAim.anglesToward(pivot, target.getEyePosition());
            desired = TurretAim.clamp(state, desired.yawDeg(), desired.pitchDeg());
        } else {
            be.lastTargetId = null;
            desired = TurretAim.restPose(state);
            desired = TurretAim.clamp(state, desired.yawDeg(), desired.pitchDeg());
        }

        float prevYaw = be.aimYaw;
        float prevPitch = be.aimPitch;
        be.aimYaw = TurretAim.approachAngle(be.aimYaw, desired.yawDeg(), TURN_SPEED);
        be.aimPitch = TurretAim.approachAngle(be.aimPitch, desired.pitchDeg(), TURN_SPEED);
        if (Math.abs(Mth.wrapDegrees(be.aimYaw - prevYaw)) > 0.05f
                || Math.abs(be.aimPitch - prevPitch) > 0.05f) {
            be.syncAim();
            changed = true;
        }

        if (!be.armed || be.overheatCooldown > 0 || be.cooldown > 0 || target == null) {
            if (changed) {
                be.setChanged();
            }
            return;
        }

        if (!be.isAimedAt(desired)) {
            if (changed) {
                be.setChanged();
            }
            return;
        }

        if (be.kind.needsAmmo() && !be.kind.isValidAmmo(be.items.get(SLOT_AMMO))) {
            if (changed) {
                be.setChanged();
            }
            return;
        }

        if (!PhiPower.consumeTick(level, pos, be.kind.fuelCost())) {
            if (changed) {
                be.setChanged();
            }
            return;
        }

        if (be.kind.needsAmmo()) {
            be.items.get(SLOT_AMMO).shrink(1);
        }

        Vec3 dir = TurretAim.directionFromAngles(be.aimYaw, be.aimPitch);
        // Clear the muzzle past the barrel collision cell.
        Vec3 from = pivot.add(dir.scale(1.35));
        be.fireAt(server, from, dir, target);
        be.cooldown = be.kind.cooldownTicks();
        be.heat = Math.min(MAX_HEAT, be.heat + be.kind.heatPerShot());
        if (be.heat >= MAX_HEAT) {
            be.overheatCooldown = OVERHEAT_COOLDOWN;
            be.heat = MAX_HEAT / 2;
            server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.8f);
        }
        be.setChanged();
        be.syncAim();
    }

    private boolean isAimedAt(TurretAim.Angles desired) {
        return Math.abs(Mth.wrapDegrees(aimYaw - desired.yawDeg())) <= AIM_TOLERANCE_DEG
                && Math.abs(aimPitch - desired.pitchDeg()) <= AIM_TOLERANCE_DEG;
    }

    private void syncAim() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel server) {
        double r = kind.range();
        double r2 = r * r;
        BlockState state = getBlockState();
        Vec3 pivot = TurretAim.pivot(worldPosition, state);
        AABB box = new AABB(worldPosition).inflate(r);
        List<LivingEntity> list = server.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive()
                        && (e instanceof Monster || e instanceof Enemy)
                        && !(e instanceof Player)
                        && e.distanceToSqr(pivot) <= r2
                        && hasLos(server, e));
        return list.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(pivot)))
                .orElse(null);
    }

    private boolean hasLos(ServerLevel server, LivingEntity target) {
        BlockPos eye = BlockPos.containing(target.getEyePosition());
        BlockPos barrel = TurretAssembly.barrelPos(worldPosition, getBlockState());
        return PhiPower.hasLineOfSight(server, barrel, eye) || PhiPower.hasLineOfSight(server, worldPosition, eye);
    }

    private void fireAt(ServerLevel server, Vec3 from, Vec3 dir, LivingEntity target) {
        triggerBreech();
        switch (kind) {
            case PLASMA -> firePlasma(server, from, dir, target);
            case KINETIC -> fireKinetic(server, from, dir);
            case SPATIAL -> fireSpatial(server, target);
            case MENTAL -> fireMental(server, target);
            case OMEGA -> fireOmega(server, from, dir, target);
            default -> {}
        }
    }

    private void firePlasma(ServerLevel server, Vec3 from, Vec3 dir, LivingEntity target) {
        target.hurt(server.damageSources().magic(), 6f);
        target.igniteForSeconds(4);
        server.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.7f, 1.3f);
        for (int i = 0; i < 10; i++) {
            double t = i / 10.0;
            server.sendParticles(
                    ParticleTypes.FLAME,
                    from.x + dir.x * t * 2.5,
                    from.y + dir.y * t * 2.5,
                    from.z + dir.z * t * 2.5,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.01);
        }
    }

    private void fireKinetic(ServerLevel server, Vec3 from, Vec3 dir) {
        TurretBoltEntity bolt = new TurretBoltEntity(server, from.x, from.y, from.z, dir, 10f, kind.range());
        server.addFreshEntity(bolt);
        // Custom gauss crack + light metallic layer for body.
        server.playSound(null, worldPosition, ModSounds.KINETIC_GAUSS_SHOT.get(), SoundSource.BLOCKS, 1.05f, 1.0f);
        server.playSound(null, worldPosition, SoundEvents.TRIDENT_THROW.value(), SoundSource.BLOCKS, 0.35f, 0.55f);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 14, 0.05, 0.05, 0.05, 0.12);
        server.sendParticles(
                ParticleTypes.CRIT,
                from.x + dir.x,
                from.y + dir.y,
                from.z + dir.z,
                8,
                0.1,
                0.1,
                0.1,
                0.2);
    }

    private void fireSpatial(ServerLevel server, LivingEntity target) {
        target.hurt(server.damageSources().magic(), 20f);
        server.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.9f, 0.5f);
        server.sendParticles(
                ParticleTypes.PORTAL,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                40,
                0.4,
                0.6,
                0.4,
                0.2);
    }

    private void fireMental(ServerLevel server, LivingEntity target) {
        AABB cone = target.getBoundingBox().inflate(2.5);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, cone, LivingEntity::isAlive)) {
            if (e instanceof Player || !(e instanceof Monster || e instanceof Enemy)) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            e.setDeltaMovement(e.getDeltaMovement()
                    .add((e.getX() - worldPosition.getX()) * 0.15, 0.15, (e.getZ() - worldPosition.getZ()) * 0.15));
            e.hurtMarked = true;
        }
        server.playSound(null, worldPosition, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 0.45f, 1.6f);
    }

    private void fireOmega(ServerLevel server, Vec3 from, Vec3 dir, LivingEntity target) {
        TurretBoltEntity bolt = new TurretBoltEntity(server, from.x, from.y, from.z, dir, 8f, kind.range());
        bolt.setOmega(true);
        server.addFreshEntity(bolt);
        // Direct splash removed — damage comes from the bolt hit so ammo isn't double-spent visually.
        server.playSound(null, worldPosition, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.35f, 0.55f);
        server.sendParticles(ParticleTypes.SCULK_SOUL, from.x, from.y, from.z, 16, 0.2, 0.2, 0.2, 0.04);
    }

    @Override
    protected Component getDefaultName() {
        if (!kind.isEmitter()) {
            return Component.translatable("container.effecoria.turret_mount");
        }
        return Component.translatable("container.effecoria." + kind.getSerializedName() + "_turret");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new PhiTurretMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, i < list.size() ? list.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return kind.needsAmmo() ? AMMO_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return kind.needsAmmo() && index == SLOT_AMMO && kind.isValidAmmo(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return !kind.needsAmmo() || (index == SLOT_AMMO && kind.isValidAmmo(stack));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean("Armed", armed);
        tag.putInt("Heat", heat);
        tag.putInt("Cooldown", cooldown);
        tag.putInt("Overheat", overheatCooldown);
        tag.putString("Kind", kind.name());
        tag.putFloat("AimYaw", aimYaw);
        tag.putFloat("AimPitch", aimPitch);
        tag.putLong("BreechStart", breechStartTick);
        if (lastTargetId != null) {
            tag.putUUID("Target", lastTargetId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        armed = tag.getBoolean("Armed");
        heat = tag.getInt("Heat");
        cooldown = tag.getInt("Cooldown");
        overheatCooldown = tag.getInt("Overheat");
        try {
            kind = TurretKind.valueOf(tag.getString("Kind"));
        } catch (Exception ignored) {
            kind = TurretKind.NONE;
        }
        if (tag.contains("AimYaw")) {
            aimYaw = tag.getFloat("AimYaw");
            aimPitch = tag.getFloat("AimPitch");
            clientAimYaw = aimYaw;
            clientAimPitch = aimPitch;
            aimInitialized = true;
        }
        breechStartTick = tag.contains("BreechStart") ? tag.getLong("BreechStart") : -1L;
        if (tag.hasUUID("Target")) {
            lastTargetId = tag.getUUID("Target");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        clientAimYaw = aimYaw;
        clientAimPitch = aimPitch;
        loadAdditional(tag, lookup);
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.Connection net,
            ClientboundBlockEntityDataPacket pkt,
            HolderLookup.Provider lookup) {
        clientAimYaw = aimYaw;
        clientAimPitch = aimPitch;
        super.onDataPacket(net, pkt, lookup);
    }
}
