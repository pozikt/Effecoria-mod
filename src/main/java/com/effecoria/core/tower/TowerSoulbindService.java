package com.effecoria.core.tower;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.content.ModItems;
import com.effecoria.core.alchemy.PhiPower;
import com.effecoria.core.glue.EssenceGlueStructure;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Ritual that binds a mage's Ψ-operator to a consecrated tower anchor. */
public final class TowerSoulbindService {
    private static final float BLOOD_DAMAGE = 6.0f;

    private TowerSoulbindService() {}

    public static boolean bind(
            ServerPlayer player, ServerLevel level, TowerAnchorBlockEntity anchor, ItemStack shardStack) {
        if (!anchor.consecrated()) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.need_consecrate"), true);
            return false;
        }
        anchor.refreshIntegrity(level);
        if (anchor.integrity() < TowerStructureValidator.MIN_INTEGRITY) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.low_integrity_bind"), true);
            return false;
        }
        if (!PhiPower.hasPower(level, anchor.getBlockPos())) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.reactor_offline"), true);
            return false;
        }

        // Cost: soul shard in hand + pure essonite from player inventory + blood.
        if (!shardStack.is(ModItems.SOUL_SHARD.get())) {
            return false;
        }
        if (!consumeFromPlayer(player, ModItems.PURE_ESSONITE.get(), 1) && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.need_essonite"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) {
            shardStack.shrink(1);
        }
        player.hurt(player.damageSources().magic(), BLOOD_DAMAGE);

        // Clear previous tower binding if any.
        PlayerPsiData data = PsiHelper.get(player);
        if (data.towerBound() && data.towerPos() != null && data.towerDim() != null) {
            clearAnchorOwner(player.server, data.towerDim(), data.towerPos(), player.getUUID());
        }

        // If this anchor had another owner, clear their bind.
        if (anchor.ownerUuid() != null && !anchor.ownerUuid().equals(player.getUUID())) {
            clearPlayerBind(player.server, anchor.ownerUuid());
        }

        anchor.bindOwner(player.getUUID());
        data.bindTower(level.dimension(), anchor.getBlockPos().immutable(), anchor.bodyType());
        PsiHelper.set(player, data);
        player.syncData(ModAttachments.PSI.get());

        level.playSound(
                null,
                anchor.getBlockPos(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1.0f,
                0.85f);
        player.displayClientMessage(Component.translatable("message.effecoria.tower.bound"), true);
        return true;
    }

    private static boolean consumeFromPlayer(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int need = count;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                int take = Math.min(need, stack.getCount());
                stack.shrink(take);
                need -= take;
            }
        }
        return need == 0;
    }

    private static void clearAnchorOwner(
            net.minecraft.server.MinecraftServer server,
            ResourceKey<Level> dim,
            BlockPos pos,
            java.util.UUID expectedOwner) {
        ServerLevel towerLevel = server.getLevel(dim);
        if (towerLevel == null) {
            return;
        }
        BlockEntity be = towerLevel.getBlockEntity(pos);
        if (be instanceof TowerAnchorBlockEntity anchor
                && expectedOwner.equals(anchor.ownerUuid())) {
            anchor.unbind();
        }
    }

    private static void clearPlayerBind(net.minecraft.server.MinecraftServer server, java.util.UUID uuid) {
        ServerPlayer other = server.getPlayerList().getPlayer(uuid);
        if (other == null) {
            return;
        }
        PlayerPsiData data = PsiHelper.get(other);
        data.clearTowerBind();
        PsiHelper.set(other, data);
        other.syncData(ModAttachments.PSI.get());
    }

    public static boolean towerAliveFor(ServerPlayer player) {
        PlayerPsiData data = PsiHelper.get(player);
        if (!data.towerBound() || data.towerDim() == null || data.towerPos() == null) {
            return false;
        }
        ServerLevel towerLevel = player.server.getLevel(data.towerDim());
        if (towerLevel == null) {
            return false;
        }
        BlockEntity be = towerLevel.getBlockEntity(data.towerPos());
        if (!(be instanceof TowerAnchorBlockEntity anchor) || !anchor.consecrated() || !anchor.bound()) {
            return false;
        }
        if (!player.getUUID().equals(anchor.ownerUuid())) {
            return false;
        }
        EssenceGlueStructure.Report report =
                EssenceGlueStructure.inspect(towerLevel, data.towerPos());
        if (report.integrity() < TowerStructureValidator.MIN_INTEGRITY) {
            return false;
        }
        return PhiPower.hasPower(towerLevel, data.towerPos());
    }
}
