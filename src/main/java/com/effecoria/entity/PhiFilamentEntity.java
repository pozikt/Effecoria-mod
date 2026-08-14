package com.effecoria.entity;

import com.effecoria.content.ModEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.circuit.PhiFilamentLinks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Φ-filament: mithril nerve between two terminals. Not living — swords do nothing; shears cut it.
 */
public final class PhiFilamentEntity extends Entity {
    public static final int MAX_RANGE = 16;

    private static final EntityDataAccessor<BlockPos> DATA_A =
            SynchedEntityData.defineId(PhiFilamentEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_B =
            SynchedEntityData.defineId(PhiFilamentEntity.class, EntityDataSerializers.BLOCK_POS);

    private boolean linked;

    public PhiFilamentEntity(EntityType<? extends PhiFilamentEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.noCulling = true;
    }

    public PhiFilamentEntity(Level level, BlockPos a, BlockPos b) {
        this(ModEntities.PHI_FILAMENT.get(), level);
        setEnds(a, b);
        Vec3 mid = Vec3.atCenterOf(a).add(Vec3.atCenterOf(b)).scale(0.5);
        setPos(mid.x, mid.y, mid.z);
    }

    public void setEnds(BlockPos a, BlockPos b) {
        entityData.set(DATA_A, a.immutable());
        entityData.set(DATA_B, b.immutable());
        relink();
    }

    public BlockPos endA() {
        return entityData.get(DATA_A);
    }

    public BlockPos endB() {
        return entityData.get(DATA_B);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_A, BlockPos.ZERO);
        builder.define(DATA_B, BlockPos.ZERO);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        relink();
    }

    @Override
    public void onRemovedFromLevel() {
        unlink();
        super.onRemovedFromLevel();
    }

    private void relink() {
        unlink();
        BlockPos a = endA();
        BlockPos b = endB();
        if (!a.equals(BlockPos.ZERO) && !b.equals(BlockPos.ZERO)) {
            PhiFilamentLinks.bind(level(), a, b);
            linked = true;
        }
    }

    private void unlink() {
        if (!linked) {
            return;
        }
        PhiFilamentLinks.unbind(level(), endA(), endB());
        linked = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (tickCount % 20 != 0) {
            return;
        }
        if (!terminalOk(endA()) || !terminalOk(endB())) {
            dropAndDiscard();
        }
    }

    private boolean terminalOk(BlockPos pos) {
        if (!level().isLoaded(pos)) {
            return true;
        }
        return PhiBusNetwork.isConductor(level().getBlockState(pos))
                || level().getBlockEntity(pos) != null;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player player && player.getMainHandItem().is(Items.SHEARS)) {
            if (!level().isClientSide()) {
                dropAndDiscard();
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Player player && player.getMainHandItem().is(Items.SHEARS)) {
            if (!level().isClientSide()) {
                dropAndDiscard();
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS)) {
            if (!level().isClientSide()) {
                dropAndDiscard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    private void dropAndDiscard() {
        unlink();
        if (level() instanceof ServerLevel server) {
            spawnAtLocation(new ItemStack(ModItems.MITHRIL_WIRE.get()));
            server.playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("A")) {
            entityData.set(DATA_A, BlockPos.of(tag.getLong("A")));
        }
        if (tag.contains("B")) {
            entityData.set(DATA_B, BlockPos.of(tag.getLong("B")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("A", endA().asLong());
        tag.putLong("B", endB().asLong());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }

    public static boolean tooFar(BlockPos a, BlockPos b) {
        return a.distManhattan(b) > MAX_RANGE || a.equals(b);
    }
}
