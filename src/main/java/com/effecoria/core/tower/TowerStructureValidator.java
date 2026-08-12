package com.effecoria.core.tower;

import com.effecoria.content.ModBlocks;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.alchemy.PhiPowerProvider;
import com.effecoria.core.glue.EssenceGlueData;
import com.effecoria.core.glue.EssenceGlueStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/** Validates a Φ-glued component as a Mage Tower and scores vertical Φ efficiency. */
public final class TowerStructureValidator {
    public static final int MIN_BLOCKS = 12;
    public static final int MAX_BLOCKS = 2000;
    public static final double MIN_INTEGRITY = 0.85;

    public enum ReactorClass {
        NONE,
        SPARK,
        HEART,
        FORGE
    }

    public record Result(
            boolean ok,
            Component error,
            EssenceGlueStructure.Report report,
            double verticality,
            double phiScatter,
            ReactorClass reactorClass,
            BlockPos reactorPos) {
        public static Result fail(Component error) {
            return new Result(false, error, null, 0, 1, ReactorClass.NONE, null);
        }
    }

    private TowerStructureValidator() {}

    public static Result validate(ServerLevel level, BlockPos anchorPos) {
        EssenceGlueData glue = EssenceGlueData.get(level);
        if (!glue.isGlued(anchorPos)) {
            return Result.fail(Component.translatable("message.effecoria.tower.not_glued"));
        }
        Set<BlockPos> component = glue.component(anchorPos);
        if (component.isEmpty()) {
            return Result.fail(Component.translatable("message.effecoria.tower.not_glued"));
        }
        EssenceGlueStructure.Report report = EssenceGlueStructure.inspect(level, anchorPos);
        if (report.gluedCells() < MIN_BLOCKS) {
            return Result.fail(Component.translatable(
                    "message.effecoria.tower.too_small", report.gluedCells(), MIN_BLOCKS));
        }
        if (report.gluedCells() > MAX_BLOCKS) {
            return Result.fail(Component.translatable(
                    "message.effecoria.tower.too_large", report.gluedCells(), MAX_BLOCKS));
        }
        if (report.integrity() < MIN_INTEGRITY) {
            int pct = (int) Math.round(report.integrity() * 100.0);
            return Result.fail(Component.translatable(
                    "message.effecoria.tower.low_integrity", pct, (int) (MIN_INTEGRITY * 100)));
        }

        ReactorHit reactor = findReactor(level, component);
        if (reactor.clazz() == ReactorClass.NONE) {
            return Result.fail(Component.translatable("message.effecoria.tower.no_reactor"));
        }
        if (!reactorPowered(level, anchorPos, reactor)) {
            return Result.fail(Component.translatable("message.effecoria.tower.reactor_offline"));
        }

        AABB bounds = report.bounds();
        double height = Math.max(1.0, bounds.getYsize());
        double spanX = Math.max(1.0, bounds.getXsize());
        double spanZ = Math.max(1.0, bounds.getZsize());
        double horizontal = Math.max(spanX, spanZ);
        double verticality = height / horizontal;
        double phiScatter = Math.max(0.35, Math.min(2.5, 1.0 / (0.5 + verticality)));

        return new Result(true, null, report, verticality, phiScatter, reactor.clazz(), reactor.pos());
    }

    private record ReactorHit(ReactorClass clazz, BlockPos pos) {}

    private static ReactorHit findReactor(ServerLevel level, Set<BlockPos> component) {
        ReactorHit best = new ReactorHit(ReactorClass.NONE, null);
        for (BlockPos p : component) {
            BlockState state = level.getBlockState(p);
            ReactorClass clazz = classify(state);
            if (clazz.ordinal() > best.clazz().ordinal()) {
                best = new ReactorHit(clazz, p.immutable());
            }
        }
        return best;
    }

    private static ReactorClass classify(BlockState state) {
        if (state.is(ModBlocks.FORGE_REACTOR_CORE.get()) || state.is(ModBlocks.FORGE_REACTOR_PART.get())) {
            return ReactorClass.FORGE;
        }
        if (state.is(ModBlocks.HEART_REACTOR_CORE.get()) || state.is(ModBlocks.HEART_REACTOR_PART.get())) {
            return ReactorClass.HEART;
        }
        if (state.is(ModBlocks.SPARK_REACTOR.get())) {
            return ReactorClass.SPARK;
        }
        return ReactorClass.NONE;
    }

    private static boolean reactorPowered(ServerLevel level, BlockPos anchorPos, ReactorHit reactor) {
        if (reactor.pos() != null) {
            BlockEntity be = level.getBlockEntity(reactor.pos());
            if (be instanceof PhiPowerProvider provider && provider.supplying()) {
                return true;
            }
            // Heart/Forge parts point at core — check nearby cores in component via PhiPower at anchor.
        }
        return PhiPower.hasPower(level, anchorPos);
    }
}
