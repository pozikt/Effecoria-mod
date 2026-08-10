package com.effecoria.core.technomagic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.entity.PhiConstructEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Binds one Φ-construct per player; entity drains Φ-cell charge while active. */
public final class ConstructBindingService {
    public static final String OWNER_TAG = "effecoria:construct_owner";
    public static final float CHARGE_DRAIN_PER_SEC = 0.004f;

    private ConstructBindingService() {}

    public static boolean isOwnedBy(PhiConstructEntity construct, UUID ownerId) {
        return construct.getPersistentData().hasUUID(OWNER_TAG)
                && ownerId.equals(construct.getPersistentData().getUUID(OWNER_TAG));
    }

    public static boolean register(PhiConstructEntity construct, ServerPlayer owner) {
        PlayerPsiData data = PsiHelper.get(owner);
        for (UUID oldId : new ArrayList<>(data.constructIds())) {
            if (oldId.equals(construct.getUUID())) {
                continue;
            }
            despawnConstruct(owner.serverLevel(), oldId);
            data.untrackConstruct(oldId);
            notifyLimit(owner);
        }
        construct.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        construct.setPersistenceRequired();
        construct.setOwnerUUID(owner.getUUID());
        data.trackConstruct(construct.getUUID());
        PsiHelper.set(owner, data);
        return true;
    }

    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            PlayerPsiData data = PsiHelper.get(player);
            if (data.constructIds().isEmpty()) {
                continue;
            }
            boolean dirty = false;
            for (UUID id : new ArrayList<>(data.constructIds())) {
                var entity = level.getEntity(id);
                if (!(entity instanceof PhiConstructEntity construct) || !construct.isAlive()) {
                    data.untrackConstruct(id);
                    dirty = true;
                }
            }
            if (dirty) {
                PsiHelper.set(player, data);
            }
        }
    }

    public static boolean consumeChargeTick(ServerPlayer owner) {
        ItemStack cell = PhiHarnessItems.findPhiCell(owner);
        if (cell.isEmpty()) {
            return false;
        }
        float charge = PhiHarnessItems.cellCharge(cell);
        if (charge < CHARGE_DRAIN_PER_SEC) {
            PhiHarnessItems.setCellCharge(cell, 0f);
            return false;
        }
        PhiHarnessItems.setCellCharge(cell, charge - CHARGE_DRAIN_PER_SEC);
        return true;
    }

    public static void despawnConstruct(ServerLevel level, UUID id) {
        var entity = level.getEntity(id);
        if (entity instanceof PhiConstructEntity construct) {
            construct.discard();
        }
    }

    public static void notifyLimit(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.effecoria.construct_replaced"), true);
    }
}
