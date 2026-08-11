package com.effecoria.block;

import com.effecoria.alchemy.menu.ClimateArrayMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.world.weather.PhiWeatherKind;
import com.effecoria.world.weather.PhiWeatherService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Climate Array BE — cycles dew/mist/rain and fires local Φ-weather events. */
public final class ClimateArrayBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_MODE = 0;
    public static final int DATA_COOLDOWN = 1;
    public static final int DATA_COUNT = 2;

    public static final int ACTIVATE_COOLDOWN = 200;
    public static final int ACTIVATE_POWER_COST = 40;
    public static final double EVENT_RADIUS = 48.0;
    public static final long EVENT_DURATION = 6000L;
    public static final float EVENT_INTENSITY = 0.85f;

    private static final PhiWeatherKind[] MODES = {
        PhiWeatherKind.ESSENCE_DEW, PhiWeatherKind.ESSENCE_MIST, PhiWeatherKind.ESSENCE_RAIN
    };

    private int modeIndex;
    private int cooldown;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_MODE -> modeIndex;
                case DATA_COOLDOWN -> cooldown;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_MODE -> modeIndex = Mth.clamp(value, 0, MODES.length - 1);
                case DATA_COOLDOWN -> cooldown = Math.max(0, value);
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ClimateArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLIMATE_ARRAY.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public PhiWeatherKind mode() {
        return MODES[Mth.clamp(modeIndex, 0, MODES.length - 1)];
    }

    public int modeIndex() {
        return modeIndex;
    }

    public int cooldown() {
        return cooldown;
    }

    public void cycleMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        setChanged();
    }

    public boolean tryActivate(Player player) {
        if (cooldown > 0 || level == null || level.isClientSide()) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (!PhiPower.consumeTick(level, worldPosition, ACTIVATE_POWER_COST)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.effecoria.climate_array_no_power"), true);
            }
            return false;
        }
        boolean started = PhiWeatherService.startLocalEvent(
                server, mode(), worldPosition, EVENT_RADIUS, EVENT_DURATION, EVENT_INTENSITY);
        if (!started) {
            return false;
        }
        cooldown = ACTIVATE_COOLDOWN;
        syncLit();
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.55f, 1.35f);
        setChanged();
        return true;
    }

    private void syncLit() {
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(worldPosition);
        if (!state.hasProperty(ClimateArrayBlock.LIT)) {
            return;
        }
        boolean lit = cooldown > 0;
        if (state.getValue(ClimateArrayBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(ClimateArrayBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ClimateArrayBlockEntity be) {
        if (be.cooldown > 0) {
            be.cooldown--;
            if (be.cooldown % 20 == 0 || be.cooldown == 0) {
                be.setChanged();
            }
            be.syncLit();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.effecoria.climate_array");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ClimateArrayMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Mode", modeIndex);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        modeIndex = Mth.clamp(tag.getInt("Mode"), 0, MODES.length - 1);
        cooldown = Math.max(0, tag.getInt("Cooldown"));
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
