package com.effecoria.content;

import java.util.List;

import com.effecoria.core.disease.DiseaseService;
import com.effecoria.core.disease.PhiDisease;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.world.OmegaScarService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Void-obsidian Ω adsorber for the damper. Damage = saturation (0…{@link #MAX_SATURATION}).
 * Fully saturated rods bury on Ω-Scar like {@link OmegaWasteItem}.
 */
public final class OmegaRodItem extends Item {
    public static final int MAX_SATURATION = 2000;

    public OmegaRodItem(Properties properties) {
        super(properties.stacksTo(1).durability(MAX_SATURATION));
    }

    public static boolean isOmegaRod(ItemStack stack) {
        return stack.is(ModItems.OMEGA_ROD.get());
    }

    public static boolean isSaturated(ItemStack stack) {
        return isOmegaRod(stack) && stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static boolean canAbsorb(ItemStack stack) {
        return isOmegaRod(stack) && !stack.isEmpty() && !isSaturated(stack);
    }

    /** Absorb up to {@code centis} Ω; returns how much was taken into the rod. */
    public static int absorb(ItemStack stack, int centis) {
        if (!canAbsorb(stack) || centis <= 0) {
            return 0;
        }
        int room = stack.getMaxDamage() - stack.getDamageValue();
        int taken = Math.min(room, centis);
        stack.setDamageValue(stack.getDamageValue() + taken);
        return taken;
    }

    public static int saturationPercent(ItemStack stack) {
        if (!isOmegaRod(stack) || stack.getMaxDamage() <= 0) {
            return 0;
        }
        return Math.min(100, (stack.getDamageValue() * 100) / stack.getMaxDamage());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int pct = saturationPercent(stack);
        if (pct >= 75) {
            return 0xC44CFF;
        }
        if (pct >= 50) {
            return 0x8844CC;
        }
        if (pct >= 25) {
            return 0x553388;
        }
        return 0x2A2A2A;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!isSaturated(stack)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!isScarSurface(level, pos, state)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        buryEffect(server, pos, player);
        return InteractionResult.SUCCESS;
    }

    /** Convert one saturated rod into waste (helper). */
    public static ItemStack toWaste(ItemStack rod) {
        if (!isSaturated(rod)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ModItems.OMEGA_WASTE.get());
    }

    private static void buryEffect(ServerLevel server, BlockPos pos, @javax.annotation.Nullable Player player) {
        server.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                12,
                0.35,
                0.25,
                0.35,
                0.02);
        server.playSound(null, pos, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.BLOCKS, 0.5f, 0.7f);
        AABB box = new AABB(pos).inflate(1.5);
        for (ServerPlayer nearby : server.getEntitiesOfClass(ServerPlayer.class, box)) {
            PlayerPsiData data = PsiHelper.get(nearby);
            data.setEntropyB(Math.max(0f, data.entropyB() - 0.08f));
            PsiHelper.set(nearby, data);
            nearby.syncData(ModAttachments.PSI.get());
            DiseaseService.cure(nearby, PhiDisease.OMEGA_SICKNESS);
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.effecoria.omega_waste_buried"), true);
        }
    }

    private static boolean isScarSurface(Level level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.ROTTEN_MOSS.get())
                || state.is(ModBlocks.OMEGA_BLADES.get())
                || state.is(ModBlocks.OMEGA_TAINTED_OBSIDIAN.get())
                || state.is(ModBlocks.OMEGA_CRYSTAL.get())
                || state.is(ModBlocks.ELDRITCH_BLOOD_PUDDLE.get())) {
            return true;
        }
        return OmegaScarService.isBiome(level, pos);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int pct = saturationPercent(stack);
        tooltip.add(Component.translatable("item.effecoria.omega_rod.saturation", pct));
        String stageKey = pct >= 100
                ? "critical"
                : pct >= 75 ? "danger" : pct >= 50 ? "replace" : pct >= 25 ? "tint" : "fresh";
        tooltip.add(Component.translatable("item.effecoria.omega_rod.stage." + stageKey));
        if (pct >= 100) {
            tooltip.add(Component.translatable("item.effecoria.omega_rod.bury_hint"));
        } else {
            tooltip.add(Component.translatable("item.effecoria.omega_rod.hint"));
        }
    }
}
