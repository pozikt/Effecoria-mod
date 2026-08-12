package com.effecoria.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.effecoria.alchemy.menu.PortalModulatorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiBeaconIndex;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.alchemy.PortalFrameFinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

/** Opens / sustains a hyper-tunnel film inside an adjacent mithril frame. */
public final class PortalModulatorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_OPEN = 0;
    public static final int DATA_MODE = 1; // 0=coords, 1=beacon
    public static final int DATA_TARGET_X = 2;
    public static final int DATA_TARGET_Y = 3;
    public static final int DATA_TARGET_Z = 4;
    public static final int DATA_FRAME_OK = 5;
    public static final int DATA_POWER_CENTI = 6;
    public static final int DATA_COOLED = 7;
    public static final int DATA_OVERHEAT = 8;
    public static final int DATA_INTERIOR = 9;
    public static final int DATA_COUNT = 10;

    public static final float MIN_POWER_FACTOR = 1.0f; // Heart-tier
    public static final int BASE_SUSTAIN_COST = 8;
    public static final int OVERHEAT_MAX = 200;

    private boolean open;
    private int mode; // 0 coords, 1 beacon
    private int targetX;
    private int targetY = 64;
    private int targetZ;
    private String beaconName = "";
    private int overheat;
    private final List<BlockPos> filmCells = new ArrayList<>();
    private final List<BlockPos> frameCells = new ArrayList<>();
    private final Map<UUID, Integer> teleportCooldown = new HashMap<>();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_OPEN -> open ? 1 : 0;
                case DATA_MODE -> mode;
                case DATA_TARGET_X -> targetX;
                case DATA_TARGET_Y -> targetY;
                case DATA_TARGET_Z -> targetZ;
                case DATA_FRAME_OK -> frameOkPreview() ? 1 : 0;
                case DATA_POWER_CENTI -> Math.round(PhiPower.powerFactor(level, worldPosition) * 100f);
                case DATA_COOLED -> hasCoolant() ? 1 : 0;
                case DATA_OVERHEAT -> overheat;
                case DATA_INTERIOR -> filmCells.size();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_MODE -> mode = value == 1 ? 1 : 0;
                case DATA_TARGET_X -> targetX = value;
                case DATA_TARGET_Y -> targetY = value;
                case DATA_TARGET_Z -> targetZ = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PortalModulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTAL_MODULATOR.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isOpen() {
        return open;
    }

    public int mode() {
        return mode;
    }

    public String beaconName() {
        return beaconName;
    }

    public void setMode(int mode) {
        this.mode = mode == 1 ? 1 : 0;
        setChanged();
    }

    public void setCoords(int x, int y, int z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        setChanged();
    }

    public void setBeaconName(String name) {
        this.beaconName = name == null ? "" : name.trim();
        setChanged();
    }

    public boolean ownsFrameCell(BlockPos pos) {
        return frameCells.contains(pos);
    }

    private boolean frameOkPreview() {
        if (level == null) {
            return false;
        }
        return PortalFrameFinder.find(level, worldPosition) != null;
    }

    public boolean tryOpen(Player player) {
        if (level == null || level.isClientSide() || open) {
            return false;
        }
        PortalFrameFinder.FrameShape shape = PortalFrameFinder.find(level, worldPosition);
        if (shape == null) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.portal_no_frame"), true);
            }
            return false;
        }
        float factor = PhiPower.powerFactor(level, worldPosition);
        if (factor < MIN_POWER_FACTOR) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.portal_no_power"), true);
            }
            return false;
        }
        if (!hasCoolant()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.portal_no_coolant"), true);
            }
            return false;
        }
        BlockPos dest = resolveDestination();
        if (dest == null) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.portal_no_dest"), true);
            }
            return false;
        }

        frameCells.clear();
        frameCells.addAll(shape.frameCells());
        filmCells.clear();
        for (BlockPos cell : shape.interiorCells()) {
            BlockState cur = level.getBlockState(cell);
            if (!cur.isAir() && !cur.canBeReplaced() && !cur.is(ModBlocks.PORTAL_GATE.get())) {
                forceClose();
                return false;
            }
            level.setBlock(cell, ModBlocks.PORTAL_GATE.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(cell) instanceof PortalGateBlockEntity film) {
                film.bindModulator(worldPosition);
            }
            filmCells.add(cell.immutable());
        }
        open = true;
        overheat = 0;
        syncLit(true);
        playOpenImpulse((ServerLevel) level);
        setChanged();
        return true;
    }

    /** Visual/audio shockwave when the hyper-tunnel membrane snaps open. */
    private void playOpenImpulse(ServerLevel server) {
        double cx = 0;
        double cy = 0;
        double cz = 0;
        int n = filmCells.size();
        if (n == 0) {
            cx = worldPosition.getX() + 0.5;
            cy = worldPosition.getY() + 0.5;
            cz = worldPosition.getZ() + 0.5;
        } else {
            for (BlockPos p : filmCells) {
                cx += p.getX() + 0.5;
                cy += p.getY() + 0.5;
                cz += p.getZ() + 0.5;
            }
            cx /= n;
            cy /= n;
            cz /= n;
        }

        server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 0.85f);
        server.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.55f, 1.35f);
        server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, 0.6f);

        // Core flash
        server.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0, 0, 0, 0);

        // Membrane cells ignite
        for (BlockPos p : filmCells) {
            double x = p.getX() + 0.5;
            double y = p.getY() + 0.5;
            double z = p.getZ() + 0.5;
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 18, 0.35, 0.35, 0.35, 0.08);
            server.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.25, 0.25, 0.25, 0.04);
            server.sendParticles(ParticleTypes.ENCHANT, x, y, z, 10, 0.4, 0.4, 0.4, 0.5);
        }

        // Expanding radial impulse in the plane of the portal
        for (int i = 0; i < 48; i++) {
            double ang = (Math.PI * 2.0 * i) / 48.0;
            double rx = Math.cos(ang);
            double rz = Math.sin(ang);
            for (int ring = 1; ring <= 4; ring++) {
                double d = ring * 0.55;
                server.sendParticles(
                        ParticleTypes.END_ROD,
                        cx + rx * d,
                        cy,
                        cz + rz * d,
                        1,
                        0,
                        0.05,
                        0,
                        0.01);
                if (ring == 2 || ring == 4) {
                    server.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            cx + rx * d * 0.85,
                            cy + (i % 2) * 0.1,
                            cz + rz * d * 0.85,
                            1,
                            0,
                            0.02,
                            0,
                            0);
                }
            }
        }
    }

    public void forceClose() {
        if (level == null || level.isClientSide()) {
            open = false;
            filmCells.clear();
            frameCells.clear();
            return;
        }
        for (BlockPos cell : filmCells) {
            if (level.getBlockState(cell).is(ModBlocks.PORTAL_GATE.get())) {
                level.removeBlock(cell, false);
            }
        }
        if (level instanceof ServerLevel server && !filmCells.isEmpty()) {
            double cx = 0, cy = 0, cz = 0;
            for (BlockPos p : filmCells) {
                cx += p.getX() + 0.5;
                cy += p.getY() + 0.5;
                cz += p.getZ() + 0.5;
            }
            cx /= filmCells.size();
            cy /= filmCells.size();
            cz /= filmCells.size();
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, cx, cy, cz, 24, 0.6, 0.6, 0.6, 0.05);
            server.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 12, 0.4, 0.4, 0.4, 0.02);
        }
        filmCells.clear();
        frameCells.clear();
        open = false;
        syncLit(false);
        level.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.55f, 0.9f);
        level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.35f, 1.4f);
        setChanged();
    }

    @Nullable
    public BlockPos resolveDestination() {
        if (level == null) {
            return null;
        }
        if (mode == 1) {
            return PhiBeaconIndex.find(level.dimension(), beaconName)
                    .map(this::safeStandNear)
                    .orElse(null);
        }
        return safeStandAt(new BlockPos(targetX, targetY, targetZ));
    }

    private BlockPos safeStandNear(BlockPos beacon) {
        for (Direction d : new Direction[] {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        }) {
            BlockPos candidate = beacon.relative(d);
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return safeStandAt(beacon.above());
    }

    private BlockPos safeStandAt(BlockPos pos) {
        if (level == null) {
            return pos;
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 8; i++) {
            if (level.getBlockState(cursor).isAir() && level.getBlockState(cursor.above()).isAir()) {
                return cursor.immutable();
            }
            cursor.move(Direction.UP);
        }
        return pos;
    }

    private boolean hasCoolant() {
        if (level == null) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int cheb = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
                    if (cheb < 1 || cheb > 2) {
                        continue;
                    }
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    FluidState fluid = level.getFluidState(cursor);
                    if (state.is(Blocks.ICE)
                            || state.is(Blocks.PACKED_ICE)
                            || state.is(Blocks.BLUE_ICE)
                            || state.is(Blocks.WATER)
                            || state.is(BlockTags.ICE)
                            || fluid.is(FluidTags.WATER)
                            || state.is(ModBlocks.PHI_WATER.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void syncLit(boolean lit) {
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(PortalModulatorBlock.LIT) && state.getValue(PortalModulatorBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(PortalModulatorBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    public boolean isPlayerOnCooldown(Player player) {
        return teleportCooldown.getOrDefault(player.getUUID(), 0) > 0;
    }

    public void teleportPlayer(ServerLevel from, ServerPlayer player) {
        BlockPos dest = resolveDestination();
        if (dest == null) {
            return;
        }
        teleportCooldown.put(player.getUUID(), 60);
        player.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        from.playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.7f, 1.0f);
        from.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.7f, 1.2f);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalModulatorBlockEntity be) {
        if (!be.teleportCooldown.isEmpty()) {
            be.teleportCooldown.entrySet().removeIf(e -> {
                int n = e.getValue() - 1;
                if (n <= 0) {
                    return true;
                }
                e.setValue(n);
                return false;
            });
        }
        if (!be.open) {
            return;
        }
        int cost = BASE_SUSTAIN_COST + Math.max(0, be.filmCells.size() / 4);
        if (!PhiPower.consumeTick(level, pos, cost) || PhiPower.powerFactor(level, pos) < MIN_POWER_FACTOR) {
            be.forceClose();
            return;
        }
        if (!be.hasCoolant()) {
            be.overheat++;
            if (be.overheat >= OVERHEAT_MAX) {
                be.forceClose();
                return;
            }
        } else if (be.overheat > 0) {
            be.overheat = Math.max(0, be.overheat - 2);
        }
        if (level.getGameTime() % 40L == 0L) {
            if (PortalFrameFinder.find(level, pos) == null) {
                be.forceClose();
            }
        }
        be.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.effecoria.portal_modulator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PortalModulatorMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Open", open);
        tag.putInt("Mode", mode);
        tag.putInt("TX", targetX);
        tag.putInt("TY", targetY);
        tag.putInt("TZ", targetZ);
        tag.putString("Beacon", beaconName);
        tag.putInt("Overheat", overheat);
        ListTag films = new ListTag();
        for (BlockPos p : filmCells) {
            films.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Films", films);
        ListTag frames = new ListTag();
        for (BlockPos p : frameCells) {
            frames.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Frames", frames);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        open = tag.getBoolean("Open");
        mode = tag.getInt("Mode") == 1 ? 1 : 0;
        targetX = tag.getInt("TX");
        targetY = tag.getInt("TY");
        targetZ = tag.getInt("TZ");
        beaconName = tag.getString("Beacon");
        overheat = tag.getInt("Overheat");
        filmCells.clear();
        if (tag.contains("Films", Tag.TAG_LIST)) {
            ListTag films = tag.getList("Films", Tag.TAG_LONG);
            for (Tag t : films) {
                if (t instanceof LongTag lt) {
                    filmCells.add(BlockPos.of(lt.getAsLong()));
                }
            }
        }
        frameCells.clear();
        if (tag.contains("Frames", Tag.TAG_LIST)) {
            ListTag frames = tag.getList("Frames", Tag.TAG_LONG);
            for (Tag t : frames) {
                if (t instanceof LongTag lt) {
                    frameCells.add(BlockPos.of(lt.getAsLong()));
                }
            }
        }
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
