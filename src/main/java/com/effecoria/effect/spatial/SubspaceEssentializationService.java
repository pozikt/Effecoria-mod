package com.effecoria.effect.spatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.world.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * Slow Φ-essentialization of matter left in hyperspace: organics → Φ-glass wood/flesh analogues,
 * stone → Φ-stone → essonite ore, water → Φ-water, gold spit-back, void-obsidian charge, essonite growth.
 *
 * <p>Lore timescales (months–millennia) are compressed via {@link BalanceConfig} for playability.
 */
public final class SubspaceEssentializationService {
    private static final String DATA_NAME = "effecoria_subspace_essentialization";
    private static final int SCAN_RADIUS = 14;
    private static final int SCAN_HEIGHT = 10;
    private static final int MAX_WATCHED = 8192;
    private static final int MAX_CONVERTS_PER_TICK = 4;

    private SubspaceEssentializationService() {}

    public static void watch(ServerLevel subspace, BlockPos pos, long gameTime) {
        if (!ModDimensions.isSubspace(subspace)) {
            return;
        }
        Data data = Data.get(subspace);
        data.watch(pos.immutable(), gameTime);
    }

    public static void tick(ServerLevel level) {
        if (!ModDimensions.isSubspace(level)) {
            return;
        }
        int interval = Math.max(1, BalanceConfig.SUBSPACE_ESSENTIALIZE_INTERVAL_TICKS.get());
        if (level.getGameTime() % interval != 0) {
            return;
        }

        Data data = Data.get(level);
        long now = level.getGameTime();
        discoverNearPlayers(level, data, now);

        int converted = 0;
        List<BlockPos> keys = new ArrayList<>(data.cells.keySet());
        // Round-robin by game time so we don't starve late entries.
        int start = (int) (now % Math.max(1, keys.size()));
        for (int n = 0; n < keys.size() && converted < MAX_CONVERTS_PER_TICK; n++) {
            BlockPos pos = keys.get((start + n) % keys.size());
            Cell cell = data.cells.get(pos);
            if (cell == null) {
                continue;
            }
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || isImmuneSurface(state)) {
                data.cells.remove(pos);
                data.setDirty();
                continue;
            }
            if (tryProcess(level, data, pos, cell, state, now)) {
                converted++;
            }
        }

        if (now % (interval * 5L) == 0) {
            tryEssoniteGrowth(level);
        }
    }

    private static void discoverNearPlayers(ServerLevel level, Data data, long now) {
        for (ServerPlayer player : level.players()) {
            BlockPos origin = SubspaceMatterService.resolveDumpOrigin(player);
            scanYard(level, data, origin, now);
            // Also watch matter near the player feet (built structures, floating junk).
            scanYard(level, data, player.blockPosition(), now);
        }
        // Active voyage yards without the host present still age.
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            SubspaceVoyageData voyage = SubspaceVoyageService.get(player);
            if (!voyage.active() && !voyage.pendingEntry()) {
                continue;
            }
            BlockPos entry = voyage.entrySubspacePos();
            if (entry != null) {
                scanYard(level, data, entry.offset(6, 0, 6), now);
            }
        }
    }

    private static void scanYard(ServerLevel level, Data data, BlockPos origin, long now) {
        if (!level.isLoaded(origin)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int step = 2;
        for (int dy = -2; dy <= SCAN_HEIGHT; dy += step) {
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += step) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += step) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || isImmuneSurface(state)) {
                        continue;
                    }
                    if (resolveTarget(state, 0) == null
                            && !isGold(state)
                            && !isEssoniteFamily(state)
                            && !isPhiStone(state)
                            && !isPhiWater(state)) {
                        // Still watch phi-stage blocks for stage-2 / growth / spit.
                        if (!isWatchWorthy(state)) {
                            continue;
                        }
                    }
                    data.watch(cursor.immutable(), now);
                }
            }
        }
    }

    private static boolean isWatchWorthy(BlockState state) {
        return resolveTarget(state, 0) != null
                || resolveTarget(state, 1) != null
                || isGold(state)
                || isEssoniteFamily(state)
                || isPhiStone(state)
                || isPhiWater(state)
                || isObsidian(state);
    }

    private static boolean tryProcess(
            ServerLevel level, Data data, BlockPos pos, Cell cell, BlockState state, long now) {
        long rawAge = now - cell.enteredAt();
        int speed = Math.max(0, level.getGameRules().getInt(com.effecoria.world.ModGameRules.SUBSPACE_ESSENTIALIZE_SPEED));
        if (speed == 0) {
            return false;
        }
        long age = rawAge * (long) speed;

        if (isGold(state)) {
            long need = BalanceConfig.SUBSPACE_GOLD_SPIT_TICKS.get();
            if (age >= need) {
                spitGold(level, pos, state);
                data.cells.remove(pos);
                data.setDirty();
                return true;
            }
            return false;
        }

        int stage = cell.stage();
        BlockState target = resolveTarget(state, stage);
        if (target == null) {
            // Already terminal or unknown — drop watch unless essonite growth candidate.
            if (!isEssoniteFamily(state) && !isPhiStone(state)) {
                data.cells.remove(pos);
                data.setDirty();
            }
            return false;
        }

        long need = requiredAgeTicks(state, stage);
        if (age < need) {
            return false;
        }

        level.setBlock(pos, target, Block.UPDATE_ALL);
        fxConvert(level, pos, target);
        data.cells.put(pos, new Cell(now, stage + 1));
        data.setDirty();
        return true;
    }

    private static long requiredAgeTicks(BlockState state, int stage) {
        if (isOrganicFast(state)) {
            return BalanceConfig.SUBSPACE_ORGANIC_TICKS.get();
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES)) {
            return BalanceConfig.SUBSPACE_WOOD_TICKS.get();
        }
        if (isWater(state) || isPhiWater(state)) {
            return stage == 0
                    ? BalanceConfig.SUBSPACE_WATER_TICKS.get()
                    : BalanceConfig.SUBSPACE_WATER_TICKS.get() * 2L;
        }
        if (isQuartzish(state) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.TINTED_GLASS)) {
            return stage == 0
                    ? BalanceConfig.SUBSPACE_QUARTZ_TICKS.get()
                    : BalanceConfig.SUBSPACE_STONE_ORE_TICKS.get();
        }
        if (isObsidian(state)) {
            return BalanceConfig.SUBSPACE_OBSIDIAN_TICKS.get();
        }
        if (isPhiStone(state) || stage >= 1) {
            return BalanceConfig.SUBSPACE_STONE_ORE_TICKS.get();
        }
        if (isIron(state)) {
            return BalanceConfig.SUBSPACE_IRON_TICKS.get();
        }
        // Generic stone / other
        return BalanceConfig.SUBSPACE_STONE_TICKS.get();
    }

    @Nullable
    private static BlockState resolveTarget(BlockState state, int stage) {
        if (stage == 0) {
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
                return ModBlocks.PHI_GRASS.get().defaultBlockState();
            }
            if (state.is(BlockTags.DIRT) || state.is(Blocks.MOSS_BLOCK)) {
                return ModBlocks.PHI_DIRT.get().defaultBlockState();
            }
            if (state.is(BlockTags.LOGS)) {
                BlockState log = ModBlocks.PHI_LOG.get().defaultBlockState();
                if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                    return log.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
                }
                return log;
            }
            if (state.is(BlockTags.PLANKS)) {
                return ModBlocks.PHI_PLANKS.get().defaultBlockState();
            }
            if (state.is(BlockTags.LEAVES)) {
                return ModBlocks.PHI_LEAVES.get().defaultBlockState();
            }
            if (state.is(BlockTags.CROPS)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SAPLINGS)
                    || state.is(Blocks.SHORT_GRASS)
                    || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.FERN)
                    || state.is(Blocks.LARGE_FERN)
                    || state.is(Blocks.MOSS_CARPET)
                    || state.is(Blocks.VINE)
                    || state.is(Blocks.CACTUS)
                    || state.is(Blocks.SUGAR_CANE)
                    || state.is(Blocks.HAY_BLOCK)
                    || state.is(Blocks.MELON)
                    || state.is(Blocks.PUMPKIN)
                    || state.is(Blocks.CARVED_PUMPKIN)
                    || state.is(Blocks.JACK_O_LANTERN)
                    || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                    || state.is(Blocks.RED_MUSHROOM_BLOCK)
                    || state.is(Blocks.MUSHROOM_STEM)
                    || state.is(Blocks.BONE_BLOCK)) {
                return ModBlocks.PHI_BLADES.get().defaultBlockState();
            }
            if (isWater(state)) {
                return ModBlocks.PHI_WATER.get().defaultBlockState();
            }
            if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) || state.is(Blocks.TINTED_GLASS)) {
                return ModBlocks.PHI_GLASS.get().defaultBlockState();
            }
            if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SANDSTONE)
                    || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.CALCITE)) {
                return ModBlocks.ESSONITE_CRUST.get().defaultBlockState();
            }
            if (isQuartzish(state)) {
                return ModBlocks.ESSONITE_CRUST.get().defaultBlockState();
            }
            if (isObsidian(state)) {
                return ModBlocks.VOID_OBSIDIAN.get().defaultBlockState();
            }
            if (state.is(Blocks.AMETHYST_CLUSTER)) {
                return ModBlocks.ESSONITE_CRYSTAL.get().defaultBlockState();
            }
            if (state.is(Blocks.SMALL_AMETHYST_BUD)) {
                return ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get().defaultBlockState();
            }
            if (state.is(Blocks.MEDIUM_AMETHYST_BUD)) {
                return ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM.get().defaultBlockState();
            }
            if (state.is(Blocks.LARGE_AMETHYST_BUD)) {
                return ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE.get().defaultBlockState();
            }
            if (isStoneFamily(state)) {
                return ModBlocks.PHI_STONE.get().defaultBlockState();
            }
            // Iron: visual/material essentialization without a dedicated Φ-iron block — stay iron,
            // but allow a rare "hardened" path to iron block with no change (skip).
            return null;
        }

        // Stage 1+: advance Φ intermediates toward ore / denser crystal.
        if (isPhiStone(state) || state.is(ModBlocks.ESSONITE_CRUST.get())) {
            return hostOreFor(state);
        }
        if (isPhiWater(state)) {
            // Φ-ice analogue — packed ice that does not melt in normal heat; still no ElementalBlockService.
            return Blocks.BLUE_ICE.defaultBlockState();
        }
        return null;
    }

    private static BlockState hostOreFor(BlockState previous) {
        // Default stone ore; crust/sand path also yields standard ore.
        return ModBlocks.ESSENITE_ORE.get().defaultBlockState();
    }

    private static boolean isOrganicFast(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.BONE_BLOCK)
                || state.is(Blocks.HAY_BLOCK);
    }

    private static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.FROSTED_ICE)
                || state.getFluidState().is(Fluids.WATER)
                || state.getFluidState().is(Fluids.FLOWING_WATER);
    }

    private static boolean isPhiWater(BlockState state) {
        return state.is(ModBlocks.PHI_WATER.get());
    }

    private static boolean isPhiStone(BlockState state) {
        return state.is(ModBlocks.PHI_STONE.get());
    }

    private static boolean isQuartzish(BlockState state) {
        return state.is(Blocks.QUARTZ_BLOCK)
                || state.is(Blocks.SMOOTH_QUARTZ)
                || state.is(Blocks.QUARTZ_PILLAR)
                || state.is(Blocks.QUARTZ_SLAB)
                || state.is(Blocks.QUARTZ_STAIRS)
                || state.is(Blocks.CHISELED_QUARTZ_BLOCK)
                || state.is(Blocks.QUARTZ_BRICKS)
                || state.is(Blocks.SEA_LANTERN);
    }

    private static boolean isStoneFamily(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.END_STONE)
                || state.is(Blocks.TERRACOTTA);
    }

    private static boolean isObsidian(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN);
    }

    private static boolean isGold(BlockState state) {
        return state.is(Blocks.GOLD_BLOCK)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.RAW_GOLD_BLOCK)
                || state.is(Blocks.GILDED_BLACKSTONE)
                || state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    private static boolean isIron(BlockState state) {
        return state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.RAW_IRON_BLOCK)
                || state.is(Blocks.IRON_BARS)
                || state.is(Blocks.CHAIN)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL);
    }

    private static boolean isEssoniteFamily(BlockState state) {
        return state.is(ModBlocks.ESSENITE_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_ESSENITE_ORE.get())
                || state.is(ModBlocks.ESSONITE_BLOCK.get())
                || state.is(ModBlocks.STAR_ESSONITE_BLOCK.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_MEDIUM.get())
                || state.is(ModBlocks.ESSONITE_CRYSTAL_BUD_LARGE.get())
                || state.is(ModBlocks.ESSONITE_CRUST.get());
    }

    private static boolean isImmuneSurface(BlockState state) {
        return state.is(ModBlocks.PHI_VEIL.get())
                || state.is(ModBlocks.SUBSPACE_PORTAL.get())
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.END_ROD);
    }

    private static void spitGold(ServerLevel subspace, BlockPos pos, BlockState state) {
        subspace.removeBlock(pos, false);
        subspace.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.8f, 1.4f);
        subspace.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                18,
                0.35,
                0.35,
                0.35,
                0.04);

        ServerLevel overworld = subspace.getServer().overworld();
        BlockPos dest = approximateSpitPos(subspace, overworld, pos);
        overworld.getChunkAt(dest);

        ItemStack stack = goldLoot(state);
        boolean placed = false;
        if (state.is(Blocks.GOLD_BLOCK) && overworld.getBlockState(dest).canBeReplaced()) {
            overworld.setBlock(dest, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            placed = true;
        } else {
            ItemEntity item = new ItemEntity(
                    overworld, dest.getX() + 0.5, dest.getY() + 1.2, dest.getZ() + 0.5, stack);
            item.setDefaultPickUpDelay();
            overworld.addFreshEntity(item);
            placed = true;
        }
        if (placed) {
            overworld.playSound(null, dest, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.5f, 1.6f);
            for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
                if (player.level() == overworld && player.distanceToSqr(Vec3.atCenterOf(dest)) < 96 * 96) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.effecoria.subspace.gold_spit",
                                    dest.getX(),
                                    dest.getY(),
                                    dest.getZ()),
                            true);
                }
            }
        }
    }

    private static BlockPos approximateSpitPos(ServerLevel subspace, ServerLevel overworld, BlockPos subspacePos) {
        // Prefer mapping via any active voyage; else hash-scatter around world spawn.
        for (ServerPlayer player : subspace.getServer().getPlayerList().getPlayers()) {
            SubspaceVoyageData voyage = SubspaceVoyageService.get(player);
            if (voyage.active() || voyage.pendingEntry() || voyage.originPos() != null) {
                BlockPos mapped = SubspaceVoyageService.mapToOrigin(voyage, subspacePos);
                int surface = overworld.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        mapped.getX(),
                        mapped.getZ());
                return new BlockPos(mapped.getX(), Math.max(overworld.getMinBuildHeight() + 1, surface), mapped.getZ());
            }
        }
        BlockPos spawn = overworld.getSharedSpawnPos();
        long h = BlockPos.asLong(subspacePos.getX(), subspacePos.getY(), subspacePos.getZ());
        int dx = (int) ((h % 401) - 200);
        int dz = (int) (((h / 401) % 401) - 200);
        int x = spawn.getX() + dx;
        int z = spawn.getZ() + dz;
        int y = overworld.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    private static ItemStack goldLoot(BlockState state) {
        if (state.is(Blocks.GOLD_BLOCK)) {
            return new ItemStack(Items.GOLD_BLOCK);
        }
        if (state.is(Blocks.RAW_GOLD_BLOCK)) {
            return new ItemStack(Items.RAW_GOLD_BLOCK);
        }
        if (state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)) {
            return new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        }
        return new ItemStack(Items.GOLD_INGOT, 2 + subspaceRandom(2));
    }

    private static int subspaceRandom(int bound) {
        return bound <= 1 ? 0 : (int) (System.nanoTime() % bound);
    }

    private static void tryEssoniteGrowth(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        if (level.random.nextFloat() > BalanceConfig.SUBSPACE_ESSONITE_GROW_CHANCE.get().floatValue()) {
            return;
        }
        ServerPlayer witness = level.players().get(level.random.nextInt(level.players().size()));
        BlockPos origin = witness.blockPosition();
        for (int i = 0; i < 12; i++) {
            BlockPos pos = origin.offset(
                    level.random.nextInt(17) - 8,
                    level.random.nextInt(7) - 2,
                    level.random.nextInt(17) - 8);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isEssoniteFamily(state) && !isPhiStone(state)) {
                continue;
            }
            // Grow a bud into adjacent air.
            for (Direction dir : Direction.values()) {
                BlockPos air = pos.relative(dir);
                if (!level.getBlockState(air).isAir()) {
                    continue;
                }
                BlockState bud = ModBlocks.ESSONITE_CRYSTAL_BUD_SMALL.get().defaultBlockState();
                level.setBlock(air, bud, Block.UPDATE_ALL);
                level.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        air.getX() + 0.5,
                        air.getY() + 0.5,
                        air.getZ() + 0.5,
                        8,
                        0.2,
                        0.2,
                        0.2,
                        0.02);
                return;
            }
            // Dense growth: phi_stone / ore → bump toward block.
            if (isPhiStone(state) && level.random.nextFloat() < 0.25f) {
                level.setBlock(pos, ModBlocks.ESSENITE_ORE.get().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
            if (state.is(ModBlocks.ESSENITE_ORE.get()) && level.random.nextFloat() < 0.08f) {
                level.setBlock(pos, ModBlocks.ESSONITE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
            if (state.is(ModBlocks.ESSONITE_BLOCK.get()) && level.random.nextFloat() < 0.03f) {
                level.setBlock(pos, ModBlocks.STAR_ESSONITE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }

    private static void fxConvert(ServerLevel level, BlockPos pos, BlockState target) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.55f, 0.7f + level.random.nextFloat() * 0.4f);
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.55,
                pos.getZ() + 0.5,
                10,
                0.3,
                0.3,
                0.3,
                0.03);
        String id = BuiltInRegistries.BLOCK.getKey(target.getBlock()).getPath();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) < 48 * 48) {
                player.displayClientMessage(
                        Component.translatable("message.effecoria.subspace.essentialize", id), true);
                break;
            }
        }
    }

    private record Cell(long enteredAt, int stage) {}

    public static final class Data extends SavedData {
        private final Map<BlockPos, Cell> cells = new HashMap<>();

        public static Data get(ServerLevel level) {
            return level.getDataStorage()
                    .computeIfAbsent(new Factory<>(Data::new, Data::load), DATA_NAME);
        }

        public void watch(BlockPos pos, long gameTime) {
            Cell existing = cells.get(pos);
            if (existing != null) {
                return;
            }
            if (cells.size() >= MAX_WATCHED) {
                // Drop an arbitrary old entry.
                Iterator<BlockPos> it = cells.keySet().iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
            cells.put(pos.immutable(), new Cell(gameTime, 0));
            setDirty();
        }

        public static Data load(CompoundTag tag, HolderLookup.Provider lookup) {
            Data data = new Data();
            ListTag list = tag.getList("Cells", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos pos = BlockPos.of(entry.getLong("Pos"));
                long entered = entry.getLong("At");
                int stage = entry.getInt("Stage");
                data.cells.put(pos, new Cell(entered, stage));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Map.Entry<BlockPos, Cell> e : cells.entrySet()) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Pos", e.getKey().asLong());
                entry.putLong("At", e.getValue().enteredAt());
                entry.putInt("Stage", e.getValue().stage());
                list.add(entry);
            }
            tag.put("Cells", list);
            return tag;
        }
    }
}
