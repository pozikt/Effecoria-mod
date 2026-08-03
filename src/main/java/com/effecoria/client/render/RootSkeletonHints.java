package com.effecoria.client.render;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client wrap anchors for the GeckoLib root cage.
 *
 * <p>Vanilla mobs are not Geo skeletons, so we cannot read true bone chains for every entity.
 * Instead we pin wrap bones to a biped layout in Blockbench units (16 = 1 block, reference height
 * 1.8) and refine arm/leg lateral placement from {@link HumanoidModel} when the captive uses one.
 * The renderer then scales the whole model to the cage hitbox.
 */
public final class RootSkeletonHints {
    public static final RootSkeletonHints REFERENCE = new RootSkeletonHints(
            0f, 24f, 0f, 0f, 16f, 0f, 6f, 18f, 0f, -6f, 18f, 0f, 2.5f, 6f, 0f, -2.5f, 6f, 0f, false);

    public final float neckX, neckY, neckZ;
    public final float torsoX, torsoY, torsoZ;
    public final float armLX, armLY, armLZ;
    public final float armRX, armRY, armRZ;
    public final float legLX, legLY, legLZ;
    public final float legRX, legRY, legRZ;
    public final boolean fromSkeleton;

    private RootSkeletonHints(
            float neckX,
            float neckY,
            float neckZ,
            float torsoX,
            float torsoY,
            float torsoZ,
            float armLX,
            float armLY,
            float armLZ,
            float armRX,
            float armRY,
            float armRZ,
            float legLX,
            float legLY,
            float legLZ,
            float legRX,
            float legRY,
            float legRZ,
            boolean fromSkeleton) {
        this.neckX = neckX;
        this.neckY = neckY;
        this.neckZ = neckZ;
        this.torsoX = torsoX;
        this.torsoY = torsoY;
        this.torsoZ = torsoZ;
        this.armLX = armLX;
        this.armLY = armLY;
        this.armLZ = armLZ;
        this.armRX = armRX;
        this.armRY = armRY;
        this.armRZ = armRZ;
        this.legLX = legLX;
        this.legLY = legLY;
        this.legLZ = legLZ;
        this.legRX = legRX;
        this.legRY = legRY;
        this.legRZ = legRZ;
        this.fromSkeleton = fromSkeleton;
    }

    public static RootSkeletonHints forCaptive(@Nullable LivingEntity captive, float partialTick) {
        if (captive == null) {
            return REFERENCE;
        }
        RootSkeletonHints humanoid = tryHumanoid(captive, partialTick);
        return humanoid != null ? humanoid : REFERENCE;
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RootSkeletonHints tryHumanoid(LivingEntity captive, float partialTick) {
        EntityRenderer<?> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(captive);
        if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
            return null;
        }
        EntityModel<?> model = livingRenderer.getModel();
        if (!(model instanceof HumanoidModel humanoid)) {
            return null;
        }

        float limbSwing = captive.walkAnimation.position(partialTick);
        float limbSwingAmount = captive.walkAnimation.speed(partialTick);
        float ageInTicks = captive.tickCount + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, captive.yBodyRotO, captive.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, captive.yHeadRotO, captive.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;
        float headPitch = Mth.lerp(partialTick, captive.xRotO, captive.getXRot());
        try {
            humanoid.setupAnim(captive, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        } catch (Throwable ignored) {
            return null;
        }

        // Lateral limb placement from the live humanoid parts; vertical stays on the authored biped.
        float armLX = Mth.clamp(Math.abs(humanoid.leftArm.x) + 1.0f, 4.0f, 11.0f);
        float armRX = -Mth.clamp(Math.abs(humanoid.rightArm.x) + 1.0f, 4.0f, 11.0f);
        float legLX = Mth.clamp(Math.abs(humanoid.leftLeg.x) + 0.5f, 1.5f, 5.0f);
        float legRX = -Mth.clamp(Math.abs(humanoid.rightLeg.x) + 0.5f, 1.5f, 5.0f);
        float armZ = Mth.clamp(humanoid.leftArm.z, -3f, 3f);
        float neckZ = Mth.clamp(humanoid.head.z, -3f, 3f);

        return new RootSkeletonHints(
                0f,
                24f,
                neckZ,
                0f,
                16f,
                0f,
                armLX,
                18f,
                armZ,
                armRX,
                18f,
                armZ,
                legLX,
                6f,
                0f,
                legRX,
                6f,
                0f,
                true);
    }
}
