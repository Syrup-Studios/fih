package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.resources.model.BakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.syrupstudios.fih.ai.FishingHookAccess;
import net.syrupstudios.fih.client.FishingRodMotion;

@Mixin(FishingHookRenderer.class)
public abstract class FishingLineRendererMixin {
    @Redirect(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;stringVertex(FFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FF)V"))
    private void fih$renderLine(float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose,
            float fraction, float nextFraction, FishingHook hook, float yaw, float partialTick,
            PoseStack stack, MultiBufferSource buffers, int light) {
        float tension = Math.min(1.0F, ((FishingHookAccess) hook).fih$visualTension(partialTick) * 2.0F);
        float curve = Mth.lerp(tension, (fraction * fraction + fraction) * 0.5F, fraction);
        float nextCurve = Mth.lerp(tension, (nextFraction * nextFraction + nextFraction) * 0.5F, nextFraction);
        float normalX = x * (nextFraction - fraction);
        float normalY = y * (nextCurve - curve);
        float normalZ = z * (nextFraction - fraction);
        float length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length > 1.0E-6F) {
            normalX /= length;
            normalY /= length;
            normalZ /= length;
        }
        consumer.addVertex(pose, x * fraction, y * curve + 0.25F, z * fraction)
                .setColor(0xFFC0C0C0).setNormal(pose, normalX, normalY, normalZ);
    }

    @Inject(method = "getPlayerHandPos", at = @At("RETURN"), cancellable = true)
    private void fih$alignFirstPersonLine(Player player, float swing, float partialTick,
            CallbackInfoReturnable<Vec3> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player != minecraft.player || !minecraft.options.getCameraType().isFirstPerson()
                || !(player instanceof LocalPlayer localPlayer) || player.isAutoSpinAttack()) return;

        ItemInHandRendererAccess hands = (ItemInHandRendererAccess) minecraft.gameRenderer.itemInHandRenderer;
        boolean mainHand = player.getMainHandItem().is(Items.FISHING_ROD);
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack renderedRod = mainHand ? hands.fih$mainHandItem() : hands.fih$offHandItem();
        if (!renderedRod.is(Items.FISHING_ROD)) return;
        HumanoidArm arm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean leftHand = arm == HumanoidArm.LEFT;
        int side = leftHand ? -1 : 1;

        float height = mainHand
                ? Mth.lerp(partialTick, hands.fih$oldMainHandHeight(), hands.fih$mainHandHeight())
                : Mth.lerp(partialTick, hands.fih$oldOffHandHeight(), hands.fih$offHandHeight());
        float equipProgress = 1.0F - height;
        float armSwing = player.swingingArm == hand ? player.getAttackAnim(partialTick) : 0.0F;
        boolean usingRod = player.isUsingItem() && player.getUsedItemHand() == hand;

        // Reconstruct the held item's pose in the hand pass, including equip and swing animation.
        PoseStack pose = new PoseStack();
        float xBob = Mth.lerp(partialTick, localPlayer.xBobO, localPlayer.xBob);
        float yBob = Mth.lerp(partialTick, localPlayer.yBobO, localPlayer.yBob);
        pose.mulPose(Axis.XP.rotationDegrees((player.getViewXRot(partialTick) - xBob) * 0.1F));
        pose.mulPose(Axis.YP.rotationDegrees((player.getViewYRot(partialTick) - yBob) * 0.1F));
        if (!usingRod) {
            float rootSwing = Mth.sqrt(armSwing);
            pose.translate(side * -0.4F * Mth.sin(rootSwing * Mth.PI),
                    0.2F * Mth.sin(rootSwing * Mth.TWO_PI), -0.2F * Mth.sin(armSwing * Mth.PI));
        }
        pose.translate(side * 0.56F, -0.52F - equipProgress * 0.6F, -0.72F);
        if (!usingRod) {
            float turn = Mth.sin(armSwing * armSwing * Mth.PI);
            float swingAmount = Mth.sin(Mth.sqrt(armSwing) * Mth.PI);
            pose.mulPose(Axis.YP.rotationDegrees(side * (45.0F - turn * 20.0F)));
            pose.mulPose(Axis.ZP.rotationDegrees(side * swingAmount * -20.0F));
            pose.mulPose(Axis.XP.rotationDegrees(swingAmount * -80.0F));
            pose.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
        }
        FishingRodMotion.apply(pose, player, partialTick);
        ItemDisplayContext context = leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        BakedModel model = minecraft.getItemRenderer().getModel(renderedRod, player.level(), player,
                player.getId() + context.ordinal());
        model.getTransforms().getTransform(context).apply(leftHand, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
        // The vanilla rod sprite's tip is near its upper-right corner in item model space.
        Vector3f tip = pose.last().pose().transformPosition(new Vector3f(15.0F / 16.0F, 15.0F / 16.0F, 0.5F));
        if (!tip.isFinite() || tip.z >= -0.001F) return;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        GameRendererFovAccess renderer = (GameRendererFovAccess) minecraft.gameRenderer;
        double worldFov = renderer.fih$getFov(camera, partialTick, true);
        double handFov = renderer.fih$getFov(camera, partialTick, false);
        double scale = Math.tan(Math.toRadians(worldFov) / 2.0)
                / Math.tan(Math.toRadians(handFov) / 2.0);
        if (!Double.isFinite(scale) || scale <= 0.0) return;
        tip.mul((float) scale, (float) scale, 1.0F).rotate(camera.rotation());
        cir.setReturnValue(camera.getPosition().add(new Vec3(tip.x, tip.y, tip.z)));
    }
}
