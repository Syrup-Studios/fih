package net.syrupstudios.fih.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.syrupstudios.fih.client.FishPitchAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class FishRendererPitchMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void fih$tiltSwimmingFish(LivingEntity entity, PoseStack poseStack, float bob, float bodyYaw,
            float partialTick, float scale, CallbackInfo ci) {
        if (!(entity instanceof AbstractFish fish) || !fish.isAlive() || !fish.isInWater()
                || !(fish instanceof FishPitchAccess pitch)) return;

        float pivotHeight = fish.getBbHeight() * 0.5F;
        poseStack.translate(0.0, pivotHeight, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch.fih$swimPitch(partialTick)));
        poseStack.translate(0.0, -pivotHeight, 0.0);
    }
}
