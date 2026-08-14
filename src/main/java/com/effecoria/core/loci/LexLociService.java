package com.effecoria.core.loci;

import com.effecoria.block.TowerAnchorBlockEntity;
import com.effecoria.block.TowerConsoleBlockEntity;
import com.effecoria.core.technomagic.TechnomagicEra;
import com.effecoria.core.technomagic.TechnomagicGates;
import com.effecoria.core.tower.TowerFacility;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Server apply for the Phoenix word program on a linked Ψ-anchor. */
public final class LexLociService {
    private LexLociService() {}

    public enum ApplyStatus {
        OK,
        NO_CONSOLE,
        NO_ANCHOR,
        NOT_OWNER,
        BAD_PROGRAM
    }

    public static ApplyStatus apply(ServerPlayer player, BlockPos consolePos, List<ResourceLocation> tokens) {
        if (!TechnomagicGates.checkOperate(player, TechnomagicEra.VI)) {
            return ApplyStatus.NO_CONSOLE;
        }
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(consolePos);
        if (!(be instanceof TowerConsoleBlockEntity console)) {
            return ApplyStatus.NO_CONSOLE;
        }
        TowerAnchorBlockEntity computer = TowerFacility.findComputer(level, consolePos).orElse(null);
        if (computer == null || !computer.bound() || computer.ownerUuid() == null) {
            return ApplyStatus.NO_ANCHOR;
        }
        if (!computer.ownerUuid().equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.effecoria.tower.not_owner"), true);
            return ApplyStatus.NOT_OWNER;
        }
        List<ResourceLocation> incoming = tokens == null ? List.of() : new ArrayList<>(tokens);
        if (incoming.size() > LexLociCompiler.MAX_TOKENS) {
            return ApplyStatus.BAD_PROGRAM;
        }
        boolean reset = incoming.isEmpty() || LexLociCompiler.isDefault(incoming);
        if (!reset) {
            LexLociCompiler.CompileResult compiled = LexLociCompiler.compile(incoming);
            if (!compiled.ok()) {
                return ApplyStatus.BAD_PROGRAM;
            }
        }
        computer.setLociTokens(reset ? List.of() : incoming);
        console.refreshTelemetry(level);
        player.displayClientMessage(
                Component.translatable(
                        reset
                                ? "message.effecoria.tower.loci_reset"
                                : "message.effecoria.tower.loci_applied"),
                true);
        return ApplyStatus.OK;
    }
}
