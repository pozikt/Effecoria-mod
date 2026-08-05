package com.effecoria.block;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** Dark Φ-wood with gold veins — alarm flash when felled. */
public final class PhiLogBlock extends RotatedPillarBlock {
    public PhiLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.isDay() && random.nextInt(10) == 0) {
            level.addParticle(
                    ModParticleTypes.PHI_SPARK.get(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0,
                    0.02,
                    0);
        }
    }

    @Override
    public void playerDestroy(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(
                ModParticleTypes.ELEMENTAL_PLASMA.get(),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                24,
                0.6,
                0.8,
                0.6,
                0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9f, 0.7f);
        AABB box = new AABB(pos).inflate(8.0);
        for (Player nearby : level.getEntitiesOfClass(Player.class, box)) {
            if (nearby == player) {
                continue;
            }
            BreathDebuffs.apply(
                    nearby, new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, true));
            BreathDebuffs.apply(
                    nearby, new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
        }
    }
}
