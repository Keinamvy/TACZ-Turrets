package com.entropy.tacz_turrets.client.renderer;

import com.entropy.tacz_turrets.client.CreateCogClock;
import com.entropy.tacz_turrets.client.model.TurretModel;
import com.entropy.tacz_turrets.config.TACZTurretsConfig;
import com.entropy.tacz_turrets.turret.TurretEntity;
import com.entropy.tacz_turrets.turret.TurretState;
import com.entropy.tacz_turrets.turret.TurretType;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class TurretRenderer<R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<TurretEntity, R> {
    public TurretRenderer(EntityRendererProvider.Context context) {
        super(context, new TurretModel());
        withRenderLayer(new TurretPaintLayer<>(this));
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected void applyRotations(RenderPassInfo<R> renderPassInfo, PoseStack poseStack, float nativeScale) {
        TurretEntity turret = renderPassInfo.getGeckolibData(TurretModel.TURRET);
        if (turret == null) return;
        float partialTick = renderPassInfo.renderState().getPartialTick();
        float center = turret.getBbHeight() / nativeScale / 2.0F;
        poseStack.translate(0.0F, center, 0.0F);
        poseStack.rotate(TurretEntity.frame(up(turret)));
        poseStack.translate(0.0F, -center, 0.0F);
        if (turret.deathTime > 0) {
            float deathRotation = (turret.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
            poseStack.rotate(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1.0F) * getDeathMaxRotation(renderPassInfo.renderState())));
        }
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        TurretEntity turret = renderPassInfo.getGeckolibData(TurretModel.TURRET);
        if (turret == null) return;
        float partialTick = renderPassInfo.renderState().getPartialTick();
        float yHeadRot = Mth.rotLerp(partialTick, turret.yHeadRotO, turret.getYHeadRot());
        float xRot = Mth.lerp(partialTick, turret.xRotO, turret.getXRot());
        Vector2f aim = aim(turret, yHeadRot, xRot);
        float yaw = 180.0F - aim.x;
        snapshots.ifPresent("head", head -> {
            head.setRotX((-aim.y - turret.getRecoilDegrees(partialTick)) * Mth.DEG_TO_RAD);
            head.setRotY(yaw * Mth.DEG_TO_RAD);
        });
        snapshots.ifPresent("center", center -> center.setRotY(yaw * Mth.DEG_TO_RAD));
        if (TurretType.BRASS.isAvailable()) {
            snapshots.ifPresent("cog", cog -> cog.setRotY(up(turret).getAxisDirection().getStep() * CreateCogClock.degrees(turret, partialTick) * Mth.DEG_TO_RAD));
        }
    }

    public static Direction up(TurretEntity turret) {
        Direction up = turret.getAttachFace().getOpposite();
        String customName = turret.getCustomName() != null ? ChatFormatting.stripFormatting(turret.getCustomName().getString()) : null;
        boolean upsideDown = "Dinnerbone".equals(customName) || "Grumm".equals(customName);
        return up == Direction.UP && upsideDown ? Direction.DOWN : up;
    }

    public static Vector2f aim(TurretEntity turret, float yaw, float pitch) {
        return TurretEntity.toLocal(up(turret), yaw, pitch);
    }

    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        super.postRenderPass(renderPassInfo, renderTasks);
        if (!renderPassInfo.willRender()) return;
        TurretEntity turret = renderPassInfo.getGeckolibData(TurretModel.TURRET);
        if (turret == null) return;

        float partialTick = renderPassInfo.renderState().getPartialTick();
        PoseStack poseStack = renderPassInfo.poseStack();
        BakedGeoModel model = renderPassInfo.model();
        float yHeadRot = Mth.rotLerp(partialTick, turret.yHeadRotO, turret.getYHeadRot());
        float xRot = Mth.lerp(partialTick, turret.xRotO, turret.getXRot());
        Vector2f aim = aim(turret, yHeadRot, xRot);

        if (turret.hasGun()) {
            poseStack.pushPose();
            applyRotations(renderPassInfo, poseStack, turret.getScale());
            model.getBone("gun").ifPresent(gun -> poseStack.translate(gun.pivotX() / 16f, gun.pivotY() / 16f, gun.pivotZ() / 16f));
            poseStack.rotate(Axis.YN.rotationDegrees(aim.x + 180));
            poseStack.rotate(Axis.XN.rotationDegrees(aim.y - turret.getRecoilDegrees(partialTick)));
            poseStack.translate(0.0F, 0.0F, turret.getRecoilPush(partialTick));
            if (turret.hasMinigun()) {
                poseStack.rotate(Axis.XN.rotationDegrees(90));
            }
            ItemStackRenderState gunState = RenderUtil.createRenderStateForItem(turret.getGunStack(), this.itemModelResolver, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, turret);
            gunState.submit(poseStack, renderTasks, renderPassInfo.packedLight(), renderPassInfo.packedOverlay(), renderPassInfo.renderState().outlineColor);
            poseStack.popPose();
        }

        TurretState state = TurretState.getState(turret);
        boolean visible = TACZTurretsConfig.SHOW_OTHERS_LASERS.get() || (Minecraft.getInstance().player != null && turret.isOwnerId(Minecraft.getInstance().player.getUUID()));
        if (visible && turret.isAlive() && turret.hasLaser() && state != TurretState.DISABLED && (state != TurretState.NO_GUN || TACZTurretsConfig.LASER_WITHOUT_GUN.get())) {
            model.getBone("laser").ifPresent(laserBone -> renderPassInfo.renderPosed(() -> {
                poseStack.pushPose();
                poseStack.last().set(renderPassInfo.getModelRenderMatrixPose());
                RenderUtil.transformToBone(poseStack, laserBone);
                Matrix4f boneMatrix = RenderUtil.extractPoseFromRoot(poseStack.last().pose(), renderPassInfo.getPreRenderMatrixState());
                poseStack.popPose();
                renderLaser(turret, boneMatrix, poseStack, renderTasks, partialTick);
            }));
        }
    }

    private static void renderLaser(TurretEntity turret, Matrix4f bone, PoseStack poseStack, SubmitNodeCollector renderTasks, float partialTick) {
        ItemStack laser = turret.getLaserStack();
        IAttachment attachment = IAttachment.getIAttachmentOrNull(laser);
        if (attachment == null) return;
        LaserConfig config = TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(laser)).map(ClientAttachmentIndex::getLaserConfig).orElse(null);
        if (config == null || config.getLengthThird() <= 0) return;

        Vector3f forward = bone.transformDirection(new Vector3f(0, 0, -1));
        Vec3 start = turret.getPosition(partialTick).add(new Vec3(bone.transformPosition(new Vector3f())));
        Vec3 end = start.add(new Vec3(forward).scale(config.getLengthThird() / forward.length()));
        Vec3 stop = turret.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, turret)).getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(turret, start, stop, new AABB(start, stop).inflate(1), entity -> !entity.isSpectator() && entity.isPickable(), start.distanceToSqr(stop));
        if (entityHit != null) stop = entityHit.getLocation();

        poseStack.pushPose();
        poseStack.last().pose().mul(bone);
        poseStack.scale(1, 1, (float) (start.distanceTo(stop) / forward.length()) / config.getLengthThird());
        BeamRenderer.renderLaserBeam(laser, poseStack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, List.of(), renderTasks);
        poseStack.popPose();
    }
}
