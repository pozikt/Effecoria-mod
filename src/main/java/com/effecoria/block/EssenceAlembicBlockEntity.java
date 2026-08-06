package com.effecoria.block;

import com.effecoria.content.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EssenceAlembicBlockEntity extends BlockEntity {
    private ItemStack base = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private Item cookingResultItem;
    private int cookProgress;

    public EssenceAlembicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_ALEMBIC.get(), pos, state);
    }

    public ItemStack getBase() {
        return base;
    }

    public ItemStack getResult() {
        return result;
    }

    public boolean isCooking() {
        return cookingResultItem != null && cookProgress < EssenceAlembicBlock.COOK_TIME;
    }

    public void setBase(ItemStack stack) {
        base = stack.copyWithCount(1);
        setChanged();
        sync();
    }

    public void startCook(Item potion) {
        cookingResultItem = potion;
        cookProgress = 0;
        setChanged();
        sync();
    }

    public ItemStack takeResult() {
        ItemStack out = result.copy();
        result = ItemStack.EMPTY;
        setChanged();
        sync();
        return out;
    }

    public void drops(Level level, BlockPos pos) {
        if (!base.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), base);
        }
        if (!result.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), result);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceAlembicBlockEntity be) {
        if (be.cookingResultItem == null || !(level instanceof ServerLevel server)) {
            return;
        }
        if (be.cookProgress >= EssenceAlembicBlock.COOK_TIME) {
            return;
        }
        if (!EssenceBurnerBlock.consumeNeighborHeat(server, pos)) {
            return;
        }
        be.cookProgress++;
        if (be.cookProgress >= EssenceAlembicBlock.COOK_TIME) {
            be.base = ItemStack.EMPTY;
            be.result = new ItemStack(be.cookingResultItem);
            be.cookingResultItem = null;
            be.cookProgress = 0;
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 0.9f);
            be.setChanged();
            be.sync();
        } else if (be.cookProgress % 20 == 0) {
            be.setChanged();
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!base.isEmpty()) {
            tag.put("Base", base.save(registries));
        }
        if (!result.isEmpty()) {
            tag.put("Result", result.save(registries));
        }
        tag.putInt("Cook", cookProgress);
        if (cookingResultItem != null) {
            tag.putString("CookItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cookingResultItem).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        base = tag.contains("Base")
                ? ItemStack.parse(registries, tag.getCompound("Base")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        result = tag.contains("Result")
                ? ItemStack.parse(registries, tag.getCompound("Result")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        cookProgress = tag.getInt("Cook");
        cookingResultItem = null;
        if (tag.contains("CookItem")) {
            cookingResultItem = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(net.minecraft.resources.ResourceLocation.parse(tag.getString("CookItem")));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
