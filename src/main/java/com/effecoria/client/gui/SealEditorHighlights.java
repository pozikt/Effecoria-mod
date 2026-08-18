package com.effecoria.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.effecoria.network.ModNetworking;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/** Client outlines for the open seal editor's glue symbols. */
public final class SealEditorHighlights {
    private static final List<ModNetworking.SealEditorMember> MEMBERS = new ArrayList<>();
    @Nullable
    private static BlockPos selected;
    @Nullable
    private static BlockPos anchor;

    private SealEditorHighlights() {}

    public static void set(List<ModNetworking.SealEditorMember> members, BlockPos anchorPos) {
        MEMBERS.clear();
        MEMBERS.addAll(members);
        anchor = anchorPos == null ? null : anchorPos.immutable();
    }

    public static void setSelected(@Nullable BlockPos pos) {
        selected = pos == null ? null : pos.immutable();
    }

    public static void clear() {
        MEMBERS.clear();
        selected = null;
        anchor = null;
    }

    public static List<ModNetworking.SealEditorMember> members() {
        return MEMBERS;
    }

    @Nullable
    public static BlockPos selected() {
        return selected;
    }

    @Nullable
    public static BlockPos anchor() {
        return anchor;
    }

    public static boolean active() {
        return !MEMBERS.isEmpty();
    }
}
