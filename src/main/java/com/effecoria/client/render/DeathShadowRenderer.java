package com.effecoria.client.render;

import java.util.Optional;
import java.util.UUID;

import com.effecoria.entity.DeathShadowEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders a half-body translucent player-skin shade. */
public class DeathShadowRenderer extends EntityRenderer<DeathShadowEntity> {
    private static final float ALPHA = 0.42f;

    private final PlayerModel<DeathShadowEntity> model;
    private final PlayerModel<DeathShadowEntity> slimModel;

    public DeathShadowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
        this.model = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(
            DeathShadowEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        PlayerSkin skin = resolveSkin(entity);
        PlayerModel<DeathShadowEntity> active =
                skin.model() == PlayerSkin.Model.SLIM ? slimModel : model;

        active.young = false;
        active.crouching = false;
        active.attackTime = 0f;
        active.setupAnim(entity, 0f, 0f, entity.tickCount + partialTick, 0f, 0f);
        applyHalfForm(active);

        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.pushPose();
        // Feet origin → lift truncated torso into a floating half-figure.
        poseStack.translate(0.0, 0.85, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0, -1.401f, 0.0);

        int color = Mth.floor(ALPHA * 255f) << 24 | 0x00FFFFFF;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(skin.texture()));
        active.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);

        // Soft hat layer if present in skin layout.
        if (active.hat.visible) {
            active.hat.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        }

        resetScales(active);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void applyHalfForm(PlayerModel<?> m) {
        m.setAllVisible(true);
        m.leftLeg.visible = false;
        m.rightLeg.visible = false;
        m.leftPants.visible = false;
        m.rightPants.visible = false;
        m.body.yScale = 0.5f;
        m.jacket.yScale = 0.5f;
        m.leftArm.yScale = 0.5f;
        m.rightArm.yScale = 0.5f;
        m.leftSleeve.yScale = 0.5f;
        m.rightSleeve.yScale = 0.5f;
        // Nudge arms/body so truncated pieces stay centered on the torso.
        m.body.y += 6.0f;
        m.jacket.y += 6.0f;
        m.leftArm.y += 3.0f;
        m.rightArm.y += 3.0f;
        m.leftSleeve.y += 3.0f;
        m.rightSleeve.y += 3.0f;
    }

    private static void resetScales(PlayerModel<?> m) {
        m.body.yScale = 1f;
        m.jacket.yScale = 1f;
        m.leftArm.yScale = 1f;
        m.rightArm.yScale = 1f;
        m.leftSleeve.yScale = 1f;
        m.rightSleeve.yScale = 1f;
        m.body.y = 0f;
        m.jacket.y = 0f;
        m.leftArm.y = 2.0f;
        m.rightArm.y = 2.0f;
        m.leftSleeve.y = 2.0f;
        m.rightSleeve.y = 2.0f;
    }

    private static PlayerSkin resolveSkin(DeathShadowEntity entity) {
        Optional<UUID> uuid = entity.skinUuid();
        String name = entity.skinName();
        if (uuid.isPresent()) {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                var info = connection.getPlayerInfo(uuid.get());
                if (info != null) {
                    return info.getSkin();
                }
            }
            GameProfile profile = new GameProfile(uuid.get(), name == null || name.isEmpty() ? "Steve" : name);
            return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
        }
        return DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(new byte[] {0}));
    }

    @Override
    public ResourceLocation getTextureLocation(DeathShadowEntity entity) {
        return resolveSkin(entity).texture();
    }
}
