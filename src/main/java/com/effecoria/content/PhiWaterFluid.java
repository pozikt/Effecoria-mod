package com.effecoria.content;

import com.effecoria.world.ModDimensions;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Φ-water that behaves like a normal lake fluid in realspace, but never spreads in hyperspace
 * (avoids the place→flow→scrub flicker loop).
 */
public final class PhiWaterFluid {
    private PhiWaterFluid() {}

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        protected int getSlopeFindDistance(LevelReader level) {
            return lockedInSubspace(level) ? 0 : super.getSlopeFindDistance(level);
        }

        @Override
        protected int getDropOff(LevelReader level) {
            return lockedInSubspace(level) ? 8 : super.getDropOff(level);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        protected int getSlopeFindDistance(LevelReader level) {
            return lockedInSubspace(level) ? 0 : super.getSlopeFindDistance(level);
        }

        @Override
        protected int getDropOff(LevelReader level) {
            return lockedInSubspace(level) ? 8 : super.getDropOff(level);
        }
    }

    private static boolean lockedInSubspace(LevelReader level) {
        return level instanceof Level world && ModDimensions.isSubspace(world);
    }
}
