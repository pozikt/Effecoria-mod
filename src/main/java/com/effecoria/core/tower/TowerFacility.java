package com.effecoria.core.tower;

import com.effecoria.block.FoundationAmuletBlockEntity;
import com.effecoria.block.RegenChamberBlockEntity;
import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.core.glue.EssenceGlueData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Queries the Φ-glued component that makes up a Mage Tower facility. */
public final class TowerFacility {
    private TowerFacility() {}

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
        return findInComponent(level, computerPos, RegenChamberBlockEntity.class).isPresent();
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
}
