package com.effecoria.mixin;

import com.effecoria.core.seal.SealLightOverlay;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge BlockState light query — overlay glow without changing BlockState. */
@Mixin(value = IBlockStateExtension.class, remap = false)
public interface IBlockStateExtensionMixin {
    @Inject(
            method = "getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true)
    default void effecoria$sealGlow(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int overlay = SealLightOverlay.emission(level, pos);
        if (overlay > cir.getReturnValueI()) {
            cir.setReturnValue(overlay);
        }
    }
}
