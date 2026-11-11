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
import net.minecraft.client.renderer.RenderType;
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
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.syrupstudios.fih.ai.FishingHookAccess;
import net.syrupstudios.fih.client.FishingRodMotion;
import net.syrupstudios.fih.client.BobberModelRenderer;

@Mixin(FishingHookRenderer.class)
public abstract class FishingLineRendererMixin {
    private static final float fih$BOBBER_TOP = 2.5F / 16.0F;
    private static final float fih$BOBBER_BOTTOM = -1.5F / 16.0F;
    private static final float fih$HOOK_TOP = 2.0F / 16.0F;
    @Unique private Vec3 fih$bobberAxis = new Vec3(0.0, 1.0, 0.0);

    @Shadow
    private Vec3 getPlayerHandPos(Player player, float swing, float partialTick) { throw new AssertionError(); }

    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void fih$alignBobber(FishingHook hook, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, CallbackInfo ci) {
        Player player = hook.getPlayerOwner();
        fih$bobberAxis = new Vec3(0.0, 1.0, 0.0);
        if (player == null) return;
        Vec3 bobber = hook.getPosition(partialTick);
        float swing = Mth.sin(Mth.sqrt(player.getAttackAnim(partialTick)) * Mth.PI);
        Vec3 rodLine = getPlayerHandPos(player, swing, partialTick).subtract(bobber);
        Vec3 lowerLine = ((FishingHookAccess) hook).fih$renderHookPosition(partialTick).subtract(bobber);
        FishingHookAccess access = (FishingHookAccess) hook;
        float upperTension = access.fih$visualTension(partialTick);
        float lowerTension = access.fih$visualLowerTension(partialTick);
        Vec3 target = rodLine.normalize().scale(0.15 + upperTension);
        if (lowerLine.lengthSqr() > 1.0E-6) {
            target = target.subtract(lowerLine.normalize().scale(0.15 + lowerTension));
        }
        if (target.lengthSqr() < 1.0E-6) return;
        target = target.normalize();
        float influence = 0.15F + 0.85F * Math.max(upperTension, lowerTension);
        Quaternionf tilt = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F,
                (float) target.x, (float) target.y, (float) target.z);
        Vector3f axis = new Quaternionf().slerp(tilt, influence).transform(new Vector3f(0.0F, 1.0F, 0.0F));
        fih$bobberAxis = new Vec3(axis.x, axis.y, axis.z);
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;IFIII)V"))
    private void fih$hideVanillaSprite(VertexConsumer consumer, PoseStack.Pose pose, int light,
            float x, int y, int u, int v) {}

    @Inject(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("TAIL"))
    private void fih$renderSubmergedHook(FishingHook hook, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, CallbackInfo ci) {
        FishingHookAccess access = (FishingHookAccess) hook;
        if (access.fih$hasBobber()) {
            pose.pushPose();
            pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F,
                    (float) fih$bobberAxis.x, (float) fih$bobberAxis.y, (float) fih$bobberAxis.z));
            BobberModelRenderer.renderBobber(pose, buffers, light);
            pose.popPose();
        }
        Vec3 offset = access.fih$renderHookPosition(partialTick)
                .subtract(hook.getPosition(partialTick));
        if (access.fih$hasBobber() && offset.lengthSqr() < 0.0001) return;

        if (access.fih$hasBobber()) {
            VertexConsumer line = buffers.getBuffer(RenderType.lines());
            PoseStack.Pose linePose = pose.last();
            Vec3 start = fih$bobberAxis.scale(fih$BOBBER_BOTTOM);
            Vec3 end = offset.add(0.0, fih$HOOK_TOP, 0.0);
            Vec3 direction = end.subtract(start).normalize();
            line.addVertex(linePose, (float) start.x, (float) start.y, (float) start.z).setColor(0xFFC0C0C0)
                    .setNormal(linePose, (float) direction.x, (float) direction.y, (float) direction.z);
            line.addVertex(linePose, (float) end.x, (float) end.y, (float) end.z).setColor(0xFFC0C0C0)
                    .setNormal(linePose, (float) direction.x, (float) direction.y, (float) direction.z);
        }

        pose.pushPose();
        pose.translate(offset.x, offset.y, offset.z);
        BobberModelRenderer.renderHook(pose, buffers, light, Minecraft.getInstance().gameRenderer.getMainCamera().getYRot());
        pose.popPose();
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/FishingHookRenderer;stringVertex(FFFLcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;FF)V"))
    private void fih$renderLine(float x, float y, float z, VertexConsumer consumer, PoseStack.Pose pose,
            float fraction, float nextFraction, FishingHook hook, float yaw, float partialTick,
            PoseStack stack, MultiBufferSource buffers, int light) {
        Player owner = hook.getPlayerOwner();
        if (owner != null && !owner.getMainHandItem().is(Items.FISHING_ROD)
                && !owner.getOffhandItem().is(Items.FISHING_ROD)) return;
        FishingHookAccess access = (FishingHookAccess) hook;
        Vec3 start = access.fih$hasBobber() ? fih$bobberAxis.scale(fih$BOBBER_TOP)
                : access.fih$renderHookPosition(partialTick).subtract(hook.getPosition(partialTick)).add(0.0, fih$HOOK_TOP, 0.0);
        float dx = x - (float) start.x;
        float dy = y + 0.25F - (float) start.y;
        float dz = z - (float) start.z;
        float tension = Math.min(1.0F, ((FishingHookAccess) hook).fih$visualTension(partialTick) * 2.0F);
        float curve = Mth.lerp(tension, (fraction * fraction + fraction) * 0.5F, fraction);
        float nextCurve = Mth.lerp(tension, (nextFraction * nextFraction + nextFraction) * 0.5F, nextFraction);
        float normalX = dx * (nextFraction - fraction);
        float normalY = dy * (nextCurve - curve);
        float normalZ = dz * (nextFraction - fraction);
        float length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length > 1.0E-6F) {
            normalX /= length;
            normalY /= length;
            normalZ /= length;
        }
        consumer.addVertex(pose, (float) start.x + dx * fraction, (float) start.y + dy * curve,
                        (float) start.z + dz * fraction)
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
