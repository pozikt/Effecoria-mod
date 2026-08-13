package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.technomagic.TelegraphService;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Era III Φ-telegraph — pair two blocks, pulse short messages same-dimension. */
public final class PhiTelegraphBlock extends BaseEntityBlock {
    public static final MapCodec<PhiTelegraphBlock> CODEC = simpleCodec(PhiTelegraphBlock::new);

    public PhiTelegraphBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhiTelegraphBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.PHI_TELEGRAPH.get(), PhiTelegraphBlockEntity::serverTick);
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
        if (!(level.getBlockEntity(pos) instanceof PhiTelegraphBlockEntity telegraph)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel server)) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!com.effecoria.core.technomagic.TechnomagicGates.checkOperate(
                serverPlayer, com.effecoria.core.technomagic.TechnomagicEra.III)) {
            return ItemInteractionResult.FAIL;
        }

        // Unlink
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            telegraph.clearLink();
            player.displayClientMessage(Component.translatable("message.effecoria.telegraph_unlinked"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // Linked telegraph → remote tower console (empty hand)
        if (stack.isEmpty() && telegraph.hasLink()) {
            if (com.effecoria.core.tower.TowerRemoteService.tryOpenRemoteFromTelegraph(
                    serverPlayer, server, telegraph)) {
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Pairing with empty hand
        if (stack.isEmpty()) {
            TelegraphService.handlePairClick(serverPlayer, server, pos, telegraph);
            return ItemInteractionResult.SUCCESS;
        }

        // Insert / swap cell
        if (stack.is(ModItems.PHI_CELL.get())) {
            ItemStack old = telegraph.takeCell();
            telegraph.setCell(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!old.isEmpty() && !player.getInventory().add(old)) {
                player.drop(old, false);
            }
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.4f);
            return ItemInteractionResult.SUCCESS;
        }

        // Extract cell
        if (stack.isEmpty()) {
            // already handled
        }

        // Pulse with flask / named paper-like phi flask
        if (stack.is(ModItems.PHI_FLASK.get()) || stack.is(ModItems.PHI_PAPER.get())) {
            String message = stack.getHoverName().getString();
            if (message.length() > 40) {
                message = message.substring(0, 40);
            }
            if (TelegraphService.sendPulse(serverPlayer, server, telegraph, message)) {
                if (!player.getAbilities().instabuild && stack.is(ModItems.PHI_FLASK.get())) {
                    // flask not consumed; paper is
                }
                if (!player.getAbilities().instabuild && stack.is(ModItems.PHI_PAPER.get())) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PhiTelegraphBlockEntity telegraph) {
            ItemStack cell = telegraph.takeCell();
            if (!cell.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cell);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static final class PhiTelegraphBlockEntity extends BlockEntity {
        public static final float PULSE_CELL_COST = 0.05f;
        private ItemStack cell = ItemStack.EMPTY;
        @Nullable
        private ResourceKey<Level> linkedDim;
        @Nullable
        private BlockPos linkedPos;
        private int pulseCooldown;
        private String lastMessage = "";

        public PhiTelegraphBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.PHI_TELEGRAPH.get(), pos, state);
        }

        public boolean hasLink() {
            return linkedDim != null && linkedPos != null;
        }

        @Nullable
        public ResourceKey<Level> linkedDim() {
            return linkedDim;
        }

        @Nullable
        public BlockPos linkedPos() {
            return linkedPos;
        }

        public void setLink(ResourceKey<Level> dim, BlockPos pos) {
            this.linkedDim = dim;
            this.linkedPos = pos.immutable();
            setChanged();
        }

        public void clearLink() {
            linkedDim = null;
            linkedPos = null;
            setChanged();
        }

        public ItemStack takeCell() {
            ItemStack stack = cell;
            cell = ItemStack.EMPTY;
            setChanged();
            return stack;
        }

        public void setCell(ItemStack stack) {
            cell = stack;
            setChanged();
        }

        public ItemStack cell() {
            return cell;
        }

        public boolean tryConsumeCell() {
            if (cell.isEmpty() || !cell.is(ModItems.PHI_CELL.get())) {
                return false;
            }
            float charge = PhiHarnessItems.cellCharge(cell);
            if (charge < PULSE_CELL_COST) {
                return false;
            }
            PhiHarnessItems.setCellCharge(cell, charge - PULSE_CELL_COST);
            setChanged();
            return true;
        }

        public boolean onCooldown() {
            return pulseCooldown > 0;
        }

        public void beginCooldown(int ticks) {
            pulseCooldown = ticks;
            setChanged();
        }

        public void setLastMessage(String message) {
            lastMessage = message == null ? "" : message;
            setChanged();
        }

        public String lastMessage() {
            return lastMessage;
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, PhiTelegraphBlockEntity be) {
            if (be.pulseCooldown > 0) {
                be.pulseCooldown--;
                if (be.pulseCooldown % 20 == 0) {
                    be.setChanged();
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.saveAdditional(tag, provider);
            if (!cell.isEmpty()) {
                tag.put("Cell", cell.save(provider));
            }
            if (linkedDim != null && linkedPos != null) {
                tag.putString("LinkDim", linkedDim.location().toString());
                tag.putInt("LinkX", linkedPos.getX());
                tag.putInt("LinkY", linkedPos.getY());
                tag.putInt("LinkZ", linkedPos.getZ());
            }
            tag.putInt("Cooldown", pulseCooldown);
            tag.putString("LastMsg", lastMessage);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
            super.loadAdditional(tag, provider);
            cell = tag.contains("Cell")
                    ? ItemStack.parse(provider, tag.getCompound("Cell")).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY;
            if (tag.contains("LinkDim")) {
                linkedDim = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(tag.getString("LinkDim")));
                linkedPos = new BlockPos(tag.getInt("LinkX"), tag.getInt("LinkY"), tag.getInt("LinkZ"));
            } else {
                linkedDim = null;
                linkedPos = null;
            }
            pulseCooldown = tag.getInt("Cooldown");
            lastMessage = tag.getString("LastMsg");
        }
    }
}
