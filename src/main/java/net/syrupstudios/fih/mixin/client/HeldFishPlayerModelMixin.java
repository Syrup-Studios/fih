package net.syrupstudios.fih.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.syrupstudios.fih.ai.HeldFish;
import net.syrupstudios.fih.client.HeldFishThirdPersonPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HeldFishPlayerModelMixin<T extends LivingEntity> {
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void fih$poseHeldFish(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (HeldFish.isFish(player.getMainHandItem())) fih$poseArm(player.getMainArm());
        if (HeldFish.isFish(player.getOffhandItem())) fih$poseArm(player.getMainArm().getOpposite());
    }

    @Unique
    private void fih$poseArm(HumanoidArm side) {
        boolean left = side == HumanoidArm.LEFT;
        float mirror = left ? 1.0F : -1.0F;
        ModelPart arm = left ? leftArm : rightArm;
        arm.xRot += HeldFishThirdPersonPose.armPitch * Mth.DEG_TO_RAD;
        arm.yRot += mirror * HeldFishThirdPersonPose.armYaw * Mth.DEG_TO_RAD;
        arm.zRot += mirror * HeldFishThirdPersonPose.armAngle * Mth.DEG_TO_RAD;
    }
}
