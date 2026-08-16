package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiArtilleryMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.ArtilleryAssembly;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Siege Φ-artillery — manual yaw/pitch, thermal beam. */
public final class PhiArtilleryBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_COUNT = 0;

    public static final int DATA_FORMED = 0;
    public static final int DATA_YAW = 1;
    public static final int DATA_PITCH = 2;
    public static final int DATA_HEAT = 3;
    public static final int DATA_HOLD = 4;
    public static final int DATA_POWER = 5;
    public static final int DATA_FIRING = 6;
    public static final int DATA_REACH = 7;
    public static final int DATA_COUNT = 8;

    public static final int MAX_HEAT = 120;
    public static final int OVERHEAT_LOCK = 60;
    public static final int BEAM_RANGE = 128;
    public static final int POWER_LOAD = 100;
    public static final float MIN_FACTOR = 2.5f;
    public static final float YAW_STEP = 5f;
    public static final float PITCH_MIN = -10f;
    public static final float PITCH_MAX = 80f;
    /** Blocks melted / destroyed per firing tick along the ray. */
    public static final int MELT_BUDGET = 14;
    public static final float ENTITY_DAMAGE = 28f;
    /** Skip melt until this far from the muzzle so the barrel / pedestal never self-melts. */
    private static final double MELT_START = 2.75;

    private static final DustParticleOptions BEAM_DUST =
            new DustParticleOptions(new Vector3f(0.55f, 0.85f, 1.0f), 1.6f);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean formed;
    private float yaw;
    private float pitch = 10f;
    private int heat;
    private int overheatCooldown;
    private boolean holdFire;
    private boolean firingPulse;
    private boolean beamActive;
    /** Last fired beam length (blocks) — synced for client BER laser. */
    private float beamReach;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_YAW -> Math.round(yaw);
                case DATA_PITCH -> Math.round(pitch);
                case DATA_HEAT -> heat;
                case DATA_HOLD -> holdFire ? 1 : 0;
                case DATA_POWER -> Math.round(PhiPower.powerFactor(level, worldPosition) * 100f);
                case DATA_FIRING -> beamActive ? 1 : 0;
                case DATA_REACH -> Math.round(beamReach);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FORMED -> formed = value != 0;
                case DATA_YAW -> yaw = value;
                case DATA_PITCH -> pitch = value;
                case DATA_HEAT -> heat = value;
                case DATA_HOLD -> holdFire = value != 0;
                case DATA_FIRING -> beamActive = value != 0;
                case DATA_REACH -> beamReach = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiArtilleryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_ARTILLERY.get(), pos, state);
        Direction face = state.hasProperty(PhiArtilleryBaseBlock.FACING)
                ? state.getValue(PhiArtilleryBaseBlock.FACING)
                : Direction.NORTH;
        yaw = face.toYRot();
    }

    public ContainerData getData() {
        return data;
    }

    public void onFormed() {
        formed = true;
        setChanged();
    }

    public void onBroken() {
        formed = false;
        holdFire = false;
        firingPulse = false;
        beamActive = false;
        beamReach = 0;
        setChanged();
    }

    public boolean isFormed() {
        return formed || ArtilleryAssembly.isFormed(level, worldPosition);
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public boolean beamActive() {
        return beamActive
                || (getBlockState().hasProperty(PhiArtilleryBaseBlock.LIT)
                        && getBlockState().getValue(PhiArtilleryBaseBlock.LIT));
    }

    public float beamReach() {
        return beamReach;
    }

    public float powerFactorNow() {
        return level == null ? 0f : PhiPower.powerFactor(level, worldPosition);
    }

    public void nudgeYaw(float delta) {
        yaw = Mth.wrapDegrees(yaw + delta);
        setChanged();
        sync();
    }

    public void nudgePitch(float delta) {
        pitch = Mth.clamp(pitch + delta, PITCH_MIN, PITCH_MAX);
        setChanged();
        sync();
    }

    public void toggleHold() {
        holdFire = !holdFire;
        setChanged();
    }

    /** Immediate Fire pulse (GUI). */
    public FireResult tryFirePulse() {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel server)) {
            return FireResult.FAILED;
        }
        if (!ArtilleryAssembly.isFormed(level, worldPosition)) {
            formed = false;
            return FireResult.NOT_FORMED;
        }
        formed = true;
        if (overheatCooldown > 0) {
            return FireResult.OVERHEAT;
        }
        float factor = PhiPower.powerFactor(level, worldPosition);
        if (factor < MIN_FACTOR) {
            return FireResult.LOW_FACTOR;
        }
        if (!PhiPower.consumeTick(level, worldPosition, POWER_LOAD)) {
            return FireResult.NO_FUEL;
        }
        fireThermalBeam(server);
        heat = Math.min(MAX_HEAT, heat + 4);
        beamActive = true;
        setLit(true);
        if (heat >= MAX_HEAT) {
            overheatCooldown = OVERHEAT_LOCK;
            holdFire = false;
            heat = MAX_HEAT;
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.6f);
        }
        setChanged();
        sync();
        return FireResult.OK;
    }

    public enum FireResult {
        OK,
        NOT_FORMED,
        OVERHEAT,
        LOW_FACTOR,
        NO_FUEL,
        FAILED
    }

    public void firePulse() {
        firingPulse = true;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiArtilleryBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean assembled = ArtilleryAssembly.isFormed(level, pos);
        if (be.formed != assembled) {
            be.formed = assembled;
            be.setChanged();
        }
        if (!assembled) {
            be.holdFire = false;
            be.firingPulse = false;
            be.beamActive = false;
            be.beamReach = 0;
            be.setLit(false);
            return;
        }

        if (be.overheatCooldown > 0) {
            be.overheatCooldown--;
            be.holdFire = false;
            be.firingPulse = false;
            be.beamActive = false;
            be.setLit(false);
            be.setChanged();
            return;
        }

        boolean wantFire = be.firingPulse || be.holdFire;
        be.firingPulse = false;
        boolean lit = false;
        if (wantFire) {
            float factor = PhiPower.powerFactor(level, pos);
            if (factor >= MIN_FACTOR && PhiPower.consumeTick(level, pos, POWER_LOAD)) {
                be.fireThermalBeam(server);
                be.heat = Math.min(MAX_HEAT, be.heat + 4);
                lit = true;
                if (be.heat >= MAX_HEAT) {
                    be.overheatCooldown = OVERHEAT_LOCK;
                    be.holdFire = false;
                    be.heat = MAX_HEAT;
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.6f);
                }
            } else {
                be.holdFire = false;
                be.beamReach = 0;
            }
        } else if (be.heat > 0 && server.getGameTime() % 3 == 0) {
            be.heat--;
            be.beamReach = 0;
        }
        be.beamActive = lit;
        be.setLit(lit);
        be.setChanged();
        if (lit) {
            be.sync();
        }
    }

    private void setLit(boolean lit) {
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.is(com.effecoria.content.ModBlocks.PHI_ARTILLERY_BASE.get())
                && state.getValue(PhiArtilleryBaseBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(PhiArtilleryBaseBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private void fireThermalBeam(ServerLevel server) {
        Vec3 origin = muzzle();
        Vec3 dir = lookVector();

        double reach = BEAM_RANGE;
        int melted = 0;
        Set<BlockPos> touched = new HashSet<>();

        for (double d = 0.5; d <= BEAM_RANGE; d += 0.35) {
            Vec3 p = origin.add(dir.scale(d));
            BlockPos bp = BlockPos.containing(p);
            if (isGunProtected(bp)) {
                continue;
            }
            if (!ensureChunkLoaded(server, bp)) {
                reach = d;
                break;
            }
            BlockState state = server.getBlockState(bp);
            if (isGunBlock(state)) {
                continue;
            }
            boolean solid = !state.isAir() && !state.getCollisionShape(server, bp).isEmpty();
            boolean canMelt = d >= MELT_START;
            if (!solid) {
                if (canMelt && melted < MELT_BUDGET) {
                    melted += meltAround(server, bp, touched, MELT_BUDGET - melted);
                }
                continue;
            }
            if (isHardStop(state)) {
                reach = d;
                break;
            }
            if (canMelt && melted < MELT_BUDGET && touched.add(bp.immutable())) {
                if (meltBlock(server, bp)) {
                    melted++;
                    melted += meltAround(server, bp, touched, Math.min(3, MELT_BUDGET - melted));
                }
            }
            if (melted >= MELT_BUDGET) {
                reach = d;
                break;
            }
        }
        beamReach = (float) reach;

        AABB box = new AABB(origin, origin.add(dir.scale(reach))).inflate(1.1);
        List<LivingEntity> entities = server.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive);
        for (LivingEntity entity : entities) {
            Vec3 mid = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            Vec3 to = mid.subtract(origin);
            double along = to.dot(dir);
            if (along < 0 || along > reach) {
                continue;
            }
            Vec3 closest = origin.add(dir.scale(along));
            if (closest.distanceToSqr(mid) > 2.25) {
                continue;
            }
            entity.hurt(server.damageSources().inFire(), ENTITY_DAMAGE);
            entity.igniteForSeconds(16);
        }

        int steps = Math.max(8, (int) (reach * 1.5));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = origin.add(dir.scale(reach * t));
            server.sendParticles(BEAM_DUST, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0.0);
            if (i % 2 == 0) {
                server.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }
        server.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.1f, 0.75f);
        server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.35f, 1.8f);

        TowerFacility.findComputer(server, worldPosition)
                .ifPresent(anchor -> anchor.addOmegaPercent(2));
    }

    /**
     * Force-load the chunk containing {@code pos} so siege fire works past the player's
     * view distance. Truly permanent unload without load is impossible — we load on demand.
     */
    private static boolean ensureChunkLoaded(ServerLevel server, BlockPos pos) {
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        if (server.hasChunk(cx, cz)) {
            return true;
        }
        LevelChunk chunk = server.getChunkSource().getChunk(cx, cz, true);
        return chunk != null;
    }

    private static boolean isHardStop(BlockState state) {
        return state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY);
    }

    private static boolean isGunBlock(BlockState state) {
        return state.is(ModBlocks.PHI_ARTILLERY_BASE.get()) || state.is(ModBlocks.PHI_BEAM_LENS.get());
    }

    /** Pedestal + lens and a small halo — never melt our own assembly. */
    private boolean isGunProtected(BlockPos pos) {
        BlockPos base = worldPosition;
        BlockPos lens = worldPosition.above();
        if (pos.equals(base) || pos.equals(lens)) {
            return true;
        }
        int dx = Math.abs(pos.getX() - base.getX());
        int dy = Math.abs(pos.getY() - base.getY());
        int dz = Math.abs(pos.getZ() - base.getZ());
        return Math.max(dx, Math.max(dy, dz)) <= 1;
    }

    private int meltAround(ServerLevel server, BlockPos center, Set<BlockPos> touched, int budget) {
        if (budget <= 0) {
            return 0;
        }
        int used = 0;
        for (Direction dir : Direction.values()) {
            if (used >= budget) {
                break;
            }
            BlockPos n = center.relative(dir);
            if (isGunProtected(n) || !touched.add(n.immutable())) {
                continue;
            }
            if (!ensureChunkLoaded(server, n)) {
                continue;
            }
            BlockState st = server.getBlockState(n);
            if (st.isAir() || isHardStop(st) || isGunBlock(st)) {
                continue;
            }
            float hardness = st.getDestroySpeed(server, n);
            if (hardness < 0 || hardness > 8f) {
                continue;
            }
            if (meltBlock(server, n)) {
                used++;
            }
        }
        return used;
    }

    /** @return true if the block was changed / destroyed */
    private boolean meltBlock(ServerLevel server, BlockPos hit) {
        if (isGunProtected(hit)) {
            return false;
        }
        BlockState state = server.getBlockState(hit);
        if (state.isAir() || isGunBlock(state) || state.getDestroySpeed(server, hit) < 0 || isHardStop(state)) {
            return false;
        }
        // Obsidian / crying — slow crack under siege heat
        if (state.is(Blocks.OBSIDIAN)) {
            server.setBlock(hit, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.CRYING_OBSIDIAN)) {
            server.destroyBlock(hit, false);
            return true;
        }
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.FLOWERS)) {
            server.setBlock(hit, Blocks.FIRE.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GLASS)
                || state.is(Blocks.TINTED_GLASS)
                || state.is(BlockTags.ICE)
                || state.is(BlockTags.IMPERMEABLE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL)) {
            server.destroyBlock(hit, false);
            return true;
        }
        if (state.is(Blocks.WATER) || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            server.setBlock(hit, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.DRIPSTONE_BLOCK)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)) {
            server.setBlock(hit, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.NETHERRACK) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL)) {
            server.setBlock(hit, Blocks.LAVA.defaultBlockState(), 3);
            return true;
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            float hardness = state.getDestroySpeed(server, hit);
            if (hardness <= 1.5f) {
                server.destroyBlock(hit, false);
            } else if (hardness <= 5f) {
                server.setBlock(hit, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
            } else {
                server.setBlock(hit, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
            return true;
        }
        return false;
    }

    private Vec3 muzzle() {
        return Vec3.atCenterOf(worldPosition.above()).add(0, 0.35, 0);
    }

    private Vec3 lookVector() {
        float yr = yaw * ((float) Math.PI / 180f);
        float pr = pitch * ((float) Math.PI / 180f);
        float cosP = Mth.cos(pr);
        return new Vec3(-Mth.sin(yr) * cosP, -Mth.sin(pr), Mth.cos(yr) * cosP).normalize();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.phi_artillery");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new PhiArtilleryMenu(id, inv, this, data);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        items.clear();
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean("Formed", formed);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putInt("Heat", heat);
        tag.putInt("Overheat", overheatCooldown);
        tag.putBoolean("Hold", holdFire);
        tag.putFloat("BeamReach", beamReach);
        tag.putBoolean("BeamActive", beamActive);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        formed = tag.getBoolean("Formed");
        yaw = tag.getFloat("Yaw");
        pitch = tag.getFloat("Pitch");
        heat = tag.getInt("Heat");
        overheatCooldown = tag.getInt("Overheat");
        holdFire = tag.getBoolean("Hold");
        beamReach = tag.getFloat("BeamReach");
        beamActive = tag.getBoolean("BeamActive");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
}
