package com.effecoria.content;

import java.util.List;

import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Φ-conductive dust — emergency Ψ, Φ-soil fertilizer, and craft ingredient. */
public final class EssoniteDustItem extends Item {
    private static final int COOLDOWN_TICKS = 60;

    public EssoniteDustItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!isPhiGrowable(state)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        boolean used = false;
        if (state.is(ModBlocks.PHI_SAPLING.get())) {
            // applyBonemeal already shrinks the stack on success
            used = BoneMealItem.applyBonemeal(stack.copy(), level, pos, player);
            if (used && player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        } else if (state.is(ModBlocks.PHI_DIRT.get()) || state.is(ModBlocks.PHI_GRASS.get())) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                level.setBlock(above, ModBlocks.PHI_BLADES.get().defaultBlockState(), 3);
                used = true;
            }
        } else if (state.is(ModBlocks.PHI_BLADES.get())) {
            used = trySpreadBlades(server, pos);
        }

        if (!used) {
            return InteractionResult.FAIL;
        }
        if (!state.is(ModBlocks.PHI_SAPLING.get())
                && player != null
                && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.levelEvent(1505, pos, 0);
        server.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.9f, 1.15f);
        return InteractionResult.SUCCESS;
    }

    private static boolean trySpreadBlades(ServerLevel level, BlockPos origin) {
        for (int i = 0; i < 8; i++) {
            BlockPos target = origin.offset(
                    level.random.nextInt(3) - 1, 0, level.random.nextInt(3) - 1);
            if (target.equals(origin)) {
                continue;
            }
            BlockState below = level.getBlockState(target.below());
            if (level.getBlockState(target).isAir()
                    && (below.is(ModBlocks.PHI_DIRT.get()) || below.is(ModBlocks.PHI_GRASS.get()))) {
                level.setBlock(target, ModBlocks.PHI_BLADES.get().defaultBlockState(), 3);
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Prefer fertilize when looking at a block; only empty air / miss uses emergency Ψ.
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        PlayerPsiData data = PsiHelper.get(serverPlayer);
        if (data.initiated()) {
            float gain = Math.max(6f, data.maxPsi() * 0.12f);
            data.setCurrentPsi(data.currentPsi() + gain);
            PsiHelper.set(serverPlayer, data);
            serverPlayer.syncData(ModAttachments.PSI.get());
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.55f,
                    1.55f);
            player.displayClientMessage(Component.translatable("message.effecoria.essonite_dust_charged"), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            player.hurt(player.damageSources().magic(), 2f);
            player.displayClientMessage(Component.translatable("message.effecoria.essonite_dust_poison"), true);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static boolean isPhiGrowable(BlockState state) {
        return state.is(ModBlocks.PHI_DIRT.get())
                || state.is(ModBlocks.PHI_GRASS.get())
                || state.is(ModBlocks.PHI_BLADES.get())
                || state.is(ModBlocks.PHI_SAPLING.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.effecoria.essonite_dust.hint"));
    }
}
