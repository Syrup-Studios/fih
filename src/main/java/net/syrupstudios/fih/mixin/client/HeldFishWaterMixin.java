package net.syrupstudios.fih.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.syrupstudios.fih.client.HeldFishRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class HeldFishWaterMixin {
    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void fih$renderFishWithoutFlopping(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractFish fish && HeldFishRenderer.isRendering(fish)) cir.setReturnValue(true);
    }
}
