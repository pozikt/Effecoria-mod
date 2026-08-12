package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.effecoria.core.tower.TowerStructureValidator;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Mage Tower Ψ-anchor — consecrate glued hull, soulbind, revive chamber, Φ-dome. */
public final class TowerAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<TowerAnchorBlock> CODEC = simpleCodec(TowerAnchorBlock::new);

    public TowerAnchorBlock(Properties properties) {
        super(properties);
    }

    public static Properties props() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLUE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(4.0f, 12f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .lightLevel(s -> 9);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TowerAnchorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.TOWER_ANCHOR.get(), TowerAnchorBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)
                || !(player instanceof ServerPlayer sp)
                || !(level.getBlockEntity(pos) instanceof TowerAnchorBlockEntity anchor)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!TechnomagicGates.checkOperate(sp, TechnomagicEra.VI)) {
            return ItemInteractionResult.FAIL;
        }

        if (stack.is(ModItems.SOUL_SHARD.get())) {
            sp.displayClientMessage(Component.translatable("message.effecoria.tower.use_foundation_amulet"), true);
            return ItemInteractionResult.FAIL;
        }

        if (stack.is(ModItems.RESONANCE_FOCUS.get())) {
            if (!anchor.consecrated() || !anchor.bound()) {
                sp.displayClientMessage(Component.translatable("message.effecoria.tower.need_bind_for_dome"), true);
                return ItemInteractionResult.FAIL;
            }
            if (anchor.ownerUuid() == null || !anchor.ownerUuid().equals(sp.getUUID())) {
                sp.displayClientMessage(Component.translatable("message.effecoria.tower.not_owner"), true);
                return ItemInteractionResult.FAIL;
            }
            boolean combat = anchor.toggleDomeCombat();
            sp.displayClientMessage(
                    Component.translatable(
                            combat
                                    ? "message.effecoria.tower.dome_combat_on"
                                    : "message.effecoria.tower.dome_combat_off"),
                    true);
            return ItemInteractionResult.CONSUME;
        }

        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            if (anchor.tryInsert(copy)) {
                stack.setCount(copy.getCount());
                sp.displayClientMessage(Component.translatable("message.effecoria.tower.chamber_insert"), true);
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)
                || !(player instanceof ServerPlayer sp)
                || !(level.getBlockEntity(pos) instanceof TowerAnchorBlockEntity anchor)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (!TechnomagicGates.checkOperate(sp, TechnomagicEra.VI)) {
            return net.minecraft.world.InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            if (!anchor.consecrated()) {
                sp.displayClientMessage(Component.translatable("message.effecoria.tower.need_consecrate"), true);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            anchor.cycleBodyType();
            if (anchor.ownerUuid() != null && anchor.ownerUuid().equals(sp.getUUID())) {
                var data = com.effecoria.core.psi.PsiHelper.get(sp);
                data.setPreferredBodyType(anchor.bodyType());
                com.effecoria.core.psi.PsiHelper.set(sp, data);
            }
            sp.displayClientMessage(
                    Component.translatable("message.effecoria.tower.body_cycle", anchor.bodyType().getSerializedName()),
                    true);
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        if (!anchor.consecrated()) {
            TowerStructureValidator.Result result = TowerStructureValidator.validate(server, pos);
            if (!result.ok()) {
                sp.displayClientMessage(result.error(), true);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            anchor.applyConsecration(result);
            sp.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.tower.consecrated",
                            result.report().gluedCells(),
                            String.format("%.2f", result.verticality()),
                            String.format("%.2f", result.phiScatter()),
                            result.reactorClass().name().toLowerCase()),
                    true);
            return net.minecraft.world.InteractionResult.CONSUME;
        }

        anchor.refreshIntegrity(server);
        sp.displayClientMessage(anchor.statusLine(), true);
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}
