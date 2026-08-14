package com.effecoria.block;

import com.effecoria.alchemy.menu.TowerConsoleMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.tower.TowerBodyType;
import com.effecoria.core.tower.TowerFacility;
import com.effecoria.core.tower.TowerStructureValidator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Syncs tower telemetry for the Control Console GUI. */
public final class TowerConsoleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_INTEGRITY = 0;
    public static final int DATA_OMEGA = 1;
    public static final int DATA_DOME_POWERED = 2;
    public static final int DATA_DOME_COMBAT = 3;
    public static final int DATA_BODY = 4;
    public static final int DATA_AMULET = 5;
    public static final int DATA_BOUND = 6;
    public static final int DATA_CONSECRATED = 7;
    public static final int DATA_LINKED = 8;
    public static final int DATA_CELLS = 9;
    public static final int DATA_PRESENT = 10;
    public static final int DATA_VERTICALITY = 11;
    public static final int DATA_SCATTER = 12;
    public static final int DATA_REACTOR = 13;
    public static final int DATA_REVIVES = 14;
    public static final int DATA_PHI_POWER = 15;
    public static final int DATA_SONAR_PRESENT = 16;
    public static final int DATA_SONAR_READY = 17;
    public static final int DATA_PHOENIX = 18;
    public static final int DATA_COUNT = 19;

    private int integrityPct;
    private int omegaPct;
    private int domePowered;
    private int domeCombat;
    private int bodyOrdinal;
    private int amuletCharged;
    private int bound;
    private int consecrated;
    private int linked;
    private int gluedCells;
    private int presentBlocks;
    private int verticalityCentis;
    private int scatterCentis;
    private int reactorOrdinal;
    private int reviveCount;
    private int phiPower;
    private int sonarPresent;
    private int sonarReady;
    private int phoenixEdict;

    private List<TowerFacility.MonitorEntry> monitors = new ArrayList<>();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_INTEGRITY -> integrityPct;
                case DATA_OMEGA -> omegaPct;
                case DATA_DOME_POWERED -> domePowered;
                case DATA_DOME_COMBAT -> domeCombat;
                case DATA_BODY -> bodyOrdinal;
                case DATA_AMULET -> amuletCharged;
                case DATA_BOUND -> bound;
                case DATA_CONSECRATED -> consecrated;
                case DATA_LINKED -> linked;
                case DATA_CELLS -> gluedCells;
                case DATA_PRESENT -> presentBlocks;
                case DATA_VERTICALITY -> verticalityCentis;
                case DATA_SCATTER -> scatterCentis;
                case DATA_REACTOR -> reactorOrdinal;
                case DATA_REVIVES -> reviveCount;
                case DATA_PHI_POWER -> phiPower;
                case DATA_SONAR_PRESENT -> sonarPresent;
                case DATA_SONAR_READY -> sonarReady;
                case DATA_PHOENIX -> phoenixEdict;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_INTEGRITY -> integrityPct = value;
                case DATA_OMEGA -> omegaPct = value;
                case DATA_DOME_POWERED -> domePowered = value;
                case DATA_DOME_COMBAT -> domeCombat = value;
                case DATA_BODY -> bodyOrdinal = value;
                case DATA_AMULET -> amuletCharged = value;
                case DATA_BOUND -> bound = value;
                case DATA_CONSECRATED -> consecrated = value;
                case DATA_LINKED -> linked = value;
                case DATA_CELLS -> gluedCells = value;
                case DATA_PRESENT -> presentBlocks = value;
                case DATA_VERTICALITY -> verticalityCentis = value;
                case DATA_SCATTER -> scatterCentis = value;
                case DATA_REACTOR -> reactorOrdinal = value;
                case DATA_REVIVES -> reviveCount = value;
                case DATA_PHI_POWER -> phiPower = value;
                case DATA_SONAR_PRESENT -> sonarPresent = value;
                case DATA_SONAR_READY -> sonarReady = value;
                case DATA_PHOENIX -> phoenixEdict = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public TowerConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER_CONSOLE.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public List<TowerFacility.MonitorEntry> monitors() {
        return Collections.unmodifiableList(monitors);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TowerConsoleBlockEntity be) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 10 != 0) {
            return;
        }
        be.refreshTelemetry(server);
    }

    public void refreshTelemetry(ServerLevel level) {
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(level, worldPosition).orElse(null);
        if (computer == null) {
            clearTelemetry();
            monitors = List.of();
            syncClient();
            return;
        }
        computer.refreshIntegrity(level);
        linked = 1;
        consecrated = computer.consecrated() ? 1 : 0;
        integrityPct = (int) Math.round(computer.integrity() * 100.0);
        omegaPct = computer.omegaPercent();
        domePowered = computer.domePowered() ? 1 : 0;
        domeCombat = computer.domeCombat() ? 1 : 0;
        bodyOrdinal = computer.bodyType().ordinal();
        bound = computer.bound() ? 1 : 0;
        gluedCells = computer.gluedCells();
        presentBlocks = computer.presentBlocks();
        verticalityCentis = (int) Math.round(computer.verticality() * 100.0);
        scatterCentis = (int) Math.round(computer.phiScatter() * 100.0);
        reactorOrdinal = reactorOrdinal(computer.reactorClass());
        reviveCount = computer.reviveCount();
        phiPower = PhiPower.hasPower(level, computer.getBlockPos()) ? 1 : 0;
        amuletCharged =
                TowerFacility.findChargedAmulet(level, worldPosition, computer.ownerUuid()).isPresent() ? 1 : 0;
        monitors = TowerFacility.listMonitors(level, worldPosition);
        var sonarOpt = TowerFacility.findInComponent(level, worldPosition, com.effecoria.block.PhiSonarBlockEntity.class);
        sonarPresent = sonarOpt.isPresent() ? 1 : 0;
        sonarReady = sonarOpt.filter(com.effecoria.block.PhiSonarBlockEntity::ready)
                        .filter(s -> phiPower != 0 && consecrated != 0 && bound != 0)
                        .isPresent()
                ? 1
                : 0;
        phoenixEdict = computer.phoenixEdictEnabled() ? 1 : 0;
        setChanged();
        syncClient();
    }

    private void clearTelemetry() {
        integrityPct = 0;
        omegaPct = 0;
        domePowered = 0;
        domeCombat = 0;
        bodyOrdinal = 0;
        amuletCharged = 0;
        bound = 0;
        consecrated = 0;
        linked = 0;
        gluedCells = 0;
        presentBlocks = 0;
        verticalityCentis = 0;
        scatterCentis = 100;
        reactorOrdinal = 0;
        reviveCount = 0;
        phiPower = 0;
        sonarPresent = 0;
        sonarReady = 0;
        phoenixEdict = 1;
    }

    private void syncClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static int reactorOrdinal(String raw) {
        if (raw == null || raw.isEmpty()) {
            return TowerStructureValidator.ReactorClass.NONE.ordinal();
        }
        return switch (raw.toLowerCase()) {
            case "spark" -> TowerStructureValidator.ReactorClass.SPARK.ordinal();
            case "heart" -> TowerStructureValidator.ReactorClass.HEART.ordinal();
            case "forge" -> TowerStructureValidator.ReactorClass.FORGE.ordinal();
            default -> TowerStructureValidator.ReactorClass.NONE.ordinal();
        };
    }

    public boolean tryToggleDome(Player player) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return false;
        }
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(server, worldPosition).orElse(null);
        if (computer == null || !computer.bound() || computer.ownerUuid() == null) {
            return false;
        }
        if (!computer.ownerUuid().equals(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.effecoria.tower.not_owner"), true);
            return false;
        }
        boolean combat = computer.toggleDomeCombat();
        sp.displayClientMessage(
                Component.translatable(
                        combat
                                ? "message.effecoria.tower.dome_combat_on"
                                : "message.effecoria.tower.dome_combat_off"),
                true);
        refreshTelemetry(server);
        return true;
    }

    public boolean tryCycleBody(Player player) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return false;
        }
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(server, worldPosition).orElse(null);
        if (computer == null || !computer.consecrated()) {
            return false;
        }
        if (computer.ownerUuid() != null && !computer.ownerUuid().equals(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.effecoria.tower.not_owner"), true);
            return false;
        }
        computer.cycleBodyType();
        if (computer.ownerUuid() != null && computer.ownerUuid().equals(sp.getUUID())) {
            var data = PsiHelper.get(sp);
            data.setPreferredBodyType(computer.bodyType());
            PsiHelper.set(sp, data);
        }
        sp.displayClientMessage(
                Component.translatable("message.effecoria.tower.body_cycle", computer.bodyType().getSerializedName()),
                true);
        refreshTelemetry(server);
        return true;
    }

    public boolean tryTogglePhoenix(Player player) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return false;
        }
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(server, worldPosition).orElse(null);
        if (computer == null || !computer.bound() || computer.ownerUuid() == null) {
            return false;
        }
        if (!computer.ownerUuid().equals(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.effecoria.tower.not_owner"), true);
            return false;
        }
        boolean on = computer.togglePhoenixEdict();
        sp.displayClientMessage(
                Component.translatable(
                        on
                                ? "message.effecoria.tower.phoenix_on"
                                : "message.effecoria.tower.phoenix_off"),
                true);
        refreshTelemetry(server);
        return true;
    }

    public TowerBodyType bodyType() {
        TowerBodyType[] values = TowerBodyType.values();
        int i = Math.max(0, Math.min(values.length - 1, bodyOrdinal));
        return values[i];
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.effecoria.tower_console");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (level instanceof ServerLevel server) {
            refreshTelemetry(server);
        }
        return new TowerConsoleMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeTelemetry(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readTelemetry(tag);
    }

    private void writeTelemetry(CompoundTag tag) {
        tag.putInt("Integrity", integrityPct);
        tag.putInt("Omega", omegaPct);
        tag.putInt("DomePowered", domePowered);
        tag.putInt("DomeCombat", domeCombat);
        tag.putInt("Body", bodyOrdinal);
        tag.putInt("Amulet", amuletCharged);
        tag.putInt("Bound", bound);
        tag.putInt("Consecrated", consecrated);
        tag.putInt("Linked", linked);
        tag.putInt("Cells", gluedCells);
        tag.putInt("Present", presentBlocks);
        tag.putInt("Vert", verticalityCentis);
        tag.putInt("Scatter", scatterCentis);
        tag.putInt("Reactor", reactorOrdinal);
        tag.putInt("Revives", reviveCount);
        tag.putInt("PhiPower", phiPower);
        tag.putInt("SonarPresent", sonarPresent);
        tag.putInt("SonarReady", sonarReady);
        tag.putInt("Phoenix", phoenixEdict);
        tag.put("Monitors", TowerFacility.saveMonitorList(monitors));
    }

    private void readTelemetry(CompoundTag tag) {
        integrityPct = tag.getInt("Integrity");
        omegaPct = tag.getInt("Omega");
        domePowered = tag.getInt("DomePowered");
        domeCombat = tag.getInt("DomeCombat");
        bodyOrdinal = tag.getInt("Body");
        amuletCharged = tag.getInt("Amulet");
        bound = tag.getInt("Bound");
        consecrated = tag.getInt("Consecrated");
        linked = tag.getInt("Linked");
        gluedCells = tag.getInt("Cells");
        presentBlocks = tag.getInt("Present");
        verticalityCentis = tag.getInt("Vert");
        scatterCentis = tag.contains("Scatter") ? tag.getInt("Scatter") : 100;
        reactorOrdinal = tag.getInt("Reactor");
        reviveCount = tag.getInt("Revives");
        phiPower = tag.getInt("PhiPower");
        sonarPresent = tag.getInt("SonarPresent");
        sonarReady = tag.getInt("SonarReady");
        phoenixEdict = tag.contains("Phoenix") ? tag.getInt("Phoenix") : 1;
        if (tag.contains("Monitors", Tag.TAG_LIST)) {
            monitors = new ArrayList<>(TowerFacility.loadMonitorList(tag.getList("Monitors", Tag.TAG_COMPOUND)));
        } else {
            monitors = List.of();
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        writeTelemetry(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readTelemetry(tag);
    }
}
