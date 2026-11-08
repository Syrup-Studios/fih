package net.syrupstudios.fih.mixin.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.AbstractFish;
import net.syrupstudios.fih.client.FishPitchAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class FishSwimPitchMixin implements FishPitchAccess {
    @Unique private float fih$previousSwimPitch;
    @Unique private float fih$swimPitch;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void fih$updateSwimPitch(CallbackInfo ci) {
        AbstractFish fish = (AbstractFish) (Object) this;
        fih$previousSwimPitch = fih$swimPitch;

        double dx = fish.getX() - fish.xo;
        double dy = fish.getY() - fish.yo;
        double dz = fish.getZ() - fish.zo;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = 0.0F;
        if (fish.isAlive() && fish.isInWater() && Math.abs(dy) > 0.002
                && dx * dx + dy * dy + dz * dz > 0.0001) {
            targetPitch = Mth.clamp((float) Math.toDegrees(Math.atan2(dy, horizontalSpeed)), -50.0F, 50.0F);
        }
        fih$swimPitch = Mth.lerp(0.25F, fih$swimPitch, targetPitch);
    }

    @Override
    public float fih$swimPitch(float partialTick) {
        return Mth.lerp(partialTick, fih$previousSwimPitch, fih$swimPitch);
    }
}
