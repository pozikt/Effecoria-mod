package com.effecoria.core.tower;

import com.effecoria.block.FoundationAmuletBlockEntity;
import com.effecoria.block.OmegaDamperBlockEntity;
import com.effecoria.block.PhiAirSynthBlockEntity;
import com.effecoria.block.PhiCartographyTableBlockEntity;
import com.effecoria.block.PhiSonarBlockEntity;
import com.effecoria.block.PhiWaterPurifierBlockEntity;
import com.effecoria.block.RegenChamberBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.block.TowerConsoleBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.glue.EssenceGlueData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Queries the Φ-glued component that makes up a Mage Tower facility. */
public final class TowerFacility {
    private TowerFacility() {}

    /** One functional block row for the tower console monitor list. */
    public record MonitorEntry(String kind, int x, int y, int z, String status, int severity) {
        public static final int OK = 0;
        public static final int WARN = 1;
        public static final int BAD = 2;
        public static final int IDLE = 3;

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Kind", kind);
            tag.putInt("X", x);
            tag.putInt("Y", y);
            tag.putInt("Z", z);
            tag.putString("Status", status);
            tag.putInt("Sev", severity);
            return tag;
        }

        public static MonitorEntry load(CompoundTag tag) {
            return new MonitorEntry(
                    tag.getString("Kind"),
                    tag.getInt("X"),
                    tag.getInt("Y"),
                    tag.getInt("Z"),
                    tag.getString("Status"),
                    tag.getInt("Sev"));
        }

        public static ListTag saveList(List<MonitorEntry> entries) {
            ListTag list = new ListTag();
            for (MonitorEntry entry : entries) {
                list.add(entry.save());
            }
            return list;
        }

        public static List<MonitorEntry> loadList(ListTag list) {
            List<MonitorEntry> out = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                out.add(load(list.getCompound(i)));
            }
            return out;
        }
    }

    public static Optional<TowerAnchorBlockEntity> findComputer(ServerLevel level, BlockPos anyPosInComponent) {
        return findInComponent(level, anyPosInComponent, TowerAnchorBlockEntity.class);
    }

    public static Optional<FoundationAmuletBlockEntity> findChargedAmulet(
            ServerLevel level, BlockPos componentOrPos, @Nullable UUID ownerUuid) {
        for (BlockPos pos : EssenceGlueData.get(level).component(componentOrPos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FoundationAmuletBlockEntity amulet
                    && amulet.charged()
                    && (ownerUuid == null || ownerUuid.equals(amulet.ownerUuid()))) {
                return Optional.of(amulet);
            }
        }
        return Optional.empty();
    }

    public static boolean hasRegenChamber(ServerLevel level, BlockPos computerPos) {
        return findInComponent(level, computerPos, RegenChamberBlockEntity.class)
                .filter(RegenChamberBlockEntity::isOperational)
                .isPresent();
    }

    public static <T extends BlockEntity> Optional<T> findInComponent(
            ServerLevel level, BlockPos pos, Class<T> type) {
        for (BlockPos member : EssenceGlueData.get(level).component(pos)) {
            BlockEntity be = level.getBlockEntity(member);
            if (type.isInstance(be)) {
                return Optional.of(type.cast(be));
            }
        }
        return Optional.empty();
    }

    /** Scan every monitored functional block in the Φ-component. */
    public static List<MonitorEntry> listMonitors(ServerLevel level, BlockPos anyPosInComponent) {
        List<MonitorEntry> out = new ArrayList<>();
        TowerAnchorBlockEntity computer = findComputer(level, anyPosInComponent).orElse(null);
        boolean towerLive = computer != null && computer.consecrated() && computer.bound();
        boolean phi = computer != null && PhiPower.hasPower(level, computer.getBlockPos());

        for (BlockPos pos : EssenceGlueData.get(level).component(anyPosInComponent)) {
            BlockEntity be = level.getBlockEntity(pos);
            BlockState state = level.getBlockState(pos);
            if (be instanceof TowerAnchorBlockEntity anchor) {
                String status;
                int sev;
                if (!anchor.consecrated()) {
                    status = "unconsecrated";
                    sev = MonitorEntry.BAD;
                } else if (!anchor.bound()) {
                    status = "unbound";
                    sev = MonitorEntry.WARN;
                } else if (anchor.omegaPercent() >= 70) {
                    status = "omega_critical";
                    sev = MonitorEntry.BAD;
                } else if (anchor.omegaPercent() >= 35) {
                    status = "omega_high";
                    sev = MonitorEntry.WARN;
                } else {
                    status = "online";
                    sev = MonitorEntry.OK;
                }
                out.add(entry("computer", pos, status, sev));
            } else if (be instanceof FoundationAmuletBlockEntity amulet) {
                if (amulet.charged()) {
                    out.add(entry("amulet", pos, "charged", MonitorEntry.OK));
                } else {
                    out.add(entry("amulet", pos, "uncharged", MonitorEntry.BAD));
                }
            } else if (be instanceof OmegaDamperBlockEntity) {
                out.add(entry("damper", pos, towerLive ? "ready" : "idle", towerLive ? MonitorEntry.OK : MonitorEntry.IDLE));
            } else if (be instanceof PhiAirSynthBlockEntity) {
                out.add(lifeEntry("air_synth", pos, towerLive, phi));
            } else if (be instanceof PhiWaterPurifierBlockEntity) {
                out.add(lifeEntry("water_purifier", pos, towerLive, phi));
            } else if (be instanceof RegenChamberBlockEntity regen) {
                if (!regen.isFormed()) {
                    out.add(entry("regen_chamber", pos, "unformed", MonitorEntry.BAD));
                } else if (!regen.isFull()) {
                    out.add(entry("regen_chamber", pos, "filling", MonitorEntry.WARN));
                } else if (!towerLive) {
                    out.add(entry("regen_chamber", pos, "tower_offline", MonitorEntry.IDLE));
                } else if (!phi) {
                    out.add(entry("regen_chamber", pos, "no_power", MonitorEntry.BAD));
                } else {
                    out.add(entry("regen_chamber", pos, "active", MonitorEntry.OK));
                }
            } else if (be instanceof TowerConsoleBlockEntity) {
                out.add(entry("console", pos, "online", MonitorEntry.OK));
            } else if (be instanceof PhiSonarBlockEntity sonar) {
                if (!towerLive) {
                    out.add(entry("sonar", pos, "tower_offline", MonitorEntry.IDLE));
                } else if (!phi) {
                    out.add(entry("sonar", pos, "no_power", MonitorEntry.BAD));
                } else if (!sonar.ready()) {
                    out.add(entry("sonar", pos, "cooldown", MonitorEntry.WARN));
                } else {
                    out.add(entry("sonar", pos, "ready", MonitorEntry.OK));
                }
            } else if (be instanceof PhiCartographyTableBlockEntity) {
                out.add(entry("cartography", pos, towerLive ? "online" : "idle", towerLive ? MonitorEntry.OK : MonitorEntry.IDLE));
            } else if (state.is(ModBlocks.SPARK_REACTOR.get())) {
                out.add(reactorEntry("spark_reactor", pos, PhiPower.hasPower(level, pos)));
            } else if (state.is(ModBlocks.HEART_REACTOR_CORE.get())) {
                out.add(reactorEntry("heart_reactor", pos, PhiPower.hasPower(level, pos)));
            } else if (state.is(ModBlocks.FORGE_REACTOR_CORE.get())) {
                out.add(reactorEntry("forge_reactor", pos, PhiPower.hasPower(level, pos)));
            }
        }

        out.sort(Comparator
                .comparingInt(TowerFacility::kindOrder)
                .thenComparingInt(MonitorEntry::y)
                .thenComparingInt(MonitorEntry::x)
                .thenComparingInt(MonitorEntry::z));
        return out;
    }

    private static MonitorEntry lifeEntry(String kind, BlockPos pos, boolean towerLive, boolean phi) {
        if (!towerLive) {
            return entry(kind, pos, "tower_offline", MonitorEntry.IDLE);
        }
        if (!phi) {
            return entry(kind, pos, "no_power", MonitorEntry.BAD);
        }
        return entry(kind, pos, "active", MonitorEntry.OK);
    }

    private static MonitorEntry reactorEntry(String kind, BlockPos pos, boolean powered) {
        return powered
                ? entry(kind, pos, "powered", MonitorEntry.OK)
                : entry(kind, pos, "offline", MonitorEntry.WARN);
    }

    private static MonitorEntry entry(String kind, BlockPos pos, String status, int severity) {
        return new MonitorEntry(kind, pos.getX(), pos.getY(), pos.getZ(), status, severity);
    }

    private static int kindOrder(MonitorEntry entry) {
        return switch (entry.kind()) {
            case "computer" -> 0;
            case "amulet" -> 1;
            case "console" -> 2;
            case "damper" -> 3;
            case "air_synth" -> 4;
            case "water_purifier" -> 5;
            case "regen_chamber" -> 6;
            case "sonar" -> 7;
            case "cartography" -> 8;
            case "spark_reactor", "heart_reactor", "forge_reactor" -> 9;
            default -> 50;
        };
    }

    public static ListTag saveMonitorList(List<MonitorEntry> entries) {
        return MonitorEntry.saveList(entries);
    }

    public static List<MonitorEntry> loadMonitorList(@Nullable ListTag list) {
        if (list == null) {
            return List.of();
        }
        return MonitorEntry.loadList(list);
    }
}
