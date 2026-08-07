package com.effecoria.client;

import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;

/** Client mirror of the active environmental matter bond for HUD / outline cues. */
public final class ClientMatterBondState {
    private static boolean active;
    private static BlockPos source = BlockPos.ZERO;
    private static String kind = "none";
    private static float strength;

    private ClientMatterBondState() {}

    public static void apply(ModNetworking.MatterBondSyncPayload payload) {
        active = payload.active();
        if (active) {
            source = new BlockPos(payload.x(), payload.y(), payload.z());
            kind = payload.kind();
            strength = payload.strength();
        } else {
            kind = "none";
            strength = 0f;
        }
    }

    public static boolean active() {
        return active;
    }

    public static BlockPos source() {
        return source;
    }

    public static String kind() {
        return kind;
    }

    public static float strength() {
        return strength;
    }
}
