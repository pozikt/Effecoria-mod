package com.effecoria.block;

import com.effecoria.alchemy.menu.PhiCartographyMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Dedicated Φ-map desk — opens the full sonar cartography GUI. */
public final class PhiCartographyTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_LINKED = 0;
    public static final int DATA_SONAR_PRESENT = 1;
    public static final int DATA_SONAR_READY = 2;
    public static final int DATA_COUNT = 3;

    private int linked;
    private int sonarPresent;
    private int sonarReady;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LINKED -> linked;
                case DATA_SONAR_PRESENT -> sonarPresent;
                case DATA_SONAR_READY -> sonarReady;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_LINKED -> linked = value;
                case DATA_SONAR_PRESENT -> sonarPresent = value;
                case DATA_SONAR_READY -> sonarReady = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PhiCartographyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PHI_CARTOGRAPHY_TABLE.get(), pos, state);
    }

    public ContainerData dataAccess() {
        return data;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhiCartographyTableBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if ((level.getGameTime() + pos.asLong()) % 10L != 0L) {
            return;
        }
        var computer = TowerFacility.findComputer(server, pos).orElse(null);
        boolean live = computer != null && computer.consecrated() && computer.bound();
        be.linked = live ? 1 : 0;
        var sonarOpt = TowerFacility.findInComponent(server, pos, PhiSonarBlockEntity.class);
        be.sonarPresent = sonarOpt.isPresent() ? 1 : 0;
        boolean phi = live
                && (PhiPower.hasPower(server, pos)
                        || (computer != null && PhiPower.hasPower(server, computer.getBlockPos())));
        be.sonarReady = sonarOpt.filter(PhiSonarBlockEntity::ready).filter(s -> phi).isPresent() ? 1 : 0;
        be.setChanged();
    }

    public boolean linked() {
        return linked != 0;
    }

    public boolean sonarPresent() {
        return sonarPresent != 0;
    }

    public boolean sonarReady() {
        return sonarReady != 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.effecoria.phi_cartography_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PhiCartographyMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
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
