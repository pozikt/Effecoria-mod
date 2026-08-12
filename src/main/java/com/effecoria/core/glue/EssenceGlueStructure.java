package com.effecoria.core.glue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Read-only snapshot of a Φ-glued component — for computers / tower integrity later.
 * Marks missing cells (glued but air) so systems can detect breaches.
 */
public final class EssenceGlueStructure {
    public record Report(
            int gluedCells,
            int presentBlocks,
            int missingBlocks,
            double integrity,
            AABB bounds,
            Map<ResourceLocation, Integer> blockCounts) {}

    private EssenceGlueStructure() {}

    public static Report inspect(ServerLevel level, BlockPos anyMember) {
        EssenceGlueData data = EssenceGlueData.get(level);
        Set<BlockPos> component = data.component(anyMember);
        if (component.isEmpty()) {
            return new Report(0, 0, 0, 0.0, new AABB(anyMember), Map.of());
        }

        int present = 0;
        int missing = 0;
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos p : component) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX() + 1);
            maxY = Math.max(maxY, p.getY() + 1);
            maxZ = Math.max(maxZ, p.getZ() + 1);

            BlockState state = level.getBlockState(p);
            if (state.isAir()) {
                missing++;
            } else {
                present++;
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                counts.merge(id, 1, Integer::sum);
            }
        }

        int total = component.size();
        double integrity = total == 0 ? 0.0 : (double) present / (double) total;
        return new Report(total, present, missing, integrity, new AABB(minX, minY, minZ, maxX, maxY, maxZ), Map.copyOf(counts));
    }
}
