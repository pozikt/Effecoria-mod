package com.effecoria.entity;

import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Ω-Worm — oversized Scar silverfish that feeds on Ω-soil. */
public class OmegaWormEntity extends Silverfish {
    public OmegaWormEntity(EntityType<? extends OmegaWormEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static boolean checkWormSpawn(
            EntityType<OmegaWormEntity> type,
            ServerLevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(ModBlocks.ASH_SOIL.get()) || below.is(ModBlocks.VOID_OBSIDIAN.get());
        return ground && checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (random.nextFloat() < 0.4f) {
            spawnAtLocation(new ItemStack(ModItems.OMEGA_CRYSTAL_SHARD.get()));
        }
        if (random.nextFloat() < 0.25f) {
            spawnAtLocation(new ItemStack(ModItems.DISTORTED_BONE.get()));
        }
    }
}
