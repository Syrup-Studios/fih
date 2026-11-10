package net.syrupstudios.fih.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.syrupstudios.fih.client.HeldFishRenderer;
import net.syrupstudios.fih.client.HeldFishPose;
import com.mojang.math.Axis;
import net.syrupstudios.fih.ai.HeldFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mixin(ItemInHandRenderer.class)
public abstract class HeldFishRenderMixin {
    @Unique private static final Vector3f fih$leftEquipDirection = fih$equipDirection(1.0F);
    @Unique private static final Vector3f fih$rightEquipDirection = fih$equipDirection(-1.0F);

    @Unique
    private static Vector3f fih$equipDirection(float mirror) {
        Quaternionf rotation = new Quaternionf()
                .rotationX(HeldFishPose.armPitch * Mth.DEG_TO_RAD)
                .rotateY(mirror * HeldFishPose.armYaw * Mth.DEG_TO_RAD)
                .rotateZ(mirror * HeldFishPose.armAngle * Mth.DEG_TO_RAD);
        return new Vector3f(0.0F, -0.6F, 0.0F).rotate(rotation);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void fih$renderHoldingArm(AbstractClientPlayer player, float partialTick, float pitch,
            InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!HeldFish.isFish(stack)) return;
        if (player.isScoping()) { ci.cancel(); return; }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float mirror = arm == HumanoidArm.LEFT ? 1.0F : -1.0F;
        Vector3f equipDirection = arm == HumanoidArm.LEFT ? fih$leftEquipDirection : fih$rightEquipDirection;
        pose.pushPose();
        pose.translate(mirror * HeldFishPose.sharedX, HeldFishPose.sharedY, HeldFishPose.sharedZ);
        pose.translate(equipDirection.x * equipProgress, equipDirection.y * equipProgress, equipDirection.z * equipProgress);
        float side = -mirror;
        float swingRoot = Mth.sqrt(swingProgress);
        float swingArc = Mth.sin(swingRoot * Mth.PI);
        pose.translate(side * -0.3F * swingArc,
                0.4F * Mth.sin(swingRoot * Mth.TWO_PI), -0.4F * Mth.sin(swingProgress * Mth.PI));
        pose.translate(side * 0.56F, -0.52F, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(side * swingArc * 70.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swingProgress * swingProgress * Mth.PI) * -20.0F));
        pose.translate(side * -0.56F, 0.52F, 0.72F);
        if (!player.isInvisible()) {
            pose.pushPose();
            pose.translate(mirror * HeldFishPose.armX, HeldFishPose.armY, HeldFishPose.armZ);
            pose.translate(mirror * -0.56, -0.52, -0.72);
            pose.mulPose(Axis.XP.rotationDegrees(HeldFishPose.armPitch));
            pose.mulPose(Axis.YP.rotationDegrees(mirror * HeldFishPose.armYaw));
            pose.mulPose(Axis.ZP.rotationDegrees(mirror * HeldFishPose.armAngle));
            pose.translate(mirror * 0.56, 0.52, 0.72);
            ((ItemInHandRendererAccess) this).fih$renderPlayerArm(pose, buffers, light, 0.0F, 0.0F, arm);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(side * 0.56F, -0.52F, -0.72F);
        HeldFishRenderer.render(stack, arm == HumanoidArm.LEFT
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                arm == HumanoidArm.LEFT, pose, buffers, light);
        pose.popPose();
        pose.popPose();
        ci.cancel();
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void fih$renderHeldFish(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof Player && HeldFishRenderer.render(stack, context, leftHand, pose, buffers, light)) {
            ci.cancel();
        }
    }
}
