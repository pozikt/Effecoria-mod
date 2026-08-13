package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;
import com.effecoria.core.tower.RegenChamberMultiblock;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Controller for the Mage Tower regen capsule multiblock. */
public final class RegenChamberBlockEntity extends BlockEntity {
    public static final int CAPACITY = RegenChamberMultiblock.CAPACITY;

    private boolean formed;
    private boolean dismantling;
    private int fillAmount;

    public RegenChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGEN_CHAMBER.get(), pos, state);
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

    public int fillAmount() {
        return fillAmount;
    }

    public float fillRatio() {
        return CAPACITY <= 0 ? 0f : fillAmount / (float) CAPACITY;
    }

    public boolean isFull() {
        return fillAmount >= CAPACITY;
    }

    public boolean isOperational() {
        return formed && isFull();
    }

    public void onDisassembled() {
        if (level instanceof ServerLevel server) {
            RegenChamberMultiblock.clearBathFluid(server, worldPosition);
        }
        fillAmount = 0;
    }

    public void tryShowStatus(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.regen_chamber.status",
                            formed ? 1 : 0,
                            fillAmount,
                            CAPACITY),
                    true);
        }
    }

    /** Insert one bucket of purified Φ-water into the next empty cavity cell. */
    public boolean tryFill(ServerPlayer player, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || !formed || isFull()
                || !stack.is(ModItems.PURIFIED_PHI_WATER_BUCKET.get())) {
            return false;
        }
        if (!RegenChamberMultiblock.placeBathFluid(server, worldPosition)) {
            fillAmount = RegenChamberMultiblock.countBathFluid(server, worldPosition);
            return false;
        }
        fillAmount = RegenChamberMultiblock.countBathFluid(server, worldPosition);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f, 1.1f);
        setChanged();
        sync();
        if (isFull()) {
            player.displayClientMessage(Component.translatable("message.effecoria.regen_chamber.full"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.regen_chamber.filled", fillAmount, CAPACITY), true);
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RegenChamberBlockEntity be) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        if (!be.formed) {
            if (RegenChamberMultiblock.isMaterialShell(level, pos)) {
                RegenChamberMultiblock.assemble(server, pos);
            }
        } else if (!RegenChamberMultiblock.isAssembled(level, pos)) {
            RegenChamberMultiblock.disassemble(server, pos);
        }

        if (be.formed) {
            int counted = RegenChamberMultiblock.countBathFluid(server, pos);
            if (counted != be.fillAmount) {
                be.fillAmount = counted;
                be.setChanged();
                be.sync();
            }
        }

        BlockState current = level.getBlockState(pos);
        if (current.is(com.effecoria.content.ModBlocks.REGEN_CHAMBER.get())
                && current.getValue(RegenChamberBlock.FORMED) != be.formed) {
            level.setBlock(pos, current.setValue(RegenChamberBlock.FORMED, be.formed), Block.UPDATE_CLIENTS);
        }

        if (!be.formed || be.fillAmount() <= 0 || server.getGameTime() % 20 != 0) {
            return;
        }

        boolean towerOk = TowerFacility.findComputer(server, pos)
                .filter(a -> a.consecrated() && a.bound())
                .isPresent();
        if (!towerOk) {
            return;
        }
        if (!PhiPower.consumeTick(level, pos, 3)) {
            return;
        }

        AABB box = RegenChamberMultiblock.interiorAabb(pos);
        boolean fullBath = be.isOperational();
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            restoreHunger(player);
            if (fullBath) {
                applyHealing(server, player);
            }
        }
    }

    /** Standing in the capsule slowly restores hunger even if the bath is not full. */
    private static void restoreHunger(Player player) {
        var food = player.getFoodData();
        if (food.getFoodLevel() < 20) {
            food.setFoodLevel(Math.min(20, food.getFoodLevel() + 2));
        }
        if (food.getSaturationLevel() < food.getFoodLevel()) {
            food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + 1.0f));
        }
        player.removeEffect(MobEffects.HUNGER);
    }

    private static void applyHealing(ServerLevel server, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
        player.clearFire();
        if (player.getTicksFrozen() > 0) {
            player.setTicksFrozen(0);
        }

        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.DARKNESS);

        if (player instanceof ServerPlayer sp) {
            cureCurableDiseases(sp);
        }
    }

    private static final List<PhiDisease> ALWAYS_CURABLE = List.of(
            PhiDisease.ESSENCE_BURN,
            PhiDisease.ESSENTOCYTOSIS,
            PhiDisease.OMEGA_SICKNESS,
            PhiDisease.DUST_LUNG,
            PhiDisease.OMEGA_ROT,
            PhiDisease.CRYSTAL_FEVER,
            PhiDisease.GHOST_ECHO);

    private static void cureCurableDiseases(ServerPlayer player) {
        for (PhiDisease disease : ALWAYS_CURABLE) {
            if (DiseaseService.get(player).has(disease)) {
                DiseaseService.cure(player, disease);
            }
        }
        // Mild stages only for hard pathologies
        for (PhiDisease hard : List.of(PhiDisease.ORKANUMN_ATROPHY, PhiDisease.SOUL_DISSONANCE, PhiDisease.CURSE_ROT)) {
            var inst = DiseaseService.get(player).get(hard);
            if (inst != null && inst.stage() <= 1) {
                DiseaseService.cure(player, hard);
            }
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Formed", formed);
        tag.putInt("Fill", fillAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        formed = tag.getBoolean("Formed");
        fillAmount = tag.getInt("Fill");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }
}
