package com.effecoria.block;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.WhisperingSpireService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps the spire registered and drives the continuous Φ-plasma column FX. */
public final class WhisperingSpireVentBlockEntity extends BlockEntity {
    private int tickCounter;

    public WhisperingSpireVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHISPERING_SPIRE_VENT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WhisperingSpireVentBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        WhisperingSpireService.register(server, pos);
        be.tickCounter++;
        if (be.tickCounter % 10 == 0) {
            be.spawnColumn(server, pos);
        }
        if (be.tickCounter % 80 == 0) {
            server.playSound(
                    null,
                    pos,
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS,
                    1.2f,
                    0.55f + server.random.nextFloat() * 0.15f);
        }
        if (be.tickCounter % 200 == 0) {
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9f, 0.35f);
        }
    }

    private void spawnColumn(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        double height = BalanceConfig.SPIRE_COLUMN_HEIGHT.get();
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        int count = 4;
        for (int i = 0; i < count; i++) {
            double y = pos.getY() + 1.0 + level.random.nextDouble() * height;
            if (y >= level.getMaxBuildHeight()) {
                continue;
            }
            level.sendParticles(
                    ModParticleTypes.ELEMENTAL_PLASMA.get(),
                    x + (level.random.nextDouble() - 0.5) * 0.6,
                    y,
                    z + (level.random.nextDouble() - 0.5) * 0.6,
                    1,
                    0.05,
                    0.2,
                    0.05,
                    0.01);
            if (level.random.nextBoolean()) {
                level.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        x,
                        y,
                        z,
                        1,
                        0.1,
                        0.15,
                        0.1,
                        0.02);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            WhisperingSpireService.unregister(server, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Ticks", tickCounter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickCounter = tag.getInt("Ticks");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
