package net.syrupstudios.fih.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class FishingHookTrackingMixin {
    @Inject(method = "clientTrackingRange", at = @At("RETURN"), cancellable = true)
    private void fih$trackLongCasts(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == EntityType.FISHING_BOBBER) cir.setReturnValue(Math.max(cir.getReturnValue(), 16));
    }
}
