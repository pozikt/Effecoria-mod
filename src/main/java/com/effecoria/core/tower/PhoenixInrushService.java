package com.effecoria.core.tower;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.effecoria.block.PhiAccumulatorBlockEntity;
import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiBusNetwork;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.circuit.PhiChannel;
import com.effecoria.core.glue.EssenceGlueData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Phase C: facility ΔQ buffers support rematerialize when the reactor is dark, and pay
 * an inrush cost on body rebuild (Law 1 — legal discharge of stored Φ-charge).
 */
public final class PhoenixInrushService {
    /** Minimum total buffer charge to exit Φ-ghost without a live injector at the anchor. */
    public static final int MIN_BUFFER = 120;

    private PhoenixInrushService() {}

    public static int bufferCharge(ServerLevel level, BlockPos anchorPos) {
        int sum = 0;
        for (PhiAccumulatorBlockEntity acc : listAccumulators(level, anchorPos)) {
            sum += acc.charge();
        }
        return sum;
    }

    public static boolean canSupportRevive(ServerLevel level, BlockPos anchorPos) {
        if (PhiPower.hasPower(level, anchorPos)) {
            return true;
        }
        return bufferCharge(level, anchorPos) >= MIN_BUFFER;
    }

    /** Inrush ΔQ cost for rematerializing the given body template. */
    public static int inrushCost(TowerBodyType body) {
        return switch (body == null ? TowerBodyType.BASIC : body) {
            case BASIC -> 120;
            case ENHANCED -> 200;
            case COMBAT -> 320;
            case ARCANE -> 280;
        };
    }

    /**
     * Drain up to {@code cost} from facility accumulators (life-channel first).
     * @return charge actually taken
     */
    public static int dischargeInrush(ServerLevel level, BlockPos anchorPos, int cost) {
        if (cost <= 0) {
            return 0;
        }
        List<PhiAccumulatorBlockEntity> buffers = listAccumulators(level, anchorPos);
        buffers.sort(Comparator
                .comparingInt((PhiAccumulatorBlockEntity a) -> isLifeBuffer(level, a.getBlockPos()) ? 0 : 1)
                .thenComparingInt(a -> -a.charge()));
        int need = cost;
        int taken = 0;
        for (PhiAccumulatorBlockEntity acc : buffers) {
            if (need <= 0) {
                break;
            }
            int got = acc.takeCharge(need);
            taken += got;
            need -= got;
        }
        return taken;
    }

    public static int dischargeInrush(ServerLevel level, BlockPos anchorPos, TowerBodyType body) {
        return dischargeInrush(level, anchorPos, inrushCost(body));
    }

    private static List<PhiAccumulatorBlockEntity> listAccumulators(ServerLevel level, BlockPos anchorPos) {
        List<PhiAccumulatorBlockEntity> out = new ArrayList<>();
        for (BlockPos pos : EssenceGlueData.get(level).component(anchorPos)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.PHI_ACCUMULATOR.get())) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhiAccumulatorBlockEntity acc) {
                out.add(acc);
            }
        }
        return out;
    }

    private static boolean isLifeBuffer(ServerLevel level, BlockPos pos) {
        if (PhiBusNetwork.channelAt(level, pos) == PhiChannel.LIFE) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (PhiBusNetwork.isConductor(level.getBlockState(adj))
                    && PhiBusNetwork.channelAt(level, adj) == PhiChannel.LIFE) {
                return true;
            }
        }
        return false;
    }
}
