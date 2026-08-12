package com.effecoria.block;

import com.effecoria.alchemy.menu.ForgeReactorMenu;
import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.alchemy.ForgeMultiblock;
import com.effecoria.core.alchemy.ForgeRecipes;
import com.effecoria.core.alchemy.PhiPowerHubs;
import com.effecoria.core.alchemy.PhiPowerProvider;
import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** Era IV Forge Reactor («Кузница») — industrial Φ hub and material forge. */
public final class ForgeReactorBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PhiPowerProvider {
    public static final int SLOT_FUEL_1 = 0;
    public static final int SLOT_FUEL_2 = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_IN_1 = 3;
    public static final int SLOT_IN_2 = 4;
    public static final int SLOT_OUT = 5;
    public static final int SLOT_COUNT = 6;

    public static final int DATA_FORMED = 0;
    public static final int DATA_RUNNING = 1;
    public static final int DATA_MODE = 2;
    public static final int DATA_POWER = 3;
    public static final int DATA_TEMP = 4;
    public static final int DATA_OMEGA = 5;
    public static final int DATA_COOLED = 6;
    public static final int DATA_FUEL = 7;
    public static final int DATA_FUEL_MAX = 8;
    public static final int DATA_PROGRESS = 9;
    public static final int DATA_PROGRESS_MAX = 10;
    public static final int DATA_COUNT = 11;

    public static final int POWER_RADIUS = 32;
    public static final int MAX_TEMP = 3000;
    public static final int OVERHEAT_NO_COOLANT_TICKS = 1200;
    public static final int SCRAM_COOLDOWN = 200;
    public static final int OMEGA_WARN = 10;
    public static final int OMEGA_SCRAM = 25;
    public static final int OMEGA_BURST = 50;

    public static final int FUEL_STAR = 24000;
    public static final int FUEL_PURE = 12000;
    public static final int FUEL_CELL = 6000;

    private static final int[] FUEL_SLOTS = {SLOT_FUEL_1, SLOT_FUEL_2};
    private static final int[] INPUT_SLOTS = {SLOT_IN_1, SLOT_IN_2};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUT};
    private static final int[] NO_SLOTS = {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean formed;
    private boolean running;
    private boolean dismantling;
    private ForgeRecipes.Mode mode = ForgeRecipes.Mode.ENERGY;
    private int fuelTicks;
    private int fuelMax = 1;
    private int temperature = 200;
    private int omegaCentis; // 0..10000 → display as %
    private int runWithoutCoolant;
    private int scramCooldown;
    private int progress;
    private int progressMax;
    private boolean hubRegistered;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FORMED -> formed ? 1 : 0;
                case DATA_RUNNING -> running ? 1 : 0;
                case DATA_MODE -> mode.ordinal();
                case DATA_POWER -> powerPercent();
                case DATA_TEMP -> temperature;
                case DATA_OMEGA -> omegaPercent();
                case DATA_COOLED -> hasCoolant() ? 1 : 0;
                case DATA_FUEL -> fuelTicks;
                case DATA_FUEL_MAX -> Math.max(1, fuelMax);
                case DATA_PROGRESS -> progress;
                case DATA_PROGRESS_MAX -> Math.max(1, progressMax);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FORMED -> formed = value != 0;
                case DATA_RUNNING -> running = value != 0;
                case DATA_MODE -> mode = ForgeRecipes.Mode.values()[Mth.clamp(value, 0, ForgeRecipes.Mode.values().length - 1)];
                case DATA_TEMP -> temperature = value;
                case DATA_OMEGA -> omegaCentis = value * 100;
                case DATA_FUEL -> fuelTicks = value;
                case DATA_PROGRESS -> progress = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ForgeReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE_REACTOR_CORE.get(), pos, state);
    }

    public ContainerData getData() {
        return data;
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean value) {
        formed = value;
    }

    public boolean isDismantling() {
        return dismantling;
    }

    public void setDismantling(boolean value) {
        dismantling = value;
    }

    public void onDisassembled() {
        running = false;
        progress = 0;
        progressMax = 0;
        runWithoutCoolant = 0;
        unregisterHub();
    }

    public boolean isRunning() {
        return running;
    }

    public ForgeRecipes.Mode mode() {
        return mode;
    }

    public void cycleMode() {
        if (running) {
            return;
        }
        mode = mode.next();
        progress = 0;
        progressMax = 0;
        setChanged();
    }

    public void toggleRunning() {
        if (running) {
            scram(false);
        } else {
            tryStart();
        }
    }

    public void emergencyScram() {
        scram(true);
    }

    private void tryStart() {
        if (!formed || scramCooldown > 0) {
            return;
        }
        if (fuelTicks <= 0 && !tryConsumeFuel()) {
            return;
        }
        running = true;
        setChanged();
    }

    private void scram(boolean emergency) {
        running = false;
        progress = 0;
        progressMax = 0;
        if (emergency) {
            scramCooldown = SCRAM_COOLDOWN;
            temperature = Math.max(200, temperature - 800);
        }
        unregisterHub();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.5f);
        }
        setChanged();
    }

    public int powerPercent() {
        if (!isActivelyPowering()) {
            return 0;
        }
        int base = Mth.clamp(Math.round(100f * fuelTicks / (float) Math.max(1, fuelMax)), 1, 100);
        if (base >= 100) {
            return 100;
        }
        return base;
    }

    public int omegaPercent() {
        return Mth.clamp(omegaCentis / 100, 0, 100);
    }

    /** Clears Ω contamination (omega_filter). */
    public boolean clearOmegaMeter() {
        if (omegaCentis <= 0) {
            return false;
        }
        omegaCentis = 0;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.2f);
        }
        return true;
    }

    private boolean isActivelyPowering() {
        return formed && running && fuelTicks > 0 && scramCooldown <= 0 && omegaPercent() < OMEGA_SCRAM;
    }

    @Override
    public boolean supplying() {
        return isActivelyPowering();
    }

    @Override
    public int radius() {
        return POWER_RADIUS;
    }

    @Override
    public float powerFactor() {
        if (!isActivelyPowering()) {
            return 0f;
        }
        // ENERGY mode is the city-scale hub; other modes still radiate but weaker.
        float base = mode == ForgeRecipes.Mode.ENERGY ? 2.5f : 1.5f;
        // Scale with remaining fuel (was a cliff at exactly 100% that made mid-burn feel "weak").
        float fuelRatio = Math.max(0.35f, fuelTicks / (float) Math.max(1, fuelMax));
        base *= 0.7f + 0.6f * fuelRatio;
        if (temperature > 2800) {
            base *= 0.6f;
        } else if (temperature >= 800 && temperature <= 2200) {
            // Stable operating band — slight boost when actually hot.
            base *= 1.1f;
        }
        return base;
    }

    @Override
    public boolean drainFuel(int ticks) {
        if (!isActivelyPowering() || ticks <= 0 || fuelTicks < ticks) {
            return false;
        }
        fuelTicks -= ticks;
        setChanged();
        return true;
    }

    public static boolean isValidFuel(ItemStack stack) {
        if (stack.is(ModItems.STAR_ESSONITE.get()) || stack.is(ModItems.PURE_ESSONITE.get())) {
            return true;
        }
        return stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) >= 0.85f;
    }

    public static boolean isValidCatalyst(ItemStack stack) {
        return stack.is(ModItems.LONVER_BLOOD_VIAL.get())
                || stack.is(ModItems.PHI_NECTAR.get())
                || stack.is(ModItems.FIREFLOWER.get());
    }

    private boolean tryConsumeFuel() {
        for (int slot : new int[] {SLOT_FUEL_1, SLOT_FUEL_2}) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int ticks = 0;
            if (stack.is(ModItems.STAR_ESSONITE.get())) {
                ticks = FUEL_STAR;
            } else if (stack.is(ModItems.PURE_ESSONITE.get())) {
                ticks = FUEL_PURE;
            } else if (stack.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(stack) >= 0.85f) {
                ticks = FUEL_CELL;
                PhiHarnessItems.setCellCharge(stack, 0f);
            }
            if (ticks <= 0) {
                continue;
            }
            if (!stack.is(ModItems.PHI_CELL.get())) {
                stack.shrink(1);
            }
            fuelTicks = ticks;
            fuelMax = ticks;
            setChanged();
            return true;
        }
        return false;
    }

    private float catalystOmegaMul() {
        ItemStack cat = items.get(SLOT_CATALYST);
        if (cat.isEmpty()) {
            return 1f;
        }
        if (cat.is(ModItems.LONVER_BLOOD_VIAL.get())) {
            return 0.55f;
        }
        if (cat.is(ModItems.PHI_NECTAR.get())) {
            return 0.7f;
        }
        if (cat.is(ModItems.FIREFLOWER.get())) {
            return 0.85f;
        }
        return 1f;
    }

    private float catalystHeatMul() {
        ItemStack cat = items.get(SLOT_CATALYST);
        if (cat.is(ModItems.FIREFLOWER.get())) {
            return 1.35f;
        }
        if (cat.is(ModItems.PHI_NECTAR.get())) {
            return 1.15f;
        }
        return 1f;
    }

    private boolean hasCoolant() {
        if (level == null) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -3; dy <= 4; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean inHull = dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1 && dy >= -1 && dy <= 2;
                    if (inHull) {
                        continue;
                    }
                    if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) > 5) {
                        continue;
                    }
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    FluidState fluid = level.getFluidState(cursor);
                    if (state.is(Blocks.ICE)
                            || state.is(Blocks.PACKED_ICE)
                            || state.is(Blocks.BLUE_ICE)
                            || state.is(Blocks.WATER)
                            || state.is(BlockTags.ICE)
                            || fluid.is(FluidTags.WATER)
                            || state.is(ModBlocks.PHI_WATER.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void applyHeatAura(ServerLevel server) {
        if (server.getGameTime() % 20L != 0L) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int melted = 0;
        for (int dy = -5; dy <= 5 && melted < 8; dy++) {
            for (int dx = -5; dx <= 5 && melted < 8; dx++) {
                for (int dz = -5; dz <= 5 && melted < 8; dz++) {
                    if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) > 5) {
                        continue;
                    }
                    cursor.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = server.getBlockState(cursor);
                    if (state.is(Blocks.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.SNOW)) {
                        server.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        melted++;
                    } else if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                        server.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        melted++;
                    }
                }
            }
        }
    }

    private void tickProcess(ServerLevel server) {
        if (mode == ForgeRecipes.Mode.ENERGY) {
            progress = 0;
            progressMax = 0;
            return;
        }
        ItemStack in1 = items.get(SLOT_IN_1);
        ItemStack in2 = items.get(SLOT_IN_2);
        var match = ForgeRecipes.match(mode, in1, in2);
        if (match.isEmpty()) {
            progress = 0;
            progressMax = 0;
            return;
        }
        ForgeRecipes.Recipe recipe = match.get();
        ItemStack out = items.get(SLOT_OUT);
        ItemStack result = recipe.out();
        if (!out.isEmpty()
                && (!ItemStack.isSameItemSameComponents(out, result)
                        || out.getCount() + result.getCount() > out.getMaxStackSize())) {
            return;
        }
        progressMax = recipe.durationTicks();
        progress++;
        if (progress >= progressMax) {
            ForgeRecipes.consume(recipe, in1, in2);
            if (out.isEmpty()) {
                items.set(SLOT_OUT, result.copy());
            } else {
                out.grow(result.getCount());
            }
            progress = 0;
            omegaCentis += Math.round(120 * catalystOmegaMul());
            if (!items.get(SLOT_CATALYST).isEmpty() && server.getRandom().nextFloat() < 0.08f) {
                items.get(SLOT_CATALYST).shrink(1);
            }
            setChanged();
        }
    }

    private void maybeOmegaBurst(ServerLevel server) {
        if (omegaPercent() < OMEGA_BURST) {
            return;
        }
        scram(true);
        omegaCentis = 0;
        server.sendParticles(
                ParticleTypes.SCULK_SOUL,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5,
                40,
                1.5,
                1.5,
                1.5,
                0.05);
        server.playSound(null, worldPosition, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.7f, 0.6f);
        AABB box = new AABB(worldPosition).inflate(6);
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, box)) {
            entity.hurt(server.damageSources().magic(), 6f);
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                DiseaseService.infect(player, PhiDisease.OMEGA_SICKNESS, 1);
            }
        }
    }

    private void registerHub() {
        if (level != null && !hubRegistered && isActivelyPowering()) {
            PhiPowerHubs.setActive(level, worldPosition, true);
            hubRegistered = true;
        }
    }

    private void unregisterHub() {
        if (level != null && hubRegistered) {
            PhiPowerHubs.setActive(level, worldPosition, false);
            hubRegistered = false;
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ForgeReactorBlockEntity be) {
        if (state.getValue(ForgeReactorBlock.LIT)) {
            com.effecoria.client.sound.ReactorHumClient.ensureForge(pos);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeReactorBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean changed = false;

        if (!be.formed) {
            if (ForgeMultiblock.isMaterialShell(level, pos)) {
                ForgeMultiblock.assemble(server, pos);
                changed = true;
            }
        } else if (!ForgeMultiblock.isAssembled(level, pos)) {
            ForgeMultiblock.disassemble(server, pos);
            changed = true;
        }

        if (be.scramCooldown > 0) {
            be.scramCooldown--;
            be.running = false;
            changed = true;
        }

        if (be.running && be.formed && be.scramCooldown <= 0) {
            if (be.fuelTicks <= 0 && !be.tryConsumeFuel()) {
                be.scram(false);
                changed = true;
            } else if (be.fuelTicks > 0) {
                be.fuelTicks--;
                changed = true;
                boolean cooled = be.hasCoolant();
                float heatMul = be.catalystHeatMul();
                if (cooled) {
                    // Approach a stable operating band (~1000–1400°C) instead of floor-locking at 200
                    // (old cool 3 > heat 1.2 kept temp stuck while "running").
                    int target = 1200;
                    if (be.temperature < target) {
                        be.temperature = Math.min(target, be.temperature + Math.max(2, Math.round(3.2f * heatMul)));
                    } else if (be.temperature > target + 200) {
                        be.temperature = Math.max(target, be.temperature - 3);
                    } else {
                        // Gentle drift within band
                        be.temperature += (server.random.nextBoolean() ? 1 : -1);
                        be.temperature = Mth.clamp(be.temperature, target - 100, target + 200);
                    }
                    be.runWithoutCoolant = Math.max(0, be.runWithoutCoolant - 4);
                } else {
                    be.temperature = Mth.clamp(
                            be.temperature + Math.round(5.5f * heatMul), 200, MAX_TEMP + 200);
                    be.runWithoutCoolant++;
                    if (be.runWithoutCoolant >= OVERHEAT_NO_COOLANT_TICKS) {
                        be.scram(true);
                        be.omegaCentis += 800;
                    }
                }
                if (be.temperature >= MAX_TEMP) {
                    be.scram(true);
                    be.omegaCentis += 500;
                }
                if (be.omegaPercent() >= OMEGA_SCRAM) {
                    be.scram(true);
                }
                be.tickProcess(server);
                be.applyHeatAura(server);
                be.maybeOmegaBurst(server);
                // idle omega creep in energy mode
                if (be.mode == ForgeRecipes.Mode.ENERGY && server.getGameTime() % 40L == 0L) {
                    be.omegaCentis += Math.round(8 * be.catalystOmegaMul());
                }
            }
        } else if (be.temperature > 200 && server.getGameTime() % 2L == 0L) {
            be.temperature--;
            changed = true;
        }

        if (be.isActivelyPowering()) {
            be.registerHub();
        } else {
            be.unregisterHub();
        }

        boolean shouldLit = be.isActivelyPowering();
        BlockState current = level.getBlockState(pos);
        if (current.is(ModBlocks.FORGE_REACTOR_CORE.get())) {
            boolean needUpdate = false;
            BlockState next = current;
            if (current.getValue(ForgeReactorBlock.LIT) != shouldLit) {
                next = next.setValue(ForgeReactorBlock.LIT, shouldLit);
                needUpdate = true;
            }
            if (current.getValue(ForgeReactorBlock.FORMED) != be.formed) {
                next = next.setValue(ForgeReactorBlock.FORMED, be.formed);
                needUpdate = true;
            }
            if (needUpdate) {
                level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            }
        }
        if (changed) {
            be.setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.effecoria.forge_reactor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new ForgeReactorMenu(id, inv, this, data);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            this.items.set(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        if (side == Direction.UP) {
            return INPUT_SLOTS;
        }
        return FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == SLOT_OUT;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (index) {
            case SLOT_FUEL_1, SLOT_FUEL_2 -> isValidFuel(stack);
            case SLOT_CATALYST -> isValidCatalyst(stack);
            case SLOT_IN_1, SLOT_IN_2 -> true;
            default -> false;
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("Running", running);
        tag.putString("Mode", mode.name());
        tag.putInt("Fuel", fuelTicks);
        tag.putInt("FuelMax", fuelMax);
        tag.putInt("Temp", temperature);
        tag.putInt("Omega", omegaCentis);
        tag.putInt("NoCool", runWithoutCoolant);
        tag.putInt("Scram", scramCooldown);
        tag.putInt("Progress", progress);
        tag.putInt("ProgressMax", progressMax);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        formed = tag.getBoolean("Formed");
        running = tag.getBoolean("Running");
        try {
            mode = ForgeRecipes.Mode.valueOf(tag.getString("Mode"));
        } catch (IllegalArgumentException ignored) {
            mode = ForgeRecipes.Mode.ENERGY;
        }
        fuelTicks = tag.getInt("Fuel");
        fuelMax = Math.max(1, tag.getInt("FuelMax"));
        temperature = tag.getInt("Temp");
        omegaCentis = tag.getInt("Omega");
        runWithoutCoolant = tag.getInt("NoCool");
        scramCooldown = tag.getInt("Scram");
        progress = tag.getInt("Progress");
        progressMax = tag.getInt("ProgressMax");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void setRemoved() {
        unregisterHub();
        super.setRemoved();
    }
}
