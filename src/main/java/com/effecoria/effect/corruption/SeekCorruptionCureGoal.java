package com.effecoria.effect.corruption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.EnumSet;
import java.util.List;

/**
 * Outside combat, path toward a visible cure block or item for the active corruption curse.
 */
public final class SeekCorruptionCureGoal extends Goal {
    private static final int SEEK_RANGE = 14;
    private static final int REPATH_INTERVAL = 40;

    private final PathfinderMob mob;
    private BlockPos targetBlock;
    private ItemEntity targetItem;
    private int repathCooldown;

    public SeekCorruptionCureGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!CorruptionCurseService.hasCurse(mob) || mob.getTarget() != null) {
            return false;
        }
        return findTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!CorruptionCurseService.hasCurse(mob) || mob.getTarget() != null) {
            return false;
        }
        if (targetItem != null && targetItem.isAlive()) {
            return true;
        }
        if (targetBlock != null) {
            return CorruptionCurseService.canSeeBlock(mob, targetBlock)
                    || mob.getNavigation().isInProgress();
        }
        return false;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        targetBlock = null;
        targetItem = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (mob.getTarget() != null) {
            stop();
            return;
        }
        if (--repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL;
            if (!findTarget()) {
                stop();
                return;
            }
            moveToTarget();
        }
    }

    private boolean findTarget() {
        targetItem = CorruptionCurseService.findVisibleCureItem(mob, SEEK_RANGE);
        if (targetItem != null) {
            targetBlock = null;
            return true;
        }
        List<BlockPos> blocks = CorruptionCurseService.findVisibleCureBlocks(mob, SEEK_RANGE);
        if (blocks.isEmpty()) {
            targetBlock = null;
            return false;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : blocks) {
            double d = mob.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestDist) {
                bestDist = d;
                best = pos;
            }
        }
        targetBlock = best;
        return targetBlock != null;
    }

    private void moveToTarget() {
        if (targetItem != null) {
            mob.getNavigation().moveTo(targetItem, 1.1);
            return;
        }
        if (targetBlock != null) {
            mob.getNavigation()
                    .moveTo(targetBlock.getX() + 0.5, targetBlock.getY() + 0.1, targetBlock.getZ() + 0.5, 1.1);
        }
    }
}
